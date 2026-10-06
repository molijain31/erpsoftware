package com.erp.erpsoftware.service;

import com.erp.erpsoftware.entity.SalesOrder;
import com.erp.erpsoftware.entity.SalesOrderDetail;
import com.erp.erpsoftware.entity.Supplier;
import com.erp.erpsoftware.repository.SalesOrderRepository;
import com.erp.erpsoftware.repository.SalesOrderDetailRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Service
@Transactional
public class SalesOrderService {

    @Autowired
    private SalesOrderRepository repository;

    @Autowired
    private SalesOrderDetailRepository detailRepository;

    @Autowired
    private ItemService itemService;

    @Autowired
    private SupplierService supplierService;

    @Autowired
    private CurrentStockService currentStockService;

    @Autowired
    private StockLedgerService stockLedgerService;

    @Autowired
    private BookingRequestService bookingRequestService;

    public List<SalesOrder> getAllSalesOrders() {
        List<SalesOrder> orders = repository.findAllActive();
        List<SalesOrder> validOrders = new ArrayList<>();
        for (SalesOrder order : orders) {
            List<SalesOrderDetail> details = detailRepository.findActiveDetailsByOrderId(order.getOrderId());
            if (details == null || details.isEmpty()) {
                continue; // Skip empty sales order headers created without items
            }

            if (order.getSupplierId() != null) {
                try {
                    Supplier supplier = supplierService.getSupplierById(order.getSupplierId());
                    if (supplier != null) {
                        order.setSupplierName(supplier.getSupplierName());
                    }
                } catch (Exception e) {
                    order.setSupplierName("Unknown (ID: " + order.getSupplierId() + ")");
                }
            }

            int slNo = 1;
            for (SalesOrderDetail detail : details) {
                detail.setSlNo(slNo++);
                if (detail.getItemId() != null) {
                    detail.setItem(itemService.getItemById(detail.getItemId()));
                }
            }
            order.setDetails(details);
            validOrders.add(order);
        }
        validOrders.sort((a, b) -> {
            if (b.getEntryDate() != null && a.getEntryDate() != null) {
                return b.getEntryDate().compareTo(a.getEntryDate());
            }
            Long idA = a.getOrderId() != null ? a.getOrderId() : 0L;
            Long idB = b.getOrderId() != null ? b.getOrderId() : 0L;
            return idB.compareTo(idA);
        });
        return validOrders;
    }

    public SalesOrder getSalesOrderById(Long id) {
        List<SalesOrder> list = repository.findByOrderId(id);
        if (list == null || list.isEmpty()) {
            throw new NoSuchElementException("Sales order not found with ID: " + id);
        }
        SalesOrder order = list.get(0);
        if (order.getSupplierId() != null) {
            try {
                Supplier supplier = supplierService.getSupplierById(order.getSupplierId());
                if (supplier != null) {
                    order.setSupplierName(supplier.getSupplierName());
                }
            } catch (Exception e) {
                order.setSupplierName("Unknown");
            }
        }
        List<SalesOrderDetail> details = detailRepository.findActiveDetailsByOrderId(order.getOrderId());
        int slNo = 1;
        for (SalesOrderDetail detail : details) {
            detail.setSlNo(slNo++);
            if (detail.getItemId() != null) {
                detail.setItem(itemService.getItemById(detail.getItemId()));
            }
        }
        order.setDetails(details);
        return order;
    }

    public SalesOrder getSalesOrderByBookingNo(String bookingNo) {
        List<SalesOrder> list = repository.findByBookingNo(bookingNo);
        if (list == null || list.isEmpty()) {
            return null;
        }
        SalesOrder order = list.get(0);
        List<SalesOrderDetail> details = detailRepository.findActiveDetailsByOrderId(order.getOrderId());
        int slNo = 1;
        for (SalesOrderDetail detail : details) {
            detail.setSlNo(slNo++);
            if (detail.getItemId() != null) {
                detail.setItem(itemService.getItemById(detail.getItemId()));
            }
        }
        order.setDetails(details);
        return order;
    }

