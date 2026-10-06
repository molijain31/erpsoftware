package com.erp.erpsoftware.controller;

import com.erp.erpsoftware.entity.BookingRequest;
import com.erp.erpsoftware.entity.BookingRequestItemMapping;
import com.erp.erpsoftware.entity.Item;
import com.erp.erpsoftware.entity.Supplier;
import com.erp.erpsoftware.entity.SupplierItemMapping;
import com.erp.erpsoftware.service.BookingRequestService;
import com.erp.erpsoftware.service.ItemService;
import com.erp.erpsoftware.service.SupplierService;
import com.erp.erpsoftware.entity.PurchaseOrderMapping;
import com.erp.erpsoftware.repository.SupplierItemMappingRepository;
import com.erp.erpsoftware.repository.PurchaseOrderMappingRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import com.erp.erpsoftware.service.SalesOrderService;
import com.erp.erpsoftware.entity.SalesOrder;

@Controller
@RequestMapping("/bookingrequest")
public class BookingRequestController {

    @Autowired
    private BookingRequestService service;

    @Autowired
    private SupplierService supplierService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private SupplierItemMappingRepository supplierItemMappingRepository;

    @Autowired
    private PurchaseOrderMappingRepository purchaseOrderMappingRepository;

    @Autowired
    private SalesOrderService salesOrderService;

    @GetMapping("/new")
    public String newRequest(Model model) {
        model.addAttribute("bookingRequest", new BookingRequest());
        model.addAttribute("suppliers", supplierService.getAllSuppliers());
        return "booking-request-form";
    }

