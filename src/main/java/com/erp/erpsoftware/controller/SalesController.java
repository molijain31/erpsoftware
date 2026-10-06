package com.erp.erpsoftware.controller;

import com.erp.erpsoftware.entity.SalesOrder;
import com.erp.erpsoftware.entity.SalesOrderDetail;
import com.erp.erpsoftware.entity.Supplier;
import com.erp.erpsoftware.entity.SupplierItemMapping;
import com.erp.erpsoftware.entity.Item;
import com.erp.erpsoftware.entity.BookingRequest;
import com.erp.erpsoftware.service.SalesOrderService;
import com.erp.erpsoftware.service.SupplierService;
import com.erp.erpsoftware.service.ItemService;
import com.erp.erpsoftware.service.BookingRequestService;
import com.erp.erpsoftware.repository.SalesOrderDetailRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Controller
@RequestMapping("/sales")
public class SalesController {

    @Autowired
    private SalesOrderService salesOrderService;

    @Autowired
    private SupplierService supplierService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private BookingRequestService bookingRequestService;

    @Autowired
    private SalesOrderDetailRepository detailRepository;

    @Autowired
    private com.erp.erpsoftware.repository.SupplierItemMappingRepository supplierItemMappingRepository;

    @Autowired
    private com.erp.erpsoftware.service.CurrentStockService currentStockService;

    @Autowired
    private com.erp.erpsoftware.repository.StockLedgerRepository stockLedgerRepository;

    private String resolveSupplierName(BookingRequest req) {
        if (req == null) return "—";
        String sName = req.getSupplierName();
        if (sName != null && !sName.trim().isEmpty() && !"null".equalsIgnoreCase(sName.trim())) {
            return sName.trim();
        }
        if (req.getSupplierId() != null && !req.getSupplierId().trim().isEmpty() && !"null".equalsIgnoreCase(req.getSupplierId().trim())) {
            try {
                Long sId = Long.parseLong(req.getSupplierId().trim());
                Supplier supplier = supplierService.getSupplierById(sId);
                if (supplier != null && supplier.getSupplierName() != null && !supplier.getSupplierName().isEmpty()) {
                    return supplier.getSupplierName();
                }
            } catch (Exception ignored) {}
        }
        // book_id removed; no per-item supplier fallback needed here
        List<Supplier> allSuppliers = supplierService.getAllSuppliers();
        if (allSuppliers != null && !allSuppliers.isEmpty()) {
            return allSuppliers.get(0).getSupplierName();
        }
        return "—";
    }

