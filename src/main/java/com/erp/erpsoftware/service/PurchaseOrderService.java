package com.erp.erpsoftware.service;

import com.erp.erpsoftware.entity.PurchaseOrder;
import com.erp.erpsoftware.entity.PurchaseOrderMapping;
import com.erp.erpsoftware.entity.PurchaseOrderMappingId;
import com.erp.erpsoftware.entity.Vendor;
import com.erp.erpsoftware.repository.PurchaseOrderRepository;
import com.erp.erpsoftware.repository.PurchaseOrderMappingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class PurchaseOrderService {

    @Autowired
    private PurchaseOrderRepository repository;

    @Autowired
    private PurchaseOrderMappingRepository detailRepository;

    @Autowired
    private ItemService itemService;

    @Autowired
    private VendorService vendorService;

    @Autowired
    private CurrentStockService currentStockService;

    @Autowired
    private StockLedgerService stockLedgerService;

    public List<PurchaseOrder> getAllPurchaseOrders() {
        List<PurchaseOrder> orders = repository.findAllActive();
        for (PurchaseOrder order : orders) {
            // Load Vendor Name
            if (order.getVendorId() != null) {
                try {
                    Vendor vendor = vendorService.getVendorById(order.getVendorId());
                    if (vendor != null) {
                        order.setVendorName(vendor.getVendorName());
                        order.setTransactionAccount(vendor.getTransactionAccount());
                    }
                } catch (Exception e) {
                    order.setVendorName("Unknown (ID: " + order.getVendorId() + ")");
                }
            }

            // Load Details
            List<PurchaseOrderMapping> details = detailRepository.findActiveDetailsByOrderId(order.getOrderId());
            int slNo = 1;
            for (PurchaseOrderMapping detail : details) {
                detail.setSlNo(slNo++);
                if (detail.getItemId() != null) {
                    detail.setItem(itemService.getItemById(detail.getItemId()));
                }
            }
            order.setDetails(details);

            // Compute total quantity
            int totalQty = details.stream().mapToInt(d -> d.getQuantity() != null ? d.getQuantity() : 0).sum();
            order.setTotalQuantity(totalQty);

            // Derive status: 1=Created, 2=Partially Received, 3=Fully Received
            order.setStatus(deriveHeaderStatus(details));
        }
        return orders;
    }

    public PurchaseOrder save(PurchaseOrder order) {
        // Calculate total cost and total quantity from details if they are set
        double grandTotal = 0.0;
        int totalQty = 0;
        if (order.getDetails() != null) {
            for (PurchaseOrderMapping detail : order.getDetails()) {
                if (detail.getIsValid() == null || detail.getIsValid() != 0) {
                    int qty = detail.getQuantity() != null ? detail.getQuantity() : 0;
                    int rQty = detail.getReceivedQuantity() != null ? detail.getReceivedQuantity() : 0;

                    if (rQty > qty) {
                        throw new IllegalArgumentException("Received Quantity cannot exceed Ordered Quantity.");
                    }

                    if (rQty == 0) {
                        detail.setStatus(1);
                        detail.setReceiveOrderStatus("create");
                    } else if (rQty < qty) {
                        detail.setStatus(2);
                        detail.setReceiveOrderStatus("Partially Received");
                    } else {
                        detail.setStatus(3);
                        detail.setReceiveOrderStatus("Fully Received");
                    }

                    double lineTotal = (detail.getRate() != null ? detail.getRate() : 0.0) * qty;
                    detail.setTotalCost(lineTotal);
                    grandTotal += lineTotal;
                    totalQty += qty;
                }
            }
        }
        order.setTotalCost(grandTotal);
        order.setTotalQuantity(totalQty);

        // Save Header
        PurchaseOrder savedHeader = repository.save(order);
        Long orderId = savedHeader.getOrderId();

        if (order.getOrderId() != null) {
            // Capture previously received quantities per item BEFORE soft-deleting
            List<PurchaseOrderMapping> existingDetails = detailRepository.findByOrderId(order.getOrderId());
            Map<Long, Integer> previousReceivedQtyMap = new HashMap<>();
            for (PurchaseOrderMapping existing : existingDetails) {
                if (existing.getIsValid() != null && existing.getIsValid() != 0) {
                    int prev = existing.getReceivedQuantity() != null ? existing.getReceivedQuantity() : 0;
                    previousReceivedQtyMap.merge(existing.getItemId(), prev, Integer::sum);
                }
            }

            // Soft-delete old detail lines
            for (PurchaseOrderMapping existing : existingDetails) {
                existing.setIsValid(0);
                detailRepository.save(existing);
            }

            // Save new details and compute stock delta per item
            if (order.getDetails() != null) {
                int slNo = 1;
                for (PurchaseOrderMapping detail : order.getDetails()) {
                    detail.setOrderId(orderId);
                    detail.setSlNo(slNo++);
                    if (detail.getIsValid() == null) detail.setIsValid(1);
                    detailRepository.save(detail);

                    // Automatically update current stock & record stock ledger: add only the NEW delta received
                    int newReceived = detail.getReceivedQuantity() != null ? detail.getReceivedQuantity() : 0;
                    int oldReceived = previousReceivedQtyMap.getOrDefault(detail.getItemId(), 0);
                    int delta = newReceived - oldReceived;
                    if (delta > 0) {
                    if (delta > 0) {
                        // Log IN movement to stock_ledger and update current stock
                        stockLedgerService.recordStockIn(detail.getItemId(), (double) delta, orderId, "PURCHASE_RECEIVED");
                    } else if (delta < 0) {
                        // Correction: items were un-received — deduct stock and log OUT
                        stockLedgerService.recordStockOut(detail.getItemId(), (double) Math.abs(delta), orderId, "PURCHASE_CORRECTION");
                    }
                    }
                }
            }

        } else {
            // New order — just save details (no stock update until items are received)
            if (order.getDetails() != null) {
                int slNo = 1;
                for (PurchaseOrderMapping detail : order.getDetails()) {
                    detail.setOrderId(orderId);
                    detail.setSlNo(slNo++);
                    if (detail.getIsValid() == null) detail.setIsValid(1);
                    detailRepository.save(detail);
                }
            }
        }

        // Update Header Status based on saved details
        int derivedStatus = deriveHeaderStatus(order.getDetails());
        savedHeader.setStatus(derivedStatus);
        repository.save(savedHeader);

        return savedHeader;
    }

    private int deriveHeaderStatus(List<PurchaseOrderMapping> details) {
        if (details == null || details.isEmpty()) {
            return 1; // Created
        }
        boolean allFullyReceived = true;
        boolean anyReceived = false;

        for (PurchaseOrderMapping d : details) {
            int qty = d.getQuantity() != null ? d.getQuantity() : 0;
            int rQty = d.getReceivedQuantity() != null ? d.getReceivedQuantity() : 0;
            int st = d.getStatus() != null ? d.getStatus() : 1;

            if (rQty > 0 || st > 1) {
                anyReceived = true;
            }
            if (st < 3 || (qty > 0 && rQty < qty)) {
                allFullyReceived = false;
            }
        }

        if (allFullyReceived) {
            return 3; // Fully Received
        } else if (anyReceived) {
            return 2; // Partially Received
        } else {
            return 1; // Created
        }
    }

    public PurchaseOrder getPurchaseOrderById(Long id) {
        PurchaseOrder order = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Purchase Order not found with id: " + id));

        // Load Vendor Name
        if (order.getVendorId() != null) {
            try {
                Vendor vendor = vendorService.getVendorById(order.getVendorId());
                if (vendor != null) {
                    order.setVendorName(vendor.getVendorName());
                    order.setTransactionAccount(vendor.getTransactionAccount());
                }
            } catch (Exception e) {
                order.setVendorName("Unknown (ID: " + order.getVendorId() + ")");
            }
        }

        // Load Details
        List<PurchaseOrderMapping> details = detailRepository.findActiveDetailsByOrderId(order.getOrderId());
        int slNo = 1;
        for (PurchaseOrderMapping detail : details) {
            detail.setSlNo(slNo++);
            if (detail.getItemId() != null) {
                detail.setItem(itemService.getItemById(detail.getItemId()));
            }
        }
        order.setDetails(details);

        // Compute total quantity
        int totalQty = details.stream().mapToInt(d -> d.getQuantity() != null ? d.getQuantity() : 0).sum();
        order.setTotalQuantity(totalQty);

        // Derive status
        order.setStatus(deriveHeaderStatus(details));

        return order;
    }

    public void deletePurchaseOrder(Long id) {
        PurchaseOrder order = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Purchase Order not found with id: " + id));
        repository.delete(order);

        // Soft delete details as well
        List<PurchaseOrderMapping> details = detailRepository.findByOrderId(id);
        for (PurchaseOrderMapping detail : details) {
            if (detail.getIsValid() != null && detail.getIsValid() != 0) {
                detail.setIsValid(0);
                detailRepository.save(detail);

                int received = detail.getReceivedQuantity() != null ? detail.getReceivedQuantity() : 0;
                if (received > 0) {
                    currentStockService.deductStock(detail.getItemId(), (double) received, id, "PURCHASE_CANCELLED");
                }
            }
        }
    }

    public void updateItemStatus(Long orderId, Long itemId, String status) {
        PurchaseOrderMappingId id = new PurchaseOrderMappingId(orderId, itemId);
        PurchaseOrderMapping detail = detailRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Purchase Order Detail not found for Order ID: " + orderId + ", Item ID: " + itemId));
        detail.setReceiveOrderStatus(status);
        detailRepository.save(detail);
    }

    public void updatePurchaseOrderStatus(Long orderId, String statusStr) {
        PurchaseOrder order = repository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Purchase Order not found with id: " + orderId));

        List<PurchaseOrderMapping> details = detailRepository.findActiveDetailsByOrderId(orderId);

        int targetStatus = 1;
        if ("3".equals(statusStr) || "Received".equalsIgnoreCase(statusStr) || "Fully Received".equalsIgnoreCase(statusStr)) {
            targetStatus = 3;
        } else if ("2".equals(statusStr) || "Partially Received".equalsIgnoreCase(statusStr)) {
            targetStatus = 2;
        } else {
            targetStatus = 1;
        }

        for (PurchaseOrderMapping detail : details) {
            detail.setStatus(targetStatus);
            if (targetStatus == 3) {
                detail.setReceivedQuantity(detail.getQuantity() != null ? detail.getQuantity() : 0);
                detail.setReceiveOrderStatus("Fully Received");
            } else if (targetStatus == 2) {
                detail.setReceiveOrderStatus("Partially Received");
            } else {
                detail.setReceivedQuantity(0);
                detail.setReceiveOrderStatus("create");
            }
            detailRepository.save(detail);
        }

        order.setStatus(targetStatus);
        repository.save(order);
    }
}