    private String getFormParam(HttpServletRequest request, String prefix, Integer itemId, Long supplierId) {
        if (request == null || itemId == null) return null;
        if (supplierId != null) {
            String val = request.getParameter(prefix + "_" + itemId + "_" + supplierId);
            if (val != null && !val.trim().isEmpty() && !"null".equalsIgnoreCase(val.trim())) return val.trim();
        }
        String val = request.getParameter(prefix + "_" + itemId + "_");
        if (val != null && !val.trim().isEmpty() && !"null".equalsIgnoreCase(val.trim())) return val.trim();

        val = request.getParameter(prefix + "_" + itemId);
        if (val != null && !val.trim().isEmpty() && !"null".equalsIgnoreCase(val.trim())) return val.trim();

        Enumeration<String> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String pName = paramNames.nextElement();
            if (pName.startsWith(prefix + "_" + itemId)) {
                val = request.getParameter(pName);
                if (val != null && !val.trim().isEmpty() && !"null".equalsIgnoreCase(val.trim())) return val.trim();
            }
        }
        return null;
    }

    @PostMapping("/save")
    public String save(
            @RequestParam(value = "selectedItems", required = false) List<Integer> selectedItems,
            @RequestParam(value = "selectedSuppliers", required = false) List<Long> selectedSuppliers,
            @RequestParam(value = "quantities", required = false) List<Integer> quantities,
            @RequestParam(value = "rates", required = false) List<Double> rates,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        try {
            String[] rawItems = request.getParameterValues("selectedItems");
            String[] rawSuppliers = request.getParameterValues("selectedSuppliers");
            String[] rawQuantities = request.getParameterValues("quantities");
            String[] rawRates = request.getParameterValues("rates");

            List<Integer> finalItems = new ArrayList<>();
            List<Long> finalSuppliers = new ArrayList<>();
            List<Integer> finalQuantities = new ArrayList<>();
            List<Double> finalRates = new ArrayList<>();

            if (rawItems != null && rawItems.length > 0) {
                for (int i = 0; i < rawItems.length; i++) {
                    if (rawItems[i] == null || rawItems[i].trim().isEmpty() || "null".equalsIgnoreCase(rawItems[i].trim())) continue;
                    try {
                        Integer itemId = Integer.parseInt(rawItems[i].trim());
                        Long supplierId = (rawSuppliers != null && i < rawSuppliers.length && rawSuppliers[i] != null && !rawSuppliers[i].trim().isEmpty() && !"null".equalsIgnoreCase(rawSuppliers[i].trim()))
                                ? Long.parseLong(rawSuppliers[i].trim()) : null;
                        int qty = (rawQuantities != null && i < rawQuantities.length && rawQuantities[i] != null && !rawQuantities[i].trim().isEmpty())
                                ? Integer.parseInt(rawQuantities[i].trim()) : 1;
                        double rate = (rawRates != null && i < rawRates.length && rawRates[i] != null && !rawRates[i].trim().isEmpty())
                                ? Double.parseDouble(rawRates[i].trim()) : 0.0;

                        finalItems.add(itemId);
                        finalSuppliers.add(supplierId);
                        finalQuantities.add(qty);
                        finalRates.add(rate);
                    } catch (Exception ignored) {}
                }
            }

            if (finalItems.isEmpty() && selectedItems != null && !selectedItems.isEmpty()) {
                for (int i = 0; i < selectedItems.size(); i++) {
                    Integer itemId = selectedItems.get(i);
                    Long supplierId = (selectedSuppliers != null && i < selectedSuppliers.size()) ? selectedSuppliers.get(i) : null;
                    int qty = (quantities != null && i < quantities.size() && quantities.get(i) != null) ? quantities.get(i) : 1;
                    double rate = (rates != null && i < rates.size() && rates.get(i) != null) ? rates.get(i) : 0.0;

                    finalItems.add(itemId);
                    finalSuppliers.add(supplierId);
                    finalQuantities.add(qty);
                    finalRates.add(rate);
                }
            }

            if (finalItems.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Please select at least one item.");
                return "redirect:/bookingrequest";
            }

            String bookingNo = "BR-"
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-HHmm"))
                    + "-" + (new Random().nextInt(900) + 100);

            Date now = new Date();

            int totalQty = 0;
            double grandTotal = 0;

            List<Integer> masterItemIds = new ArrayList<>();
            List<Double> processedRates = new ArrayList<>();

            // 1. First pass: Parse and calculate overall totals
            for (int i = 0; i < finalItems.size(); i++) {
                Integer itemId = finalItems.get(i);
                int qty = finalQuantities.get(i);
                double rate = finalRates.get(i);

                Item itemFromMaster = itemService.getItemById(itemId.longValue());
                Integer itemMasterId = (itemFromMaster != null && itemFromMaster.getItemId() != null)
                        ? itemFromMaster.getItemId().intValue()
                        : itemId;

                if (rate <= 0 && itemFromMaster != null && itemFromMaster.getRate() != null) {
                    rate = itemFromMaster.getRate();
                }

                masterItemIds.add(itemMasterId);
                processedRates.add(rate);

                totalQty += qty;
                grandTotal += qty * rate;
            }

            Long supplierId = finalSuppliers.get(0);

            BookingRequest booking = new BookingRequest();
            booking.setBookingNo(bookingNo);
            booking.setSupplierId(supplierId != null ? String.valueOf(supplierId) : "");
            booking.setTotalQuantity(totalQty);
            booking.setTotalCost(grandTotal);
            booking.setStatus("Pending");
            booking.setEntryDate(now);
            booking.setModifiedDate(now);

            service.save(booking);

            // 2. Second pass: Persist each item record
            for (int i = 0; i < finalItems.size(); i++) {
                Integer itemMasterId = masterItemIds.get(i);
                int qty = finalQuantities.get(i);
                double rate = processedRates.get(i);

                // Save Item Mapping
                BookingRequestItemMapping mapping = new BookingRequestItemMapping();
                mapping.setBookingNo(bookingNo);
                mapping.setItemId(itemMasterId);
                mapping.setQuantity(qty);
                mapping.setTotalCost(qty * rate);
                mapping.setStatus("Pending");
                mapping.setEntryDate(now);
                mapping.setModifiedDate(now);

                service.saveItemMapping(mapping);
            }

            redirectAttributes.addFlashAttribute("successMessage", "Booking Request Saved Successfully.");

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/bookingrequest";
    }

    @GetMapping("/supplier/{id}")
    @ResponseBody
    public Map<String, Object> getSupplier(@PathVariable Long id) {
        Supplier supplier = supplierService.getSupplierById(id);
        Map<String, Object> map = new HashMap<>();
        if (supplier != null) {
            map.put("supplierId", supplier.getSupplierId());
            map.put("supplierName", supplier.getContactPerson());
            map.put("supplierCode", supplier.getSupplierCode());
            map.put("contactPerson", supplier.getContactPerson());
            map.put("mobile", supplier.getMobile());
            map.put("email", supplier.getEmail());
            map.put("address", supplier.getAddress());
            map.put("city", supplier.getCity());
            map.put("state", supplier.getState());
            map.put("pin code", supplier.getPincode());
            map.put("supplierType", supplier.getSupplierType());
            map.put("panNo", supplier.getPanNo());
            map.put("gstNo", supplier.getGstNo());
            map.put("bankName", supplier.getBankName());
            map.put("accountNumber", supplier.getAccountNumber());
            map.put("ifscCode", supplier.getIfscCode());
            map.put("branch", supplier.getBranch());
        }
        return map;
    }

    @GetMapping("/supplier/{id}/mappings")
    @ResponseBody
    public List<Map<String, Object>> getSupplierMappings(@PathVariable Long id) {
        List<SupplierItemMapping> mappings = supplierItemMappingRepository.findBySupplierSupplierId(id);
        List<Map<String, Object>> result = new ArrayList<>();
        Set<Long> seenItemIds = new HashSet<>();
        if (mappings != null) {
            for (SupplierItemMapping mapping : mappings) {
                if (mapping != null && mapping.getItem() != null && mapping.getItem().getItemId() != null) {
                    Long itemId = mapping.getItem().getItemId();
                    if (!seenItemIds.contains(itemId)) {
                        seenItemIds.add(itemId);
                        Map<String, Object> map = new HashMap<>();
                        map.put("mappingId", mapping.getMappingId());
                        map.put("rate", mapping.getRate());
                        map.put("itemId", mapping.getItem().getItemId());
                        map.put("itemCode", mapping.getItem().getItemCode());
                        map.put("itemName", mapping.getItem().getItemName());
                        if (mapping.getType() != null) {
                            map.put("typeId", mapping.getType().getTypeId());
                            map.put("typeName", mapping.getType().getTypeName());
                        }
                        result.add(map);
                    }
                }
            }
        }
        return result;
    }

    @GetMapping("/items/type/{supplierType}")
    @ResponseBody
    public List<Item> getItemsBySupplierType(@PathVariable String supplierType) {
        List<Item> allItems = itemService.getAllItems();
        List<Item> filtered = allItems.stream()
                .filter(item -> item.getType() != null && item.getType().getTypeName().equalsIgnoreCase(supplierType))
                .toList();
        List<Item> targetList = filtered.isEmpty() ? allItems : filtered;
        List<Item> result = new ArrayList<>();
        Set<Long> seenItemIds = new HashSet<>();
        for (Item item : targetList) {
            if (item != null && item.getItemId() != null && !seenItemIds.contains(item.getItemId())) {
                seenItemIds.add(item.getItemId());
                result.add(item);
            }
        }
        return result;
    }

    @GetMapping("/item/{itemId}")
    @ResponseBody
    public Map<String, Object> getItemById(@PathVariable Long itemId) {
        Item item = itemService.getItemById(itemId);
        Map<String, Object> map = new HashMap<>();
        if (item != null) {
            map.put("itemId", item.getItemId());
            map.put("itemCode", item.getItemCode());
            map.put("itemName", item.getItemName());
            map.put("rate", item.getRate());
            if (item.getType() != null) {
                map.put("typeId", item.getType().getTypeId());
                map.put("typeName", item.getType().getTypeName());
            }
        }
        return map;
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
        // book_id removed; no per-item supplier fallback needed here
        List<Supplier> allSuppliers = supplierService.getAllSuppliers();
        if (allSuppliers != null && !allSuppliers.isEmpty()) {
            return allSuppliers.get(0).getSupplierName();
        }
        return "—";
    }

    @GetMapping(value = {"", "/list"})
    public String listBookingRequests(Model model) {
        List<BookingRequest> requests = service.getAllBookingRequests();

        Map<String, List<BookingRequest>> groupedRequests = new LinkedHashMap<>();
        for (BookingRequest req : requests) {
            String bNo = req.getBookingNo();
            if (bNo != null && !bNo.isEmpty()) {
                groupedRequests.computeIfAbsent(bNo, k -> new ArrayList<>()).add(req);
            }
        }
        model.addAttribute("groupedRequests", groupedRequests);

        Map<String, String> supplierMap = new HashMap<>();
        try {
            List<Supplier> allSuppliers = supplierService.getAllSuppliers();
            if (allSuppliers != null) {
                for (Supplier s : allSuppliers) {
                    if (s != null && s.getSupplierId() != null) {
                        String name = s.getSupplierName();
                        if (name == null || name.trim().isEmpty()) {
                            name = s.getContactPerson();
                        }
                        if (name != null && !name.trim().isEmpty()) {
                            supplierMap.put(String.valueOf(s.getSupplierId()), name.trim());
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        Map<String, String> groupedSuppliers = new HashMap<>();
        Map<String, Integer> groupedQuantities = new HashMap<>();
        Map<String, List<String>> groupedItemNames = new HashMap<>();
        for (Map.Entry<String, List<BookingRequest>> entry : groupedRequests.entrySet()) {
            Set<String> suppliers = new LinkedHashSet<>();
            for (BookingRequest req : entry.getValue()) {
                String resolvedName = resolveSupplierName(req);
                if (resolvedName != null && !resolvedName.equals("—") && !resolvedName.trim().isEmpty()) {
                    suppliers.add(resolvedName);
                } else if (req.getSupplierId() != null) {
                    String suppName = supplierMap.get(req.getSupplierId());
                    if (suppName != null && !suppName.isEmpty()) {
                        suppliers.add(suppName);
                    }
                }
            }
            int totalQty = 0;
            if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                for (BookingRequest req : entry.getValue()) {
                    if (req.getTotalQuantity() != null && req.getTotalQuantity() > 0) {
                        totalQty = Math.max(totalQty, req.getTotalQuantity());
                    }
                }
            }
            
            List<BookingRequestItemMapping> mappings = service.getItemMappingsByBookingNo(entry.getKey());
            List<String> itemNames = new ArrayList<>();
            if (mappings != null && !mappings.isEmpty()) {
                for (BookingRequestItemMapping mapping : mappings) {
                    if (mapping.getItemId() != null) {
                        Item item = itemService.getItemById(mapping.getItemId().longValue());
                        if (item != null && item.getItemName() != null) {
                            itemNames.add(item.getItemName());
                        }
                    }
                }
            }
            groupedItemNames.put(entry.getKey(), itemNames);

            if (totalQty == 0 && mappings != null) {
                for (BookingRequestItemMapping mapping : mappings) {
                    if (mapping.getQuantity() != null && mapping.getQuantity() > 0) {
                        totalQty += mapping.getQuantity();
                    }
                }
            }
            // book_id/quantity removed from BookingRequest header; use item mapping for totals
            groupedSuppliers.put(entry.getKey(), String.join(", ", suppliers));
            groupedQuantities.put(entry.getKey(), totalQty);
        }

        Map<String, String> groupedStatuses = new HashMap<>();
        for (Map.Entry<String, List<BookingRequest>> entry : groupedRequests.entrySet()) {
            String status = "Pending";
            if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                for (BookingRequest req : entry.getValue()) {
                    if (req.getStatus() != null && !req.getStatus().trim().isEmpty()) {
                        status = req.getStatus().trim();
                        break;
                    }
                }
            }
            groupedStatuses.put(entry.getKey(), status);
        }

        Map<Integer, String> itemNameMap = new HashMap<>();
        try {
            List<Item> allItems = itemService.getAllItems();
            if (allItems != null) {
                for (Item item : allItems) {
                    if (item != null && item.getItemId() != null) {
                        itemNameMap.put(item.getItemId().intValue(), item.getItemName() != null ? item.getItemName() : "Item #" + item.getItemId());
                    }
                }
            }
        } catch (Exception ignored) {}

        Map<String, List<String>> groupedItemsList = new HashMap<>();
        Map<String, Double> groupedTotalCosts = new HashMap<>();

        for (Map.Entry<String, List<BookingRequest>> entry : groupedRequests.entrySet()) {
            String bNo = entry.getKey();
            List<BookingRequestItemMapping> mappings = service.getItemMappingsByBookingNo(bNo);
            List<String> itemList = new ArrayList<>();
            List<String> itemNames = new ArrayList<>();
            double calculatedTotal = 0.0;

            if (mappings != null && !mappings.isEmpty()) {
                for (BookingRequestItemMapping mapping : mappings) {
                    if (mapping.getItemId() != null) {
                        String name = itemNameMap.get(mapping.getItemId());
                        if (name == null || name.trim().isEmpty()) {
                            name = "Item #" + mapping.getItemId();
                        }
                        itemNames.add(name);

                        int qty = mapping.getQuantity() != null ? mapping.getQuantity() : 0;
                        double rate = 0.0;
                        if (mapping.getItem() != null && mapping.getItem().getRate() != null) {
                            rate = mapping.getItem().getRate();
                        } else {
                            Item item = itemService.getItemById(mapping.getItemId().longValue());
                            if (item != null && item.getRate() != null) {
                                rate = item.getRate();
                            }
                        }
                        double itemTotal = (mapping.getTotalCost() != null && mapping.getTotalCost() > 0) ? mapping.getTotalCost() : (qty * rate);
                        calculatedTotal += itemTotal;

                        String itemPill = name + " (" + qty + " Qty - ₹" + String.format(java.util.Locale.US, "%.2f", itemTotal) + ")";
                        itemList.add(itemPill);
                    }
                }
            }

            if (calculatedTotal <= 0.0 && entry.getValue() != null && !entry.getValue().isEmpty()) {
                for (BookingRequest req : entry.getValue()) {
                    if (req.getTotalCost() != null && req.getTotalCost() > 0) {
                        calculatedTotal = Math.max(calculatedTotal, req.getTotalCost());
                    }
                }
            }

            groupedItemsList.put(bNo, itemList);
            groupedItemNames.put(bNo, itemNames);
            groupedTotalCosts.put(bNo, calculatedTotal);
        }

        Map<String, Boolean> hasChallanOrdersMap = new HashMap<>();
        for (String bNo : groupedRequests.keySet()) {
            List<SalesOrder> sos = salesOrderService.getAllSalesOrdersByBookingNo(bNo);
            hasChallanOrdersMap.put(bNo, (sos != null && !sos.isEmpty()));
        }

        model.addAttribute("groupedSuppliers", groupedSuppliers);
        model.addAttribute("groupedQuantities", groupedQuantities);
        model.addAttribute("groupedStatuses", groupedStatuses);
        model.addAttribute("groupedItemsList", groupedItemsList);
        model.addAttribute("groupedItemNames", groupedItemNames);
        model.addAttribute("groupedTotalCosts", groupedTotalCosts);
        model.addAttribute("hasChallanOrdersMap", hasChallanOrdersMap);

        // book_id removed from booking_request; itemMap is built from item mappings in viewRequest

        return "booking-request-list";
    }

    @GetMapping("/view/{bookingNo}")
    public String viewRequest(@PathVariable String bookingNo, Model model, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes){
        try {
            List<BookingRequest> requests = service.getRequestsByBookingNo(bookingNo);
            if (requests == null || requests.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Booking Request not found with booking number: " + bookingNo);
                return "redirect:/bookingrequest";
            }

            model.addAttribute("bookingNo", bookingNo);

        Map<String, String> supplierMap = supplierService.getAllSuppliers().stream()
                .filter(s -> s.getSupplierId() != null && s.getContactPerson() != null)
                .collect(Collectors.toMap(
                        s -> String.valueOf(s.getSupplierId()),
                        Supplier::getContactPerson,
                        (existing, replacement) -> existing
                ));

        Set<String> suppliers = new LinkedHashSet<>();
        double grandTotal = 0.0;
        Map<Integer, Item> itemMap = new HashMap<>();
        List<BookingRequestItemMapping> allItemMappings = new ArrayList<>();

        for (BookingRequest req : requests) {
            if (req.getSupplierId() != null) {
                String suppName = supplierMap.get(req.getSupplierId());
                if (suppName != null && !suppName.isEmpty()) {
                    suppliers.add(suppName);
                }
            }
        }

        List<BookingRequestItemMapping> mappings = service.getItemMappingsByBookingNo(bookingNo);
        if (mappings != null && !mappings.isEmpty()) {
            for (BookingRequestItemMapping mapping : mappings) {
                allItemMappings.add(mapping);
                if (mapping.getItemId() != null) {
                    Item item = itemService.getItemById(mapping.getItemId().longValue());
                    if (item != null) {
                        itemMap.put(mapping.getItemId(), item);
                    }
                }
                if (mapping.getTotalCost() != null) {
                    grandTotal += mapping.getTotalCost();
                } else if (mapping.getQuantity() != null && itemMap.get(mapping.getItemId()) != null && itemMap.get(mapping.getItemId()).getRate() != null) {
                    grandTotal += itemMap.get(mapping.getItemId()).getRate() * mapping.getQuantity();
                }
            }
        }
        // book_id removed; all item data comes from item mappings above

        model.addAttribute("supplierNames", String.join(", ", suppliers));
        model.addAttribute("requests", requests);
        model.addAttribute("allItemMappings", allItemMappings);
        model.addAttribute("itemMap", itemMap);
        model.addAttribute("grandTotal", grandTotal);

            return "booking-request-view";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error loading booking details: " + e.getMessage());
            return "redirect:/bookingrequest";
        }
    }

    @GetMapping("/details/{bookingNo}")
    @ResponseBody
    public Map<String, Object> getBookingDetailsJson(@PathVariable String bookingNo) {
        Map<String, Object> response = new HashMap<>();
        List<BookingRequest> requests = service.getRequestsByBookingNo(bookingNo);
        List<BookingRequestItemMapping> itemMappings = service.getItemMappingsByBookingNo(bookingNo);

        if ((requests == null || requests.isEmpty()) && (itemMappings == null || itemMappings.isEmpty())) {
            response.put("error", "Booking Request not found");
            return response;
        }

        response.put("bookingNo", bookingNo);
        String orderId = (requests != null && !requests.isEmpty() && requests.get(0).getBookingNo() != null) 
                ? requests.get(0).getBookingNo() 
                : bookingNo;
        String status = (requests != null && !requests.isEmpty() && requests.get(0).getStatus() != null) 
                ? requests.get(0).getStatus() 
                : "Pending";
        response.put("orderId", orderId);
        response.put("status", status);

        Set<String> suppliers = new LinkedHashSet<>();
        if (requests != null) {
            for (BookingRequest req : requests) {
                String sName = resolveSupplierName(req);
                if (sName != null && !sName.trim().isEmpty() && !"—".equals(sName)) {
                    suppliers.add(sName);
                }
            }
        }

        double grandTotal = 0.0;
        List<Map<String, Object>> itemsList = new ArrayList<>();
        Set<Integer> processedItemIds = new HashSet<>();

        // 1. Process items from BookingRequestItemMapping table
        if (itemMappings != null && !itemMappings.isEmpty()) {
            for (BookingRequestItemMapping mapping : itemMappings) {
                if (mapping.getItemId() == null) continue;
                processedItemIds.add(mapping.getItemId());

                Map<String, Object> itemData = new HashMap<>();
                int qty = (mapping.getQuantity() != null && mapping.getQuantity() > 0) ? mapping.getQuantity() : 1;

                double cost = mapping.getTotalCost() != null ? mapping.getTotalCost() : 0.0;

                itemData.put("quantity", qty);

                Item item = itemService.getItemById(mapping.getItemId().longValue());
                double rate;
                if (cost > 0 && qty > 0) {
                    rate = cost / qty;
                } else if (item != null && item.getRate() != null) {
                    rate = item.getRate();
                } else {
                    rate = 0.0;
                }

                double lineTotal = (cost > 0) ? cost : (rate * qty);

                if (item != null) {
                    itemData.put("itemCode", item.getItemCode() != null ? item.getItemCode() : "N/A");
                    itemData.put("itemName", item.getItemName() != null ? item.getItemName() : "N/A");
                } else {
                    itemData.put("itemCode", "N/A");
                    itemData.put("itemName", "Item #" + mapping.getItemId());
                }

                itemData.put("rate", rate);
                itemData.put("totalCost", lineTotal);
                grandTotal += lineTotal;
                itemsList.add(itemData);
            }
        }

        // book_id removed from BookingRequest; all items come from item_mapping above

        response.put("supplierNames", suppliers.isEmpty() ? "—" : String.join(", ", suppliers));
        response.put("grandTotal", grandTotal);
        response.put("items", itemsList);

        return response;
    }
}