    @GetMapping
    public String list(Model model) {
        try {
            List<BookingRequest> requests = bookingRequestService.getAllBookingRequests();

            // Load all items once for in-memory lookups
            Map<Integer, Item> itemMap = new HashMap<>();
            for (Item item : itemService.getAllItems()) {
                if (item.getItemId() != null) {
                    itemMap.put(item.getItemId().intValue(), item);
                }
            }
            model.addAttribute("itemMap", itemMap);

            List<SalesOrder> allSalesOrders = salesOrderService.getAllSalesOrders();

            // Group booking_request header rows by bookingNo (for supplier/status/entryDate)
            Map<String, BookingRequest> bookingHeaderMap = new LinkedHashMap<>();
            if (requests != null) {
                for (BookingRequest req : requests) {
                    String bNo = req.getBookingNo();
                    if (bNo != null && !bNo.trim().isEmpty() && !bookingHeaderMap.containsKey(bNo)) {
                        bookingHeaderMap.put(bNo, req);
                    }
                }
            }

            // Build groupedRequests using ONLY BookingRequestItemMapping for items
            // groupedRequests maps bookingNo -> List of synthetic BookingRequest rows (one per item from mapping table)
            Map<String, List<com.erp.erpsoftware.entity.BookingRequestItemMapping>> groupedItemMappings = new LinkedHashMap<>();
            for (String bNo : bookingHeaderMap.keySet()) {
                List<com.erp.erpsoftware.entity.BookingRequestItemMapping> mappings = bookingRequestService.getItemMappingsByBookingNo(bNo);
                if (mappings != null && !mappings.isEmpty()) {
                    groupedItemMappings.put(bNo, mappings);
                }
            }

            // Build groupedRequests as bookingNo -> List<BookingRequest> (synthetic, for Thymeleaf compatibility)
            Map<String, List<BookingRequest>> groupedRequests = new LinkedHashMap<>();
            for (Map.Entry<String, List<com.erp.erpsoftware.entity.BookingRequestItemMapping>> entry : groupedItemMappings.entrySet()) {
                String bNo = entry.getKey();
                BookingRequest header = bookingHeaderMap.get(bNo);
                List<BookingRequest> rows = new ArrayList<>();
                for (com.erp.erpsoftware.entity.BookingRequestItemMapping m : entry.getValue()) {
                    BookingRequest r = new BookingRequest();
                    r.setBookingNo(bNo);
                    r.setEntryDate(header != null ? header.getEntryDate() : null);
                    r.setStatus(header != null ? header.getStatus() : "Pending");
                    r.setSupplierId(header != null ? header.getSupplierId() : null);
                    // Store itemId and qty in totalQuantity/totalCost as proxies (no DB columns anymore)
                    r.setTotalQuantity(m.getQuantity());
                    // We use a transient-safe approach: pass item ID via the mapping
                    rows.add(r);
                }
                if (!rows.isEmpty()) groupedRequests.put(bNo, rows);
            }
            // Include bookings that have header but no mappings yet
            for (Map.Entry<String, BookingRequest> entry : bookingHeaderMap.entrySet()) {
                if (!groupedRequests.containsKey(entry.getKey())) {
                    List<BookingRequest> rows = new ArrayList<>();
                    rows.add(entry.getValue());
                    groupedRequests.put(entry.getKey(), rows);
                }
            }
            model.addAttribute("groupedRequests", groupedRequests);

            // Build per-bookingNo aggregates using BookingRequestItemMapping
            Map<String, String> groupedSuppliers = new HashMap<>();
            Map<String, String> groupedStatuses = new HashMap<>();
            Map<String, Double> groupedTotals = new HashMap<>();
            Map<String, java.util.Date> groupedEntryDates = new HashMap<>();

            for (Map.Entry<String, BookingRequest> entry : bookingHeaderMap.entrySet()) {
                String bNo = entry.getKey();
                BookingRequest header = entry.getValue();

                // Supplier name
                String sName = resolveSupplierName(header);
                groupedSuppliers.put(bNo, sName != null && !sName.equals("—") ? sName : "—");

                // Entry date
                groupedEntryDates.put(bNo, header.getEntryDate());

                // Status
                groupedStatuses.put(bNo, header.getStatus() != null ? header.getStatus() : "Pending");

                // Total cost from item mappings
                double total = 0.0;
                List<com.erp.erpsoftware.entity.BookingRequestItemMapping> mappings = groupedItemMappings.get(bNo);
                if (mappings != null) {
                    for (com.erp.erpsoftware.entity.BookingRequestItemMapping m : mappings) {
                        Item item = itemMap.get(m.getItemId());
                        if (item != null && item.getRate() != null && m.getQuantity() != null) {
                            total += item.getRate() * m.getQuantity();
                        }
                    }
                }
                groupedTotals.put(bNo, total);
            }
            model.addAttribute("groupedSuppliers", groupedSuppliers);
            model.addAttribute("groupedStatuses", groupedStatuses);
            model.addAttribute("groupedTotals", groupedTotals);
            model.addAttribute("groupedEntryDates", groupedEntryDates);

            // Build deliveredQty map: bookingNo -> itemId -> totalDelivered
            Map<String, Map<Integer, Integer>> bookingDeliveredQtyMap = new HashMap<>();
            for (SalesOrder so : allSalesOrders) {
                if (so.getBookingNo() != null && so.getDetails() != null) {
                    Map<Integer, Integer> itemDeliveredMap = bookingDeliveredQtyMap.computeIfAbsent(so.getBookingNo(), k -> new HashMap<>());
                    for (SalesOrderDetail detail : so.getDetails()) {
                        if (detail.getItemId() != null) {
                            int itemId = detail.getItemId().intValue();
                            int prev = itemDeliveredMap.getOrDefault(itemId, 0);
                            int cur = detail.getDeliveredQuantity() != null ? detail.getDeliveredQuantity() : 0;
                            itemDeliveredMap.put(itemId, prev + cur);
                        }
                    }
                }
            }

            // Clamp delivered qty to booked qty using item mappings
            for (Map.Entry<String, List<com.erp.erpsoftware.entity.BookingRequestItemMapping>> entry : groupedItemMappings.entrySet()) {
                String bNo = entry.getKey();
                Map<Integer, Integer> delMap = bookingDeliveredQtyMap.get(bNo);
                if (delMap != null) {
                    for (com.erp.erpsoftware.entity.BookingRequestItemMapping m : entry.getValue()) {
                        if (m.getItemId() != null && delMap.containsKey(m.getItemId())) {
                            int booked = m.getQuantity() != null ? m.getQuantity() : 0;
                            delMap.put(m.getItemId(), Math.min(booked, delMap.get(m.getItemId())));
                        }
                    }
                }
            }
            model.addAttribute("bookingDeliveredQtyMap", bookingDeliveredQtyMap);

            // Group all saved sales orders by bookingNo for multiple save history views (only include orders with actual saved details)
            Map<String, List<SalesOrder>> bookingSalesOrdersMap = new LinkedHashMap<>();
            for (SalesOrder so : allSalesOrders) {
                if (so.getBookingNo() != null && !so.getBookingNo().trim().isEmpty()) {
                    if (so.getDetails() != null && !so.getDetails().isEmpty()) {
                        bookingSalesOrdersMap.computeIfAbsent(so.getBookingNo().trim(), k -> new ArrayList<>()).add(so);
                    }
                }
            }
            model.addAttribute("bookingSalesOrdersMap", bookingSalesOrdersMap);

            // groupedItemMappings passed to Thymeleaf for modal table rows
            model.addAttribute("groupedItemMappings", groupedItemMappings);

            Map<String, String> groupedBookingIds = new HashMap<>();
            for (SalesOrder so : allSalesOrders) {
                if (so.getBookingNo() != null && so.getBookingId() != null) {
                    groupedBookingIds.put(so.getBookingNo(), so.getBookingId());
                }
            }
            model.addAttribute("groupedBookingIds", groupedBookingIds);

            Map<String, String> groupedSalesOrderNos = new HashMap<>();
            for (SalesOrder so : allSalesOrders) {
                if (so.getBookingNo() != null && so.getSalesOrderNo() != null && !so.getSalesOrderNo().trim().isEmpty()) {
                    groupedSalesOrderNos.put(so.getBookingNo().trim(), so.getSalesOrderNo().trim());
                }
            }
            for (String bNo : bookingHeaderMap.keySet()) {
                if (bNo != null && (!groupedSalesOrderNos.containsKey(bNo.trim()) || groupedSalesOrderNos.get(bNo.trim()) == null)) {
                    int seed = Math.abs(bNo.trim().hashCode()) % 90000 + 10000;
                    groupedSalesOrderNos.put(bNo.trim(), String.format("CO-%05d", seed));
                }
            }
            model.addAttribute("groupedSalesOrderNos", groupedSalesOrderNos);

            // Sales order status map: 1=create, 2=Partially, 3=Reached
            Map<String, Integer> salesOrderStatusMap = new HashMap<>();
            for (Map.Entry<String, List<com.erp.erpsoftware.entity.BookingRequestItemMapping>> entry : groupedItemMappings.entrySet()) {
                String bNo = entry.getKey();
                List<SalesOrder> ordersForBNo = salesOrderService.getAllSalesOrdersByBookingNo(bNo);

                int totalBooked = 0;
                for (com.erp.erpsoftware.entity.BookingRequestItemMapping m : entry.getValue()) {
                    if (m.getQuantity() != null) totalBooked += m.getQuantity();
                }

                int totalDelivered = 0;
                Map<Integer, Integer> delMap = bookingDeliveredQtyMap.get(bNo);
                if (delMap != null) {
                    for (Integer q : delMap.values()) totalDelivered += q;
                }

                int status = 1;
                if (ordersForBNo != null && !ordersForBNo.isEmpty()) {
                    status = (totalBooked > 0 && totalDelivered >= totalBooked) ? 3 : 2;
                }
                BookingRequest hdr = bookingHeaderMap.get(bNo);
                if (hdr != null && ("Delivered".equalsIgnoreCase(hdr.getStatus()) || "Reached".equalsIgnoreCase(hdr.getStatus()))) {
                    status = 3;
                }
                salesOrderStatusMap.put(bNo, status);
            }
            // For bookings with no mappings, default status=1
            for (String bNo : bookingHeaderMap.keySet()) {
                if (!salesOrderStatusMap.containsKey(bNo)) salesOrderStatusMap.put(bNo, 1);
            }
            model.addAttribute("salesOrderStatusMap", salesOrderStatusMap);

        } catch (Exception e) {
            System.err.println("Error loading sales list: " + e.getMessage());
            model.addAttribute("groupedRequests", new LinkedHashMap<>());
            model.addAttribute("groupedSuppliers", new HashMap<>());
            model.addAttribute("groupedStatuses", new HashMap<>());
            model.addAttribute("itemMap", new HashMap<>());
            model.addAttribute("errorMessage", "Error loading sales list: " + e.getMessage());
        }

        return "sales-list";
    }

