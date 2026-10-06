package com.erp.erpsoftware.controller;

import com.erp.erpsoftware.entity.*;
import com.erp.erpsoftware.repository.SalesOrderDetailRepository;
import com.erp.erpsoftware.repository.SalesOrderRepository;
import com.erp.erpsoftware.service.BookingRequestService;
import com.erp.erpsoftware.service.CurrentStockService;
import com.erp.erpsoftware.service.ItemService;
import com.erp.erpsoftware.service.SalesOrderService;
import com.erp.erpsoftware.service.StockLedgerService;
import com.erp.erpsoftware.service.SupplierService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Controller
@RequestMapping({"/delivered-sales", "/delivered-quantity"})
public class DeliveredQuantityController {

    @Autowired
    private SalesOrderService salesOrderService;

    @Autowired
    private BookingRequestService bookingRequestService;

    @Autowired
    private SupplierService supplierService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private SalesOrderRepository salesOrderRepository;

    @Autowired
    private SalesOrderDetailRepository detailRepository;

    @Autowired
    private StockLedgerService stockLedgerService;

    @Autowired
    private CurrentStockService currentStockService;

    private Date getNowWithoutMillis() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

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
        List<Supplier> allSuppliers = supplierService.getAllSuppliers();
        if (allSuppliers != null && !allSuppliers.isEmpty()) {
            return allSuppliers.get(0).getSupplierName();
        }
        return "—";
    }

    private void ensureSupplierName(SalesOrder order, String bookingNo) {
        if (order == null) return;
        if (order.getSupplierName() == null || order.getSupplierName().trim().isEmpty() || "—".equals(order.getSupplierName().trim()) || order.getSupplierName().startsWith("Unknown")) {
            if (bookingNo != null && !bookingNo.trim().isEmpty()) {
                List<BookingRequest> reqs = bookingRequestService.getRequestsByBookingNo(bookingNo);
                if (reqs != null && !reqs.isEmpty()) {
                    String resolvedName = resolveSupplierName(reqs.get(0));
                    if (resolvedName != null && !"—".equals(resolvedName)) {
                        order.setSupplierName(resolvedName);
                        return;
                    }
                }
            }
            if (order.getSupplierId() != null) {
                try {
                    Supplier supplier = supplierService.getSupplierById(order.getSupplierId());
                    if (supplier != null && supplier.getSupplierName() != null && !supplier.getSupplierName().isEmpty()) {
                        order.setSupplierName(supplier.getSupplierName());
                        return;
                    }
                } catch (Exception ignored) {}
            }
            List<Supplier> allSuppliers = supplierService.getAllSuppliers();
            if (allSuppliers != null && !allSuppliers.isEmpty()) {
                order.setSupplierName(allSuppliers.get(0).getSupplierName());
            } else {
                order.setSupplierName("—");
            }
        }
    }

    @GetMapping
    public String listDeliveredSales(Model model) {
        try {
            List<BookingRequest> requests = bookingRequestService.getAllBookingRequests();

            // Load all items into map for lookups
            Map<Integer, Item> itemMap = new HashMap<>();
            for (Item item : itemService.getAllItems()) {
                if (item.getItemId() != null) {
                    itemMap.put(item.getItemId().intValue(), item);
                }
            }
            model.addAttribute("itemMap", itemMap);

            List<SalesOrder> allSalesOrders = salesOrderService.getAllSalesOrders();

            // Group booking headers by bookingNo
            Map<String, BookingRequest> bookingHeaderMap = new LinkedHashMap<>();
            if (requests != null) {
                for (BookingRequest req : requests) {
                    String bNo = req.getBookingNo();
                    if (bNo != null && !bNo.trim().isEmpty() && !bookingHeaderMap.containsKey(bNo)) {
                        bookingHeaderMap.put(bNo, req);
                    }
                }
            }

            // Group SalesOrders by bookingNo to eliminate duplicates
            Map<String, List<SalesOrder>> groupedSalesOrders = new LinkedHashMap<>();
            if (allSalesOrders != null) {
                for (SalesOrder so : allSalesOrders) {
                    String bNo = so.getBookingNo();
                    if (bNo != null && !bNo.trim().isEmpty()) {
                        groupedSalesOrders.computeIfAbsent(bNo, k -> new ArrayList<>()).add(so);
                    }
                }
            }

            List<SalesOrder> deliveredSalesOrders = new ArrayList<>();
            Map<String, List<String>> bookingItemNames = new HashMap<>();
            Set<String> processedBookingNos = new HashSet<>();

            for (Map.Entry<String, List<SalesOrder>> entry : groupedSalesOrders.entrySet()) {
                String bNo = entry.getKey();
                List<SalesOrder> orderList = entry.getValue();
                if (orderList == null || orderList.isEmpty()) continue;

                SalesOrder summarySO = orderList.get(0);
                ensureSupplierName(summarySO, bNo);
                processedBookingNos.add(bNo);

                List<BookingRequestItemMapping> itemMappings = bookingRequestService.getItemMappingsByBookingNo(bNo);
                Map<Long, Integer> totalSentMap = new HashMap<>();
                Date latestEntryDate = summarySO.getEntryDate();

                for (SalesOrder so : orderList) {
                    if (so.getEntryDate() != null) {
                        latestEntryDate = so.getEntryDate();
                    }
                    if (so.getDetails() != null) {
                        for (SalesOrderDetail d : so.getDetails()) {
                            if (d.getItemId() != null) {
                                int curr = totalSentMap.getOrDefault(d.getItemId(), 0);
                                int sent = (d.getDeliveredQuantity() != null) ? d.getDeliveredQuantity() : 0;
                                totalSentMap.put(d.getItemId(), curr + sent);
                            }
                        }
                    }
                }
                if (latestEntryDate != null) summarySO.setEntryDate(latestEntryDate);

                int totalBookedQty = 0;
                int totalSentQty = 0;
                double calcCost = 0.0;
                List<String> itemNames = new ArrayList<>();

                if (itemMappings != null && !itemMappings.isEmpty()) {
                    for (BookingRequestItemMapping m : itemMappings) {
                        if (m.getItemId() == null) continue;
                        int bQty = m.getQuantity() != null ? m.getQuantity() : 0;
                        int sQty = totalSentMap.getOrDefault(m.getItemId().longValue(), 0);
                        totalBookedQty += bQty;
                        totalSentQty += sQty;

                        Item itm = itemMap.get(m.getItemId());
                        double rate = (itm != null && itm.getRate() != null) ? itm.getRate() : 0.0;
                        calcCost += (rate * sQty);
                        if (itm != null && itm.getItemName() != null) {
                            itemNames.add(itm.getItemName());
                        }
                    }
                } else if (summarySO.getDetails() != null) {
                    for (SalesOrderDetail d : summarySO.getDetails()) {
                        int bQty = d.getQuantity() != null ? d.getQuantity() : 0;
                        int sQty = d.getDeliveredQuantity() != null ? d.getDeliveredQuantity() : 0;
                        totalBookedQty += bQty;
                        totalSentQty += sQty;
                        calcCost += (d.getTotalCost() != null ? d.getTotalCost() : 0.0);

                        Item itm = itemMap.get(d.getItemId().intValue());
                        if (itm != null && itm.getItemName() != null) {
                            itemNames.add(itm.getItemName());
                        } else if (d.getItem() != null && d.getItem().getItemName() != null) {
                            itemNames.add(d.getItem().getItemName());
                        }
                    }
                }

                summarySO.setTotalQuantity(totalSentQty);
                summarySO.setTotalCost(calcCost);
                bookingItemNames.put(bNo, itemNames);

                // Finalized check: An order is Delivered (status = 3) when all sent quantities have been delivered.
                boolean allSentItemsDelivered = true;
                int totalSentCount = 0;
                int totalAccountedDeliv = 0;

                if (summarySO.getDetails() != null && !summarySO.getDetails().isEmpty()) {
                    for (SalesOrderDetail d : summarySO.getDetails()) {
                        int sQty = d.getDeliveredQuantity() != null ? d.getDeliveredQuantity() : 0;
                        int aQty = d.getActualDeliveredQuantity() != null ? d.getActualDeliveredQuantity() : 0;
                        int dmg = d.getDamagedQuantity() != null ? d.getDamagedQuantity() : 0;
                        int ret = d.getReturnedQuantity() != null ? d.getReturnedQuantity() : 0;

                        totalSentCount += sQty;
                        totalAccountedDeliv += (aQty + dmg + ret);

                        if (sQty > 0 && (aQty + dmg + ret) < sQty) {
                            allSentItemsDelivered = false;
                        }
                    }
                } else {
                    allSentItemsDelivered = false;
                }

                if (totalAccountedDeliv == 0) {
                    summarySO.setStatus(1); // Pending
                } else if (allSentItemsDelivered && totalSentCount > 0 && totalAccountedDeliv >= totalSentCount) {
                    summarySO.setStatus(3); // Delivered
                } else {
                    summarySO.setStatus(2); // Partially Delivered
                }

                deliveredSalesOrders.add(summarySO);
            }

            model.addAttribute("deliveredSalesOrders", deliveredSalesOrders);
            model.addAttribute("bookingItemNames", bookingItemNames);

        } catch (Exception e) {
            System.err.println("Error loading delivered quantity list: " + e.getMessage());
            model.addAttribute("deliveredSalesOrders", new ArrayList<>());
            model.addAttribute("errorMessage", "Error loading delivered quantity list: " + e.getMessage());
        }

        return "delivered-quantity-list";
    }

    @GetMapping("/details/{bookingNo}")
    public String viewDeliveredDetails(@PathVariable String bookingNo, Model model, RedirectAttributes redirectAttributes) {
        try {
            List<BookingRequest> requests = bookingRequestService.getRequestsByBookingNo(bookingNo);
            List<BookingRequestItemMapping> itemMappings = bookingRequestService.getItemMappingsByBookingNo(bookingNo);
            List<SalesOrder> orders = salesOrderService.getAllSalesOrdersByBookingNo(bookingNo);

            if ((requests == null || requests.isEmpty()) && (itemMappings == null || itemMappings.isEmpty()) && (orders == null || orders.isEmpty())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Delivered Quantity record not found for: " + bookingNo);
                return "redirect:/delivered-sales";
            }

            Map<Integer, Item> itemMap = new HashMap<>();
            for (Item item : itemService.getAllItems()) {
                if (item.getItemId() != null) {
                    itemMap.put(item.getItemId().intValue(), item);
                }
            }
            model.addAttribute("itemMap", itemMap);

            Map<Long, Integer> totalSentMap = new HashMap<>();
            Map<Long, SalesOrderDetail> savedDetailsMap = new HashMap<>();
            Date latestSalesEntryDate = null;
            SalesOrder targetOrder = null;

            if (orders != null && !orders.isEmpty()) {
                targetOrder = orders.get(0);
                for (SalesOrder so : orders) {
                    if (latestSalesEntryDate == null && so.getEntryDate() != null) {
                        latestSalesEntryDate = so.getEntryDate();
                    }
                    if (so.getDetails() != null) {
                        for (SalesOrderDetail d : so.getDetails()) {
                            if (d.getItemId() != null) {
                                int current = totalSentMap.getOrDefault(d.getItemId(), 0);
                                int sent = (d.getDeliveredQuantity() != null) ? d.getDeliveredQuantity() : 0;
                                totalSentMap.put(d.getItemId(), current + sent);
                                savedDetailsMap.put(d.getItemId(), d);
                            }
                        }
                    }
                }
            }

            if (targetOrder == null) {
                targetOrder = new SalesOrder();
                targetOrder.setBookingNo(bookingNo);
                int seed = Math.abs(bookingNo.trim().hashCode()) % 90000 + 10000;
                targetOrder.setSalesOrderNo(String.format("CO-%05d", seed));
                targetOrder.setStatus(3);
            }

            if (latestSalesEntryDate != null) {
                targetOrder.setEntryDate(latestSalesEntryDate);
            } else if (targetOrder.getEntryDate() == null && requests != null && !requests.isEmpty()) {
                targetOrder.setEntryDate(requests.get(0).getEntryDate());
            }

            ensureSupplierName(targetOrder, bookingNo);

            List<SalesOrderDetail> mergedDetails = new ArrayList<>();
            int slNo = 1;

            if (itemMappings != null && !itemMappings.isEmpty()) {
                for (BookingRequestItemMapping m : itemMappings) {
                    if (m.getItemId() == null) continue;
                    Long itemId = m.getItemId().longValue();
                    SalesOrderDetail d = new SalesOrderDetail();
                    d.setSlNo(slNo++);
                    d.setItemId(itemId);

                    Item item = itemMap.get(m.getItemId());
                    d.setItem(item);

                    int bookedQty = m.getQuantity() != null ? m.getQuantity() : 0;
                    d.setQuantity(bookedQty);

                    int sentQty = totalSentMap.getOrDefault(itemId, 0);
                    d.setDeliveredQuantity(sentQty);

                    SalesOrderDetail savedD = savedDetailsMap.get(itemId);
                    if (savedD != null) {
                        d.setActualDeliveredQuantity(savedD.getActualDeliveredQuantity());
                        d.setDamagedQuantity(savedD.getDamagedQuantity());
                        d.setReturnedQuantity(savedD.getReturnedQuantity());
                        d.setReturnedDate(savedD.getReturnedDate());
                    } else {
                        d.setActualDeliveredQuantity(sentQty);
                        d.setDamagedQuantity(0);
                        d.setReturnedQuantity(0);
                    }
                    mergedDetails.add(d);
                }
            } else if (targetOrder.getDetails() != null) {
                for (SalesOrderDetail d : targetOrder.getDetails()) {
                    if (d.getItemId() == null) continue;
                    d.setSlNo(slNo++);
                    if (itemMap.containsKey(d.getItemId().intValue())) {
                        d.setItem(itemMap.get(d.getItemId().intValue()));
                    }
                    mergedDetails.add(d);
                }
            }

            targetOrder.setDetails(mergedDetails);
            model.addAttribute("so", targetOrder);
            return "delivered-quantity-details";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error loading details: " + e.getMessage());
            return "redirect:/delivered-sales";
        }
    }

    @GetMapping("/edit/{bookingNo}")
    public String editDeliveredForm(@PathVariable String bookingNo, Model model, RedirectAttributes redirectAttributes) {
        try {
            List<BookingRequest> requests = bookingRequestService.getRequestsByBookingNo(bookingNo);
            List<BookingRequestItemMapping> itemMappings = bookingRequestService.getItemMappingsByBookingNo(bookingNo);
            List<SalesOrder> orders = salesOrderService.getAllSalesOrdersByBookingNo(bookingNo);

            if ((requests == null || requests.isEmpty()) && (itemMappings == null || itemMappings.isEmpty()) && (orders == null || orders.isEmpty())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Delivered Quantity record not found for: " + bookingNo);
                return "redirect:/delivered-sales";
            }

            Map<Integer, Item> itemMap = new HashMap<>();
            for (Item item : itemService.getAllItems()) {
                if (item.getItemId() != null) {
                    itemMap.put(item.getItemId().intValue(), item);
                }
            }
            model.addAttribute("itemMap", itemMap);

            Map<Long, Double> stockMap = new HashMap<>();
            List<CurrentStock> allStocks = currentStockService.getAllStocks();
            if (allStocks != null) {
                for (CurrentStock cs : allStocks) {
                    if (cs.getItemId() != null) {
                        stockMap.put(cs.getItemId(), cs.getCurrentStock() != null ? cs.getCurrentStock() : 0.0);
                    }
                }
            }
            model.addAttribute("stockMap", stockMap);

            // Compute total cumulative sent quantities and find the date of sent quantity from Sales Request tab
            Map<Long, Integer> totalSentMap = new HashMap<>();
            Map<Long, SalesOrderDetail> savedDetailsMap = new HashMap<>();
            Date latestSalesEntryDate = null;
            SalesOrder targetOrder = null;

            if (orders != null && !orders.isEmpty()) {
                targetOrder = orders.get(0);
                for (SalesOrder so : orders) {
                    if (latestSalesEntryDate == null && so.getEntryDate() != null) {
                        latestSalesEntryDate = so.getEntryDate();
                    }
                    if (so.getDetails() != null) {
                        for (SalesOrderDetail d : so.getDetails()) {
                            if (d.getItemId() != null) {
                                int current = totalSentMap.getOrDefault(d.getItemId(), 0);
                                int sent = (d.getDeliveredQuantity() != null) ? d.getDeliveredQuantity() : 0;
                                totalSentMap.put(d.getItemId(), current + sent);
                                savedDetailsMap.put(d.getItemId(), d);
                            }
                        }
                    }
                }
            }

            if (targetOrder == null) {
                targetOrder = new SalesOrder();
                targetOrder.setBookingNo(bookingNo);
                int seed = Math.abs(bookingNo.trim().hashCode()) % 90000 + 10000;
                targetOrder.setSalesOrderNo(String.format("CO-%05d", seed));
                targetOrder.setStatus(3);
            }

            if (latestSalesEntryDate != null) {
                targetOrder.setEntryDate(latestSalesEntryDate);
            } else if (targetOrder.getEntryDate() == null && requests != null && !requests.isEmpty()) {
                targetOrder.setEntryDate(requests.get(0).getEntryDate());
            }

            ensureSupplierName(targetOrder, bookingNo);

            // Build merged details ensuring all booked items for this bookingNo are present
            List<SalesOrderDetail> mergedDetails = new ArrayList<>();
            int totalQty = 0;
            double totalCost = 0.0;
            int slNo = 1;

            if (itemMappings != null && !itemMappings.isEmpty()) {
                for (BookingRequestItemMapping m : itemMappings) {
                    if (m.getItemId() == null) continue;
                    Long itemId = m.getItemId().longValue();
                    SalesOrderDetail d = new SalesOrderDetail();
                    d.setSlNo(slNo++);
                    d.setOrderId(targetOrder.getOrderId());
                    d.setItemId(itemId);

                    Item item = itemMap.get(m.getItemId());
                    d.setItem(item);

                    int bookedQty = m.getQuantity() != null ? m.getQuantity() : 0;
                    d.setQuantity(bookedQty);

                    // Sent quantity ALWAYS comes from total cumulative sent quantity recorded in Sales!
                    int sentQty = totalSentMap.getOrDefault(itemId, 0);
                    d.setDeliveredQuantity(sentQty);

                    SalesOrderDetail savedD = savedDetailsMap.get(itemId);
                    if (savedD != null) {
                        d.setActualDeliveredQuantity(savedD.getActualDeliveredQuantity());
                        d.setDamagedQuantity(savedD.getDamagedQuantity());
                        d.setReturnedQuantity(savedD.getReturnedQuantity());
                        d.setReturnedDate(savedD.getReturnedDate());
                        d.setRate(savedD.getRate() != null ? savedD.getRate() : (item != null && item.getRate() != null ? item.getRate() : 0.0));
                    } else {
                        d.setActualDeliveredQuantity(sentQty);
                        d.setDamagedQuantity(0);
                        d.setReturnedQuantity(0);
                        double rate = item != null && item.getRate() != null ? item.getRate() : 0.0;
                        d.setRate(rate);
                    }

                    double cost = (d.getRate() != null ? d.getRate() : 0.0) * sentQty;
                    d.setTotalCost(cost);

                    totalQty += d.getActualDeliveredQuantity();
                    totalCost += cost;
                    mergedDetails.add(d);
                }
            } else if (targetOrder.getDetails() != null) {
                for (SalesOrderDetail d : targetOrder.getDetails()) {
                    if (d.getItemId() == null) continue;
                    d.setSlNo(slNo++);
                    if (itemMap.containsKey(d.getItemId().intValue())) {
                        d.setItem(itemMap.get(d.getItemId().intValue()));
                    }
                    int sentQty = totalSentMap.getOrDefault(d.getItemId(), d.getDeliveredQuantity() != null ? d.getDeliveredQuantity() : 0);
                    d.setDeliveredQuantity(sentQty);
                    mergedDetails.add(d);
                }
            }

            targetOrder.setDetails(mergedDetails);
            targetOrder.setTotalQuantity(totalQty);
            targetOrder.setTotalCost(totalCost);

            model.addAttribute("so", targetOrder);
            return "delivered-quantity-update";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error loading update page: " + e.getMessage());
            return "redirect:/delivered-sales";
        }
    }

    @PostMapping("/update")
    public String updateDeliveredSales(
            @RequestParam(value = "bookingNo", required = false) String bookingNo,
            @RequestParam(value = "orderId", required = false) Long orderId,
            @RequestParam(value = "itemIds", required = false) List<Long> itemIds,
            @RequestParam(value = "sentQuantities", required = false) List<Integer> sentQuantities,
            @RequestParam(value = "actualDeliveredQuantities", required = false) List<Integer> actualDeliveredQuantities,
            @RequestParam(value = "damagedQuantities", required = false) List<Integer> damagedQuantities,
            @RequestParam(value = "returnedQuantities", required = false) List<Integer> returnedQuantities,
            RedirectAttributes redirectAttributes) {
        try {
            if (itemIds != null && !itemIds.isEmpty()) {
                List<SalesOrder> existingOrders = null;
                if (orderId != null) {
                    try {
                        SalesOrder so = salesOrderService.getSalesOrderById(orderId);
                        if (so != null) existingOrders = Collections.singletonList(so);
                    } catch (Exception ignored) {}
                }
                if (existingOrders == null && bookingNo != null && !bookingNo.trim().isEmpty()) {
                    existingOrders = salesOrderService.getAllSalesOrdersByBookingNo(bookingNo);
                }

                if (existingOrders != null && !existingOrders.isEmpty()) {
                    // Pre-validation pass: Ensure NO item breaks validation rules before applying ANY changes
                    for (SalesOrder order : existingOrders) {
                        List<SalesOrderDetail> details = detailRepository.findActiveDetailsByOrderId(order.getOrderId());
                        if (details != null) {
                            for (SalesOrderDetail d : details) {
                                int idx = itemIds.indexOf(d.getItemId());
                                if (idx != -1) {
                                    int oldSent = (d.getDeliveredQuantity() != null) ? d.getDeliveredQuantity() : 0;
                                    int newSent = (sentQuantities != null && idx < sentQuantities.size() && sentQuantities.get(idx) != null) ? sentQuantities.get(idx) : oldSent;
                                    int actualDeliv = (actualDeliveredQuantities != null && idx < actualDeliveredQuantities.size() && actualDeliveredQuantities.get(idx) != null) ? actualDeliveredQuantities.get(idx) : (d.getActualDeliveredQuantity() != null ? d.getActualDeliveredQuantity() : newSent);

                                    int newReturned = (returnedQuantities != null && idx < returnedQuantities.size() && returnedQuantities.get(idx) != null) ? returnedQuantities.get(idx) : 0;
                                    int damaged = (damagedQuantities != null && idx < damagedQuantities.size() && damagedQuantities.get(idx) != null) ? damagedQuantities.get(idx) : 0;

                                    int sumBreakdown = actualDeliv + damaged + newReturned;
                                    int maxAllowed = Math.max(newSent, (d.getQuantity() != null ? d.getQuantity() : 0));
                                    if (sumBreakdown > maxAllowed) {
                                        redirectAttributes.addFlashAttribute("errorMessage", "Invalid value");
                                        return "redirect:/delivered-sales/edit/" + (bookingNo != null ? bookingNo : (order.getBookingNo() != null ? order.getBookingNo() : order.getOrderId()));
                                    }
                                }
                            }
                        }
                    }

                    // Execution pass: Apply updates only after ALL items have passed validation
                    for (SalesOrder order : existingOrders) {
                        List<SalesOrderDetail> details = detailRepository.findActiveDetailsByOrderId(order.getOrderId());
                        int newTotalQty = 0;
                        double newTotalCost = 0.0;

                        if (details != null) {
                            for (SalesOrderDetail d : details) {
                                int idx = itemIds.indexOf(d.getItemId());
                                if (idx != -1) {
                                    int oldSent = (d.getDeliveredQuantity() != null) ? d.getDeliveredQuantity() : 0;
                                    int newSent = (sentQuantities != null && idx < sentQuantities.size() && sentQuantities.get(idx) != null) ? sentQuantities.get(idx) : oldSent;
                                    int actualDeliv = (actualDeliveredQuantities != null && idx < actualDeliveredQuantities.size() && actualDeliveredQuantities.get(idx) != null) ? actualDeliveredQuantities.get(idx) : (d.getActualDeliveredQuantity() != null ? d.getActualDeliveredQuantity() : newSent);

                                    int oldReturned = d.getReturnedQuantity() != null ? d.getReturnedQuantity() : 0;
                                    int newReturned = (returnedQuantities != null && idx < returnedQuantities.size() && returnedQuantities.get(idx) != null) ? returnedQuantities.get(idx) : 0;
                                    int damaged = (damagedQuantities != null && idx < damagedQuantities.size() && damagedQuantities.get(idx) != null) ? damagedQuantities.get(idx) : 0;

                                    int sumBreakdown = actualDeliv + damaged + newReturned;
                                    if (sumBreakdown > newSent) {
                                        newSent = sumBreakdown;
                                    }

                                    // 1. Sent quantity difference: updates stock and Stock Ledger (Sent column)
                                    int diffSent = newSent - oldSent;
                                    if (diffSent > 0) {
                                        stockLedgerService.recordStockOut(d.getItemId(), (double) diffSent, order.getOrderId(), "DELIVERED_SALES_UPDATE");
                                    } else if (diffSent < 0) {
                                        int returnDelta = Math.abs(diffSent);
                                        stockLedgerService.recordStockIn(d.getItemId(), (double) returnDelta, order.getOrderId(), "DELIVERED_SALES_CORRECTION");
                                    }

                                    // 2. Delivered quantity: saved to DB record only (DOES NOT affect stock or ledger)

                                    // 3. Returned stock: added back to current stock and displayed in Received column in Stock Ledger
                                    int returnedDiff = newReturned - oldReturned;
                                    if (returnedDiff > 0) {
                                        stockLedgerService.recordStockIn(d.getItemId(), (double) returnedDiff, order.getOrderId(), "DELIVERED_SALES_RETURN");
                                    } else if (returnedDiff < 0) {
                                        int returnedDecrease = Math.abs(returnedDiff);
                                        stockLedgerService.recordStockOut(d.getItemId(), (double) returnedDecrease, order.getOrderId(), "DELIVERED_SALES_RETURN_CORRECTION");
                                    }

                                    d.setDeliveredQuantity(newSent);
                                    d.setActualDeliveredQuantity(actualDeliv);
                                    d.setDamagedQuantity(damaged);
                                    d.setReturnedQuantity(newReturned);
                                    if (newReturned > 0) {
                                        if (d.getReturnedDate() == null || newReturned != oldReturned) {
                                            d.setReturnedDate(getNowWithoutMillis());
                                        }
                                    } else {
                                        d.setReturnedDate(null);
                                    }

                                    double rate = d.getRate() != null ? d.getRate() : 0.0;
                                    double cost = rate * newSent;
                                    d.setTotalCost(cost);
                                    detailRepository.save(d);

                                    newTotalQty += actualDeliv;
                                    newTotalCost += cost;
                                }
                            }
                        }

                        boolean isAllDelivered = true;
                        int totalSentCount = 0;
                        int totalAccountedDeliv = 0;

                        if (details != null && !details.isEmpty()) {
                            for (SalesOrderDetail d : details) {
                                int aDeliv = d.getActualDeliveredQuantity() != null ? d.getActualDeliveredQuantity() : 0;
                                int dmg = d.getDamagedQuantity() != null ? d.getDamagedQuantity() : 0;
                                int ret = d.getReturnedQuantity() != null ? d.getReturnedQuantity() : 0;
                                int sent = d.getDeliveredQuantity() != null ? d.getDeliveredQuantity() : 0;

                                totalSentCount += sent;
                                totalAccountedDeliv += (aDeliv + dmg + ret);

                                if (sent > 0 && (aDeliv + dmg + ret) < sent) {
                                    isAllDelivered = false;
                                }
                            }
                        } else {
                            isAllDelivered = false;
                        }

                        if (totalAccountedDeliv == 0) {
                            order.setStatus(1); // Pending
                        } else if (isAllDelivered && totalSentCount > 0 && totalAccountedDeliv >= totalSentCount) {
                            order.setStatus(3); // Delivered
                        } else {
                            order.setStatus(2); // Partially Delivered
                        }

                        order.setTotalQuantity(newTotalQty);
                        order.setTotalCost(newTotalCost);
                        salesOrderRepository.save(order);
                    }
                } else if (bookingNo != null && !bookingNo.trim().isEmpty()) {
                    // Pre-validation pass for new order
                    for (int i = 0; i < itemIds.size(); i++) {
                        int sent = (sentQuantities != null && i < sentQuantities.size() && sentQuantities.get(i) != null) ? sentQuantities.get(i) : 0;
                        int actualDeliv = (actualDeliveredQuantities != null && i < actualDeliveredQuantities.size() && actualDeliveredQuantities.get(i) != null) ? actualDeliveredQuantities.get(i) : sent;
                        int damaged = (damagedQuantities != null && i < damagedQuantities.size() && damagedQuantities.get(i) != null) ? damagedQuantities.get(i) : 0;
                        int returned = (returnedQuantities != null && i < returnedQuantities.size() && returnedQuantities.get(i) != null) ? returnedQuantities.get(i) : 0;

                        int sumBreakdown = actualDeliv + damaged + returned;
                        if (sumBreakdown > sent && sent > 0) {
                            redirectAttributes.addFlashAttribute("errorMessage", "Invalid value");
                            return "redirect:/delivered-sales/edit/" + bookingNo;
                        }
                    }

                    // Create new SalesOrder if none previously existed
                    SalesOrder newOrder = new SalesOrder();
                    newOrder.setBookingNo(bookingNo);
                    int seed = Math.abs(bookingNo.trim().hashCode()) % 90000 + 10000;
                    newOrder.setSalesOrderNo(String.format("CO-%05d", seed));
                    SalesOrder savedOrder = salesOrderRepository.save(newOrder);
                    int newTotalQty = 0;
                    double newTotalCost = 0.0;
                    boolean isNewOrderAllDelivered = true;
                    int totalNewBooked = 0;
                    int totalNewAccounted = 0;

                    List<BookingRequestItemMapping> bookedMappings = bookingRequestService.getItemMappingsByBookingNo(bookingNo);
                    Map<Long, Integer> bookedQtyMap = new HashMap<>();
                    if (bookedMappings != null) {
                        for (BookingRequestItemMapping m : bookedMappings) {
                            if (m.getItemId() != null) {
                                bookedQtyMap.put(m.getItemId().longValue(), m.getQuantity() != null ? m.getQuantity() : 0);
                            }
                        }
                    }

                    for (int i = 0; i < itemIds.size(); i++) {
                        Long itemId = itemIds.get(i);
                        int sent = (sentQuantities != null && i < sentQuantities.size() && sentQuantities.get(i) != null) ? sentQuantities.get(i) : 0;
                        int actualDeliv = (actualDeliveredQuantities != null && i < actualDeliveredQuantities.size() && actualDeliveredQuantities.get(i) != null) ? actualDeliveredQuantities.get(i) : sent;
                        int damaged = (damagedQuantities != null && i < damagedQuantities.size() && damagedQuantities.get(i) != null) ? damagedQuantities.get(i) : 0;
                        int returned = (returnedQuantities != null && i < returnedQuantities.size() && returnedQuantities.get(i) != null) ? returnedQuantities.get(i) : 0;

                        SalesOrderDetail d = new SalesOrderDetail();
                        d.setOrderId(savedOrder.getOrderId());
                        d.setItemId(itemId);
                        d.setDeliveredQuantity(sent);
                        d.setActualDeliveredQuantity(actualDeliv);
                        d.setDamagedQuantity(damaged);
                        d.setReturnedQuantity(returned);
                        if (returned > 0) {
                            d.setReturnedDate(getNowWithoutMillis());
                        }

                        int bQty = bookedQtyMap.getOrDefault(itemId, sent);
                        d.setQuantity(bQty);
                        totalNewBooked += bQty;
                        totalNewAccounted += (actualDeliv + damaged + returned);

                        if (sent > 0 && (actualDeliv + damaged + returned) < sent) {
                            isNewOrderAllDelivered = false;
                        }

                        Item item = itemService.getItemById(itemId);
                        double rate = (item != null && item.getRate() != null) ? item.getRate() : 0.0;
                        d.setRate(rate);
                        double cost = rate * sent;
                        d.setTotalCost(cost);
                        detailRepository.save(d);

                        if (sent > 0) {
                            stockLedgerService.recordStockOut(itemId, (double) sent, savedOrder.getOrderId(), "DELIVERED_SALES_UPDATE");
                        }
                        if (returned > 0) {
                            stockLedgerService.recordStockIn(itemId, (double) returned, savedOrder.getOrderId(), "DELIVERED_SALES_RETURN");
                        }

                        newTotalQty += actualDeliv;
                        newTotalCost += cost;
                    }

                    if (totalNewAccounted == 0) {
                        savedOrder.setStatus(1); // Pending
                    } else if (isNewOrderAllDelivered && newTotalQty > 0 && totalNewAccounted >= newTotalQty) {
                        savedOrder.setStatus(3); // Delivered
                    } else {
                        savedOrder.setStatus(2); // Partially Delivered
                    }

                    savedOrder.setTotalQuantity(newTotalQty);
                    savedOrder.setTotalCost(newTotalCost);
                    salesOrderRepository.save(savedOrder);
                }
            }
            redirectAttributes.addFlashAttribute("successMessage", "Delivered Quantity updated successfully! Current stock and stock ledger updated.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating Delivered Quantity: " + e.getMessage());
        }
        return "redirect:/delivered-sales";
    }
}