    public List<SalesOrder> getSalesOrderBySalesOrderNo(String salesOrderNo) {
        if (salesOrderNo == null || salesOrderNo.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return repository.findBySalesOrderNo(salesOrderNo.trim());
    }

    public List<SalesOrder> getAllSalesOrdersByBookingNo(String bookingNo) {
        List<SalesOrder> list = repository.findByBookingNo(bookingNo);
        List<SalesOrder> validList = new ArrayList<>();
        if (list != null) {
            for (SalesOrder order : list) {
                List<SalesOrderDetail> details = detailRepository.findActiveDetailsByOrderId(order.getOrderId());
                if (details != null && !details.isEmpty()) {
                    order.setDetails(details);
                    validList.add(order);
                }
            }
        }
        validList.sort((a, b) -> {
            if (b.getEntryDate() != null && a.getEntryDate() != null) {
                return b.getEntryDate().compareTo(a.getEntryDate());
            }
            Long idA = a.getOrderId() != null ? a.getOrderId() : 0L;
            Long idB = b.getOrderId() != null ? b.getOrderId() : 0L;
            return idB.compareTo(idA);
        });
        return validList;
    }

    @Transactional
    public void deleteByBookingNo(String bookingNo) {
        List<SalesOrder> orders = repository.findByBookingNo(bookingNo);
        for (SalesOrder order : orders) {
            List<SalesOrderDetail> details = detailRepository.findActiveDetailsByOrderId(order.getOrderId());
            detailRepository.deleteAll(details);
            repository.delete(order);
        }
        // Reset BookingRequest status back to Pending
        List<com.erp.erpsoftware.entity.BookingRequest> bookingRequests = bookingRequestService.getRequestsByBookingNo(bookingNo);
        for (com.erp.erpsoftware.entity.BookingRequest req : bookingRequests) {
            req.setStatus("Pending");
            bookingRequestService.save(req);
        }
    }

    public SalesOrder save(SalesOrder order) {
        if (order.getReceivedAmount() == null) {
            order.setReceivedAmount(0.0);
        }
        if (order.getPaymentStatus() == null || order.getPaymentStatus().trim().isEmpty()) {
            order.setPaymentStatus("1 - Unpaid");
        }
        double grandTotal = 0.0;
        int totalQty = 0;
        if (order.getDetails() != null) {
            for (SalesOrderDetail detail : order.getDetails()) {
                if (detail.getIsValid() == null || detail.getIsValid() != 0) {
                    int qty = detail.getQuantity() != null ? detail.getQuantity() : 0;
                    int dQty = detail.getDeliveredQuantity() != null ? detail.getDeliveredQuantity() : 0;

                    if (dQty > qty) {
                        throw new IllegalArgumentException("Delivered Quantity cannot exceed Booked Quantity.");
                    }

                    if (dQty == 0) {
                        detail.setStatus(1);
                        detail.setDeliveryOrderStatus(0);
                    } else if (dQty < qty) {
                        detail.setStatus(2);
                        detail.setDeliveryOrderStatus(0);
                    } else {
                        detail.setStatus(3);
                        detail.setDeliveryOrderStatus(1);
                    }

                    double lineTotal = (detail.getRate() != null ? detail.getRate() : 0.0) * dQty;
                    detail.setTotalCost(lineTotal);
                    grandTotal += lineTotal;
                    totalQty += dQty;
                }
            }
        }
        order.setTotalCost(grandTotal);
        order.setTotalQuantity(totalQty);

        boolean isExisting = (order.getOrderId() != null);

        // Save Header
        SalesOrder savedHeader = repository.save(order);
        Long orderId = savedHeader.getOrderId();

        if (isExisting) {
            // Edit Case:
            List<SalesOrderDetail> existingDetails = detailRepository.findByOrderId(orderId);
            Map<Long, SalesOrderDetail> existingDetailsMap = new HashMap<>();
            Map<Long, Integer> previousDeliveredQtyMap = new HashMap<>();
            
            for (SalesOrderDetail existing : existingDetails) {
                if (existing.getIsValid() != null && existing.getIsValid() != 0) {
                    int prev = existing.getDeliveredQuantity() != null ? existing.getDeliveredQuantity() : 0;
                    previousDeliveredQtyMap.merge(existing.getItemId(), prev, Integer::sum);
                    existingDetailsMap.put(existing.getItemId(), existing);
                }
            }

            // Save new/updated details and update stock
            if (order.getDetails() != null) {
                int slNo = 1;
                for (SalesOrderDetail detail : order.getDetails()) {
                    detail.setOrderId(orderId);
                    detail.setSlNo(slNo++);
                    if (detail.getIsValid() == null) detail.setIsValid(1);
                    
                    int newSent = detail.getDeliveredQuantity() != null ? detail.getDeliveredQuantity() : 0;
                    int oldSent = previousDeliveredQtyMap.getOrDefault(detail.getItemId(), 0);
                    int delta = newSent - oldSent;

                    SalesOrderDetail existingDetail = existingDetailsMap.remove(detail.getItemId());
                    if (existingDetail != null) {
                        existingDetail.setQuantity(detail.getQuantity());
                        existingDetail.setRate(detail.getRate());
                        existingDetail.setTotalCost(detail.getTotalCost());
                        existingDetail.setStatus(detail.getStatus());
                        existingDetail.setDeliveredQuantity(detail.getDeliveredQuantity());
                        existingDetail.setDeliveryOrderStatus(detail.getDeliveryOrderStatus());
                        existingDetail.setIsValid(1);
                        detailRepository.save(existingDetail);
                    } else {
                        detailRepository.save(detail);
                    }

                    // Record send_quantity in stock_ledger and update stock
                    if (delta > 0) {
                        stockLedgerService.recordStockOut(detail.getItemId(), (double) delta, orderId, "SALES_ORDER");
                    } else if (delta < 0) {
                        stockLedgerService.recordStockIn(detail.getItemId(), (double) Math.abs(delta), orderId, "SALES_CORRECTION");
                    }
                }
            }

            // Soft-delete old details that are no longer present
            for (SalesOrderDetail remaining : existingDetailsMap.values()) {
                remaining.setIsValid(0);
                detailRepository.save(remaining);
            }
        } else {
            // New Case:
            if (order.getDetails() != null) {
                int slNo = 1;
                for (SalesOrderDetail detail : order.getDetails()) {
                    detail.setOrderId(orderId);
                    detail.setSlNo(slNo++);
                    if (detail.getIsValid() == null) detail.setIsValid(1);
                    detailRepository.save(detail);

                    // Record send_quantity in stock_ledger and update stock
                    int sent = detail.getDeliveredQuantity() != null ? detail.getDeliveredQuantity() : 0;
                    if (sent > 0) {
                        stockLedgerService.recordStockOut(detail.getItemId(), (double) sent, orderId, "SALES_ORDER");
                    }
                }
            }
        }

        // Update Header Status based on saved details
        int derivedStatus = deriveHeaderStatus(order.getDetails());
        savedHeader.setStatus(derivedStatus);
        repository.save(savedHeader);

        return savedHeader;
    }

    private int deriveHeaderStatus(List<SalesOrderDetail> details) {
        if (details == null || details.isEmpty()) {
            return 1; // Pending
        }
        boolean allFullyDelivered = true;
        boolean anyDelivered = false;

        for (SalesOrderDetail d : details) {
            int qty = d.getQuantity() != null ? d.getQuantity() : 0;
            int dQty = d.getDeliveredQuantity() != null ? d.getDeliveredQuantity() : 0;
            int st = d.getStatus() != null ? d.getStatus() : 1;

            if (dQty > 0 || st > 1) {
                anyDelivered = true;
            }
            if (st < 3 || (qty > 0 && dQty < qty)) {
                allFullyDelivered = false;
            }
        }

        if (allFullyDelivered) {
            return 3; // Delivered
        } else if (anyDelivered) {
            return 2; // Partially Delivered
        } else {
            return 1; // Pending
        }
    }

    public void deleteSalesOrder(Long id) {
        List<SalesOrder> list = repository.findByOrderId(id);
        if (list == null || list.isEmpty()) {
            return;
        }
        SalesOrder order = list.get(0);
        List<SalesOrderDetail> details = detailRepository.findByOrderId(id);
        for (SalesOrderDetail detail : details) {
            detail.setIsValid(0);
            detailRepository.save(detail);

            int delivered = detail.getDeliveredQuantity() != null ? detail.getDeliveredQuantity() : 0;
            if (delivered > 0) {
                currentStockService.addStock(detail.getItemId(), (double) delivered, id, "SALES_CANCELLED");
            }
        }
        
        // Reset related booking requests back to "Pending"
        if (order.getBookingNo() != null && !order.getBookingNo().isEmpty()) {
            List<com.erp.erpsoftware.entity.BookingRequest> bookingRequests = bookingRequestService.getRequestsByBookingNo(order.getBookingNo());
            for (com.erp.erpsoftware.entity.BookingRequest req : bookingRequests) {
                req.setStatus("Pending");
                bookingRequestService.save(req);
            }
        }
        
        repository.delete(order);
    }

    @Transactional
    public void saveSalesOrderFromForm(
            List<Integer> itemIds,
            List<Integer> quantities,
            List<Integer> bookedQuantities,
            List<String> supplierNames,
            String bookingNo) {
        
        List<Supplier> allSuppliers = supplierService.getAllSuppliers();
        Map<String, Long> supplierNameToId = new HashMap<>();
        for (Supplier s : allSuppliers) {
            if (s.getSupplierName() != null) {
                supplierNameToId.put(s.getSupplierName().trim().toLowerCase(), s.getSupplierId());
            }
        }

        SalesOrder order = new SalesOrder();
        order.setBookingNo(bookingNo);
        order.setBookingId(bookingNo);
        int seed = (int)(System.currentTimeMillis() % 90000) + (int)(Math.random() * 9000) + 10000;
        order.setSalesOrderNo(String.format("CO-%05d", seed));

        List<com.erp.erpsoftware.entity.BookingRequest> brs = bookingRequestService.getRequestsByBookingNo(bookingNo);
        if (brs != null && !brs.isEmpty()) {
            com.erp.erpsoftware.entity.BookingRequest firstBr = brs.get(0);
            if (firstBr.getPaymentAmount() != null) {
                order.setReceivedAmount(firstBr.getPaymentAmount());
            } else if (order.getReceivedAmount() == null) {
                order.setReceivedAmount(0.0);
            }
            if (firstBr.getPaymentStatus() != null && !firstBr.getPaymentStatus().isEmpty()) {
                order.setPaymentStatus(firstBr.getPaymentStatus());
            } else if (order.getPaymentStatus() == null || order.getPaymentStatus().isEmpty()) {
                order.setPaymentStatus("1 - Unpaid");
            }
        } else {
            if (order.getReceivedAmount() == null) order.setReceivedAmount(0.0);
            if (order.getPaymentStatus() == null || order.getPaymentStatus().isEmpty()) order.setPaymentStatus("1 - Unpaid");
        }

        // Set Header Supplier
        Long firstSupplierId = null;
        if (supplierNames != null) {
            for (String name : supplierNames) {
                if (name != null && !name.isEmpty() && !name.equalsIgnoreCase("N/A") && !name.equalsIgnoreCase("Unknown")) {
                    firstSupplierId = supplierNameToId.get(name.trim().toLowerCase());
                    if (firstSupplierId != null) {
                        break;
                    }
                }
            }
        }
        if (firstSupplierId == null) {
            firstSupplierId = allSuppliers.isEmpty() ? null : allSuppliers.get(0).getSupplierId();
        }
        order.setSupplierId(firstSupplierId);

        List<SalesOrderDetail> details = new java.util.ArrayList<>();
        for (int i = 0; i < itemIds.size(); i++) {
            Long itemId = itemIds.get(i).longValue();
            Integer sentQty = quantities.get(i) != null ? quantities.get(i) : 0;
            Integer bookedQty = (bookedQuantities != null && i < bookedQuantities.size()) ? bookedQuantities.get(i) : sentQty;

            com.erp.erpsoftware.entity.Item item = itemService.getItemById(itemId);
            double rate = (item != null && item.getRate() != null) ? item.getRate() : 0.0;

            SalesOrderDetail detail = new SalesOrderDetail();
            detail.setItemId(itemId);
            detail.setQuantity(bookedQty);
            detail.setDeliveredQuantity(sentQty);
            detail.setRate(rate);
            detail.setTotalCost(rate * sentQty);
            detail.setIsValid(1);

            // Detail Status derivation
            if (sentQty == 0) {
                detail.setStatus(1);
                detail.setDeliveryOrderStatus(0);
            } else if (sentQty < bookedQty) {
                detail.setStatus(2);
                detail.setDeliveryOrderStatus(0);
            } else {
                detail.setStatus(3);
                detail.setDeliveryOrderStatus(1);
            }

            details.add(detail);
        }
        order.setDetails(details);
        // Set SalesOrder integer status: 2 = Sending
        if (order.getStatus() == null || order.getStatus() == 1 || order.getStatus() == 3) {
            order.setStatus(2);
        }
        save(order);

        // ── Auto-set status on BookingRequest ──
        if (bookingNo != null && !bookingNo.isEmpty()) {
            List<SalesOrder> allOrders = repository.findByBookingNo(bookingNo);
            Map<Integer, Integer> totalDeliveredMap = new HashMap<>();
            if (allOrders != null) {
                for (SalesOrder so : allOrders) {
                    List<SalesOrderDetail> detailsList = detailRepository.findActiveDetailsByOrderId(so.getOrderId());
                    if (detailsList != null) {
                        for (SalesOrderDetail d : detailsList) {
                            if (d.getItemId() != null) {
                                int prev = totalDeliveredMap.getOrDefault(d.getItemId().intValue(), 0);
                                int cur = d.getDeliveredQuantity() != null ? d.getDeliveredQuantity() : 0;
                                totalDeliveredMap.put(d.getItemId().intValue(), prev + cur);
                            }
                        }
                    }
                }
            }

            List<com.erp.erpsoftware.entity.BookingRequestItemMapping> mappings = bookingRequestService.getItemMappingsByBookingNo(bookingNo);
            boolean fullyDelivered = true;
            if (mappings != null && !mappings.isEmpty()) {
                for (com.erp.erpsoftware.entity.BookingRequestItemMapping m : mappings) {
                    if (m.getItemId() != null) {
                        int booked = m.getQuantity() != null ? m.getQuantity() : 0;
                        int delivered = totalDeliveredMap.getOrDefault(m.getItemId(), 0);
                        if (delivered < booked) {
                            fullyDelivered = false;
                            break;
                        }
                    }
                }
            }

            String targetStatus = fullyDelivered ? "Delivered" : "Sending";
            List<com.erp.erpsoftware.entity.BookingRequest> bookingRequests = bookingRequestService.getRequestsByBookingNo(bookingNo);
            for (com.erp.erpsoftware.entity.BookingRequest req : bookingRequests) {
                req.setStatus(targetStatus);
                bookingRequestService.save(req);
            }
        }
    }
}