    @GetMapping("/new")
    public String newSaleForm(@RequestParam(value = "bookingNo", required = false) String bookingNo,
                              @RequestParam(value = "origin", required = false) String origin,
                              Model model) {
        model.addAttribute("suppliers", supplierService.getAllSuppliers());
        model.addAttribute("items", itemService.getAllItems());

        List<BookingRequest> requests = bookingRequestService.getAllBookingRequests();
        Set<String> bookingNumbers = new LinkedHashSet<>();
        for (BookingRequest req : requests) {
            if (req.getBookingNo() != null && !req.getBookingNo().isEmpty()) {
                bookingNumbers.add(req.getBookingNo());
            }
        }
        model.addAttribute("bookingNumbers", bookingNumbers);
        model.addAttribute("selectedBookingNo", bookingNo);

        // Determine origin for back button navigation
        if (origin != null && !origin.isEmpty()) {
            model.addAttribute("origin", origin);
        } else if (bookingNo != null && !bookingNo.isEmpty()) {
            model.addAttribute("origin", "bookingrequest");
        } else {
            model.addAttribute("origin", "sales");
        }

        return "sales-form";
    }

    @GetMapping("/history/{bookingNo}")
    @ResponseBody
    public Map<String, Object> getBookingHistory(@PathVariable String bookingNo) {
        Map<String, Object> response = new HashMap<>();
        List<SalesOrder> existingOrders = salesOrderService.getAllSalesOrdersByBookingNo(bookingNo);
        List<BookingRequest> requests = bookingRequestService.getRequestsByBookingNo(bookingNo);

        String supplierName = "—";
        if (requests != null && !requests.isEmpty()) {
            for (BookingRequest req : requests) {
                String sName = resolveSupplierName(req);
                if (sName != null && !sName.trim().isEmpty() && !"—".equals(sName)) {
                    supplierName = sName;
                    break;
                }
            }
        }
        response.put("bookingNo", bookingNo);
        response.put("supplierName", supplierName);

        Map<Long, Item> itemMap = new HashMap<>();
        for (Item item : itemService.getAllItems()) {
            if (item.getItemId() != null) {
                itemMap.put(item.getItemId(), item);
            }
        }

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd-MMM-yyyy hh:mm a");

        List<Map<String, Object>> ordersList = new ArrayList<>();
        if (existingOrders != null) {
            for (SalesOrder so : existingOrders) {
                if (so.getDetails() == null || so.getDetails().isEmpty()) continue;
                Map<String, Object> orderMap = new HashMap<>();
                orderMap.put("orderId", so.getOrderId());
                orderMap.put("salesOrderNo", so.getSalesOrderNo() != null ? so.getSalesOrderNo() : "CO-N/A");
                orderMap.put("entryDate", so.getEntryDate() != null ? sdf.format(so.getEntryDate()) : "N/A");
                orderMap.put("totalCost", so.getTotalCost() != null ? so.getTotalCost() : 0.0);

                List<Map<String, Object>> itemDetailsList = new ArrayList<>();
                double calcOrderTotalCost = 0.0;
                for (SalesOrderDetail d : so.getDetails()) {
                    Map<String, Object> dMap = new HashMap<>();
                    Item item = d.getItemId() != null ? itemMap.get(d.getItemId()) : null;
                    dMap.put("itemName", item != null && item.getItemName() != null ? item.getItemName() : "Item #" + d.getItemId());
                    dMap.put("itemCode", item != null && item.getItemCode() != null ? item.getItemCode() : "N/A");
                    dMap.put("itemType", item != null && item.getType() != null && item.getType().getTypeName() != null ? item.getType().getTypeName() : "N/A");
                    double rate = d.getRate() != null ? d.getRate() : (item != null && item.getRate() != null ? item.getRate() : 0.0);
                    int sentQty = d.getDeliveredQuantity() != null ? d.getDeliveredQuantity() : 0;
                    double lineCost = rate * sentQty;
                    calcOrderTotalCost += lineCost;
                    dMap.put("rate", rate);
                    dMap.put("deliveredQty", sentQty);
                    dMap.put("totalCost", lineCost);
                    itemDetailsList.add(dMap);
                }
                orderMap.put("totalCost", calcOrderTotalCost);
                orderMap.put("items", itemDetailsList);
                ordersList.add(orderMap);
            }
        }
        response.put("orders", ordersList);

        // Build summary per item mapping for this booking
        List<com.erp.erpsoftware.entity.BookingRequestItemMapping> itemMappings = bookingRequestService.getItemMappingsByBookingNo(bookingNo);
        Map<Integer, Integer> totalDeliveredMap = new HashMap<>();
        if (existingOrders != null) {
            for (SalesOrder so : existingOrders) {
                if (so.getDetails() != null) {
                    for (SalesOrderDetail d : so.getDetails()) {
                        if (d.getItemId() != null) {
                            int prev = totalDeliveredMap.getOrDefault(d.getItemId().intValue(), 0);
                            int cur = d.getDeliveredQuantity() != null ? d.getDeliveredQuantity() : 0;
                            totalDeliveredMap.put(d.getItemId().intValue(), prev + cur);
                        }
                    }
                }
            }
        }

        List<Map<String, Object>> summaryList = new ArrayList<>();
        if (itemMappings != null) {
            for (com.erp.erpsoftware.entity.BookingRequestItemMapping m : itemMappings) {
                if (m.getItemId() == null) continue;
                Map<String, Object> sMap = new HashMap<>();
                Item item = itemMap.get(m.getItemId().longValue());
                sMap.put("itemName", item != null && item.getItemName() != null ? item.getItemName() : "Item #" + m.getItemId());
                sMap.put("itemType", item != null && item.getType() != null && item.getType().getTypeName() != null ? item.getType().getTypeName() : "N/A");
                int booked = m.getQuantity() != null ? m.getQuantity() : 0;
                int delivered = totalDeliveredMap.getOrDefault(m.getItemId(), 0);
                sMap.put("bookedQty", booked);
                sMap.put("deliveredQty", Math.min(booked, delivered));
                sMap.put("pendingQty", Math.max(0, booked - delivered));
                sMap.put("status", delivered >= booked ? "Complete" : "Pending");
                summaryList.add(sMap);
            }
        }
        response.put("summary", summaryList);

        return response;
    }

    @GetMapping("/booking-details/{bookingNo}")
    @ResponseBody
    public List<Map<String, Object>> getBookingDetails(@PathVariable String bookingNo) {
        List<BookingRequest> requests = bookingRequestService.getRequestsByBookingNo(bookingNo);
        List<com.erp.erpsoftware.entity.BookingRequestItemMapping> itemMappings = bookingRequestService.getItemMappingsByBookingNo(bookingNo);
        List<Map<String, Object>> list = new ArrayList<>();

        List<SalesOrder> existingOrders = salesOrderService.getAllSalesOrdersByBookingNo(bookingNo);
        // existingOrders is sorted descending (latest saved order first)
        Map<Integer, Integer> deliveredMap = new HashMap<>();
        Map<Integer, Integer> latestOrderQtyMap = new HashMap<>();
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd-MMM-yyyy hh:mm a");

        if (existingOrders != null && !existingOrders.isEmpty()) {
            SalesOrder latestOrder = existingOrders.get(0);
            if (latestOrder.getDetails() != null) {
                for (SalesOrderDetail d : latestOrder.getDetails()) {
                    if (d.getItemId() != null) {
                        latestOrderQtyMap.put(d.getItemId().intValue(), d.getDeliveredQuantity() != null ? d.getDeliveredQuantity() : 0);
                    }
                }
            }

            for (SalesOrder existingOrder : existingOrders) {
                if (existingOrder.getDetails() != null) {
                    for (SalesOrderDetail mapping : existingOrder.getDetails()) {
                        if (mapping.getItemId() != null) {
                            int prev = deliveredMap.getOrDefault(mapping.getItemId().intValue(), 0);
                            int cur = mapping.getDeliveredQuantity() != null ? mapping.getDeliveredQuantity() : 0;
                            deliveredMap.put(mapping.getItemId().intValue(), prev + cur);
                        }
                    }
                }
            }
        }

        String mainSupplierName = "—";
        if (requests != null && !requests.isEmpty()) {
            for (BookingRequest req : requests) {
                String sName = resolveSupplierName(req);
                if (sName != null && !sName.trim().isEmpty() && !"—".equals(sName)) {
                    mainSupplierName = sName;
                    break;
                }
            }
        }

        Set<Integer> processedItemIds = new HashSet<>();

        // Priority 1: Process items from BookingRequestItemMapping table
        if (itemMappings != null && !itemMappings.isEmpty()) {
            for (com.erp.erpsoftware.entity.BookingRequestItemMapping mapping : itemMappings) {
                if (mapping.getItemId() == null) continue;
                processedItemIds.add(mapping.getItemId());

                String defaultCoNo = String.format("CO-%05d", Math.abs(mapping.getBookingNo().trim().hashCode()) % 90000 + 10000);
                String resolvedCoNo = (existingOrders != null && !existingOrders.isEmpty() && existingOrders.get(0).getSalesOrderNo() != null && !existingOrders.get(0).getSalesOrderNo().trim().isEmpty()) 
                        ? existingOrders.get(0).getSalesOrderNo().trim() 
                        : defaultCoNo;

                Map<String, Object> map = new HashMap<>();
                map.put("requestId", mapping.getBookingNo());
                map.put("bookingNo", mapping.getBookingNo());
                map.put("supplierName", mainSupplierName);
                map.put("salesOrderNo", resolvedCoNo);
                map.put("bookId", mapping.getItemId());
                map.put("itemId", mapping.getItemId());

                int qty = (mapping.getQuantity() != null && mapping.getQuantity() > 0) ? mapping.getQuantity() : 1;
                map.put("quantity", qty);
                int rawDelivered = deliveredMap.getOrDefault(mapping.getItemId(), 0);
                map.put("deliveredQty", Math.min(qty, rawDelivered));

                List<Map<String, Object>> savedOrdersList = new ArrayList<>();
                if (existingOrders != null) {
                    for (int i = 0; i < existingOrders.size(); i++) {
                        SalesOrder so = existingOrders.get(i);
                        Map<String, Object> soMap = new HashMap<>();
                        soMap.put("orderId", so.getOrderId());
                        soMap.put("salesOrderNo", so.getSalesOrderNo());
                        soMap.put("entryDate", so.getEntryDate() != null ? sdf.format(so.getEntryDate()) : "");
                        soMap.put("totalCost", so.getTotalCost() != null ? so.getTotalCost() : 0.0);

                        int itemSendQtyInThisSO = 0;
                        if (so.getDetails() != null) {
                            for (SalesOrderDetail d : so.getDetails()) {
                                if (d.getItemId() != null && d.getItemId().equals(mapping.getItemId().longValue())) {
                                    itemSendQtyInThisSO = d.getDeliveredQuantity() != null ? d.getDeliveredQuantity() : 0;
                                    break;
                                }
                            }
                        }
                        soMap.put("sendQty", itemSendQtyInThisSO);

                        int prevQtyBeforeThisSO = 0;
                        for (int j = i + 1; j < existingOrders.size(); j++) {
                            SalesOrder olderSO = existingOrders.get(j);
                            if (olderSO.getDetails() != null) {
                                for (SalesOrderDetail d : olderSO.getDetails()) {
                                    if (d.getItemId() != null && d.getItemId().equals(mapping.getItemId().longValue())) {
                                        prevQtyBeforeThisSO += (d.getDeliveredQuantity() != null ? d.getDeliveredQuantity() : 0);
                                        break;
                                    }
                                }
                            }
                        }
                        soMap.put("prevDeliveredQty", prevQtyBeforeThisSO);
                        savedOrdersList.add(soMap);
                    }
                }
                map.put("savedOrders", savedOrdersList);

                int remainingQty = Math.max(0, qty - rawDelivered);
                map.put("remainingQty", remainingQty);

                int latestSendQty = 0;
                int latestPrevDeliveredQty = rawDelivered;
                if (!savedOrdersList.isEmpty()) {
                    latestSendQty = (Integer) savedOrdersList.get(0).get("sendQty");
                }
                latestSendQty = Math.min(remainingQty, latestSendQty);
                map.put("savedSendQty", latestSendQty);
                map.put("latestSendQty", latestSendQty);
                map.put("latestPrevDeliveredQty", latestPrevDeliveredQty);

                Item item = itemService.getItemById(mapping.getItemId().longValue());
                if (item != null) {
                    map.put("itemCode", item.getItemCode() != null ? item.getItemCode() : "N/A");
                    map.put("itemName", item.getItemName() != null ? item.getItemName() : "N/A");
                    map.put("rate", item.getRate() != null ? item.getRate() : 0.0);
                    map.put("itemType", (item.getType() != null && item.getType().getTypeName() != null) ? item.getType().getTypeName() : "N/A");
                } else {
                    map.put("itemCode", "N/A");
                    map.put("itemName", "Item #" + mapping.getItemId());
                    map.put("rate", 0.0);
                    map.put("itemType", "N/A");
                }

                com.erp.erpsoftware.entity.CurrentStock cs = currentStockService.getStockByItemId(mapping.getItemId().longValue());
                double stockVal = (cs != null && cs.getCurrentStock() != null) ? cs.getCurrentStock() : 0.0;
                map.put("currentStock", (int) Math.round(stockVal));

                list.add(map);
            }
        }

        // book_id removed from BookingRequest; all item data comes from BookingRequestItemMapping above

        return list;
    }

    @PostMapping("/save")
    public String saveSale(@RequestParam(value = "itemIds", required = false) List<Integer> itemIds,
                           @RequestParam(value = "quantities", required = false) List<Integer> quantities,
                           @RequestParam(value = "bookedQuantities", required = false) List<Integer> bookedQuantities,
                           @RequestParam(value = "supplierNames", required = false) List<String> supplierNames,
                           @RequestParam(value = "bookingNo", required = false) String bookingNo,
                           RedirectAttributes redirectAttributes) {
        try {
            if (itemIds == null || itemIds.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Error: No items added to the sales request.");
                return "redirect:/sales/new" + (bookingNo != null && !bookingNo.trim().isEmpty() ? "?bookingNo=" + bookingNo : "");
            }

            // Validate available stock for each requested item
            for (int i = 0; i < itemIds.size(); i++) {
                Long itemId = itemIds.get(i).longValue();
                int sentQty = (quantities != null && i < quantities.size() && quantities.get(i) != null) ? quantities.get(i) : 0;
                if (sentQty > 0) {
                    com.erp.erpsoftware.entity.CurrentStock cs = currentStockService.getStockByItemId(itemId);
                    double stockVal = (cs != null && cs.getCurrentStock() != null) ? cs.getCurrentStock() : 0.0;
                    if (stockVal < 0) stockVal = 0.0;

                    if (sentQty > stockVal) {
                        com.erp.erpsoftware.entity.Item item = itemService.getItemById(itemId);
                        String itemName = (item != null && item.getItemName() != null) ? item.getItemName() : "Item #" + itemId;
                        redirectAttributes.addFlashAttribute("errorMessage", "Selected stock is currently unavailable for " + itemName + ". (Requested: " + sentQty + ", Available Stock: " + (int)Math.round(stockVal) + ")");
                        return "redirect:/sales/new" + (bookingNo != null && !bookingNo.trim().isEmpty() ? "?bookingNo=" + bookingNo : "");
                    }
                }
            }

            salesOrderService.saveSalesOrderFromForm(itemIds, quantities, bookedQuantities, supplierNames, bookingNo);

            redirectAttributes.addFlashAttribute("successMessage", "Sales Order saved successfully!");
            return "redirect:/sales";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error saving Sales Order: " + e.getMessage());
            return "redirect:/sales/new" + (bookingNo != null && !bookingNo.trim().isEmpty() ? "?bookingNo=" + bookingNo : "");
        }
    }

    @GetMapping("/view/{bookingNo}")
    public String viewSalesBooking(@PathVariable String bookingNo, Model model, RedirectAttributes redirectAttributes) {
        String resolvedBookingNo = bookingNo != null ? bookingNo.trim() : "";
        if (resolvedBookingNo.startsWith("CO-")) {
            List<SalesOrder> sos = salesOrderService.getSalesOrderBySalesOrderNo(resolvedBookingNo);
            if (sos != null && !sos.isEmpty() && sos.get(0).getBookingNo() != null && !sos.get(0).getBookingNo().trim().isEmpty()) {
                resolvedBookingNo = sos.get(0).getBookingNo().trim();
            }
        }
        try {
            List<BookingRequest> requests = bookingRequestService.getRequestsByBookingNo(resolvedBookingNo);
            List<com.erp.erpsoftware.entity.BookingRequestItemMapping> itemMappings = bookingRequestService.getItemMappingsByBookingNo(resolvedBookingNo);

            if ((requests == null || requests.isEmpty()) && (itemMappings == null || itemMappings.isEmpty())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Challan Order not found with order number: " + bookingNo);
                return "redirect:/sales";
            }

            // Use only BookingRequestItemMapping for item-level data
            model.addAttribute("bookingNo", resolvedBookingNo);

            Set<String> suppliers = new LinkedHashSet<>();
            double grandTotal = 0.0;
            Map<Integer, Item> itemMap = new HashMap<>();

            // Supplier from header booking_request row
            if (requests != null) {
                for (BookingRequest req : requests) {
                    String sName = resolveSupplierName(req);
                    if (sName != null && !sName.trim().isEmpty() && !"—".equals(sName)) {
                        suppliers.add(sName);
                    }
                }
            }

            // Items and totals from item mapping
            if (itemMappings != null) {
                for (com.erp.erpsoftware.entity.BookingRequestItemMapping m : itemMappings) {
                    if (m.getItemId() != null) {
                        try {
                            Item item = itemService.getItemById(m.getItemId().longValue());
                            if (item != null) {
                                itemMap.put(m.getItemId(), item);
                                int qty = m.getQuantity() != null ? m.getQuantity() : 0;
                                double rate = item.getRate() != null ? item.getRate() : 0.0;
                                grandTotal += rate * qty;
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }

            String supplierNames = suppliers.isEmpty() ? "—" : String.join(", ", suppliers);
            model.addAttribute("supplierNames", supplierNames);
            model.addAttribute("requests", requests);
            model.addAttribute("itemMappings", itemMappings);
            model.addAttribute("itemMap", itemMap);
            model.addAttribute("grandTotal", grandTotal);

            List<SalesOrder> existingOrders = salesOrderService.getAllSalesOrdersByBookingNo(resolvedBookingNo);
            Map<Integer, Integer> deliveredMap = new HashMap<>();
            if (existingOrders != null && !existingOrders.isEmpty()) {
                for (SalesOrder so : existingOrders) {
                    if (so.getDetails() != null) {
                        for (SalesOrderDetail mapping : so.getDetails()) {
                            if (mapping.getItemId() != null) {
                                int prev = deliveredMap.getOrDefault(mapping.getItemId().intValue(), 0);
                                int cur = mapping.getDeliveredQuantity() != null ? mapping.getDeliveredQuantity() : 0;
                                deliveredMap.put(mapping.getItemId().intValue(), prev + cur);
                            }
                        }
                    }
                }
            }
            model.addAttribute("deliveredMap", deliveredMap);

            int status = (existingOrders != null && !existingOrders.isEmpty()) ? existingOrders.get(0).getStatus() : 1;
            model.addAttribute("status", status == 3 ? "Delivered" : (status == 2 ? "Partially Delivered" : "Pending"));

            // Get all saved sales orders for this bookingNo to display multiple save history (only non-empty orders with details)
            List<SalesOrder> validSavedSalesOrders = new ArrayList<>();
            if (existingOrders != null) {
                for (SalesOrder so : existingOrders) {
                    if (so.getDetails() != null && !so.getDetails().isEmpty()) {
                        validSavedSalesOrders.add(so);
                        for (SalesOrderDetail d : so.getDetails()) {
                            if (d.getItemId() != null && !itemMap.containsKey(d.getItemId().intValue())) {
                                try {
                                    Item itm = itemService.getItemById(d.getItemId());
                                    if (itm != null) itemMap.put(d.getItemId().intValue(), itm);
                                } catch (Exception ignored) {}
                            }
                        }
                    }
                }
            }
            model.addAttribute("savedSalesOrders", validSavedSalesOrders);

            return "sales-booking-view";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error loading booking details: " + e.getMessage());
            return "redirect:/sales";
        }
    }

    @GetMapping("/challan-details/{orderNo}")
    @ResponseBody
    public Map<String, Object> getChallanDetails(@PathVariable String orderNo) {
        Map<String, Object> result = new HashMap<>();
        String resolvedBookingNo = orderNo != null ? orderNo.trim() : "";
        String requestedCo = orderNo;

        if (resolvedBookingNo.startsWith("CO-")) {
            List<SalesOrder> sos = salesOrderService.getSalesOrderBySalesOrderNo(resolvedBookingNo);
            if (sos != null && !sos.isEmpty() && sos.get(0).getBookingNo() != null && !sos.get(0).getBookingNo().trim().isEmpty()) {
                resolvedBookingNo = sos.get(0).getBookingNo().trim();
            }
        }

        List<BookingRequest> requests = bookingRequestService.getRequestsByBookingNo(resolvedBookingNo);
        List<com.erp.erpsoftware.entity.BookingRequestItemMapping> itemMappings = bookingRequestService.getItemMappingsByBookingNo(resolvedBookingNo);
        List<SalesOrder> existingOrders = salesOrderService.getAllSalesOrdersByBookingNo(resolvedBookingNo);

        String coNo = (requestedCo != null && requestedCo.startsWith("CO-")) ? requestedCo :
                (existingOrders != null && !existingOrders.isEmpty() && existingOrders.get(0).getSalesOrderNo() != null ? existingOrders.get(0).getSalesOrderNo() : "CO-" + resolvedBookingNo);

        String supplierName = "—";
        if (requests != null) {
            for (BookingRequest req : requests) {
                String s = resolveSupplierName(req);
                if (s != null && !s.trim().isEmpty() && !"—".equals(s)) {
                    supplierName = s;
                    break;
                }
            }
        }

        String orderDate = "-";
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd-MMM-yyyy hh:mm a");
        if (existingOrders != null && !existingOrders.isEmpty() && existingOrders.get(0).getEntryDate() != null) {
            orderDate = sdf.format(existingOrders.get(0).getEntryDate());
        } else if (requests != null && !requests.isEmpty() && requests.get(0).getEntryDate() != null) {
            orderDate = sdf.format(requests.get(0).getEntryDate());
        }

        Map<Integer, Integer> deliveredMap = new HashMap<>();
        if (existingOrders != null) {
            for (SalesOrder so : existingOrders) {
                if (so.getDetails() != null) {
                    for (SalesOrderDetail d : so.getDetails()) {
                        if (d.getItemId() != null) {
                            int prev = deliveredMap.getOrDefault(d.getItemId().intValue(), 0);
                            int cur = d.getDeliveredQuantity() != null ? d.getDeliveredQuantity() : 0;
                            deliveredMap.put(d.getItemId().intValue(), prev + cur);
                        }
                    }
                }
            }
        }

        List<Map<String, Object>> items = new ArrayList<>();
        double grandTotal = 0.0;
        int totalQty = 0;
        int totalDelivered = 0;

        if (itemMappings != null) {
            int slNo = 1;
            for (com.erp.erpsoftware.entity.BookingRequestItemMapping m : itemMappings) {
                if (m.getItemId() == null) continue;
                Item item = itemService.getItemById(m.getItemId().longValue());
                int qty = m.getQuantity() != null ? m.getQuantity() : 0;
                int delQty = deliveredMap.getOrDefault(m.getItemId(), 0);
                double rate = item != null && item.getRate() != null ? item.getRate() : 0.0;
                double cost = rate * qty;

                grandTotal += cost;
                totalQty += qty;
                totalDelivered += delQty;

                Map<String, Object> itemMap = new HashMap<>();
                itemMap.put("slNo", slNo++);
                itemMap.put("itemCode", item != null && item.getItemCode() != null ? item.getItemCode() : "N/A");
                itemMap.put("itemName", item != null && item.getItemName() != null ? item.getItemName() : "Item #" + m.getItemId());
                itemMap.put("itemType", (item != null && item.getType() != null && item.getType().getTypeName() != null) ? item.getType().getTypeName() : "N/A");
                itemMap.put("rate", rate);
                itemMap.put("quantity", qty);
                itemMap.put("deliveredQty", delQty);
                itemMap.put("totalCost", cost);
                items.add(itemMap);
            }
        }

        String status = (totalDelivered >= totalQty && totalQty > 0) ? "Delivered" : (totalDelivered > 0 ? "Partially Delivered" : "Pending");

        result.put("salesOrderNo", coNo);
        result.put("bookingNo", resolvedBookingNo);
        result.put("supplierName", supplierName);
        result.put("orderDate", orderDate);
        result.put("status", status);
        result.put("grandTotal", grandTotal);
        result.put("totalQuantity", totalQty);
        result.put("totalDelivered", totalDelivered);
        result.put("items", items);

        return result;
    }

    @Autowired
    private com.erp.erpsoftware.service.StockLedgerService stockLedgerService;

    @PostMapping("/delete/{bookingNo}")
    public String deleteSalesOrder(@PathVariable String bookingNo, RedirectAttributes redirectAttributes) {
        try {
            salesOrderService.deleteByBookingNo(bookingNo);
            redirectAttributes.addFlashAttribute("successMessage", "Sales Order for " + bookingNo + " deleted successfully! Status reset to Pending.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting Sales Order: " + e.getMessage());
        }
        return "redirect:/sales";
    }

    @PostMapping("/reach/{bookingNo}")
    public String markAsReached(@PathVariable String bookingNo, RedirectAttributes redirectAttributes) {
        try {
            List<BookingRequest> requests = bookingRequestService.getRequestsByBookingNo(bookingNo);
            List<SalesOrder> orders = salesOrderService.getAllSalesOrdersByBookingNo(bookingNo);

            if (orders != null && !orders.isEmpty()) {
                for (SalesOrder so : orders) {
                    so.setStatus(3);
                    salesOrderService.save(so);

                    if (so.getDetails() != null) {
                        for (SalesOrderDetail d : so.getDetails()) {
                            double alreadyDeducted = stockLedgerRepository.sumSendQuantityByOrderIdAndItemId(so.getOrderId(), d.getItemId());
                            double dQty = d.getDeliveredQuantity() != null ? d.getDeliveredQuantity() : 0;
                            double toDeduct = dQty - alreadyDeducted;
                            if (toDeduct > 0) {
                                currentStockService.deductStock(d.getItemId(), toDeduct, so.getOrderId(), "SALES_ORDER");
                            }
                        }
                    }
                }
            }

            for (BookingRequest req : requests) {
                req.setStatus("Delivered");
                bookingRequestService.save(req);
            }
            
            redirectAttributes.addFlashAttribute("successMessage", "Booking status updated to Reached!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating status: " + e.getMessage());
        }
        return "redirect:/sales";
    }
}
