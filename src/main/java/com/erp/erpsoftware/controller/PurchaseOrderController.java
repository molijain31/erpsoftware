package com.erp.erpsoftware.controller;

import com.erp.erpsoftware.entity.PurchaseOrder;
import com.erp.erpsoftware.entity.PurchaseOrderMapping;
import com.erp.erpsoftware.entity.Item;
import com.erp.erpsoftware.entity.Vendor;
import com.erp.erpsoftware.entity.VendorItemMapping;
import com.erp.erpsoftware.service.PurchaseOrderService;
import com.erp.erpsoftware.service.VendorService;
import com.erp.erpsoftware.service.ItemService;
import com.erp.erpsoftware.repository.VendorItemMappingRepository;
import com.erp.erpsoftware.entity.BookingRequestItemMapping;
import com.erp.erpsoftware.entity.CurrentStock;
import com.erp.erpsoftware.repository.BookingRequestItemMappingRepository;
import com.erp.erpsoftware.service.CurrentStockService;
import com.erp.erpsoftware.repository.PurchaseOrderMappingRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/purchaseorders")
public class PurchaseOrderController {

    @Autowired
    private PurchaseOrderService service;

    @Autowired
    private VendorService vendorService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private VendorItemMappingRepository vendorItemMappingRepository;

    @Autowired
    private BookingRequestItemMappingRepository bookingRequestItemMappingRepository;

    @Autowired
    private com.erp.erpsoftware.service.BookingRequestService bookingRequestService;

    @Autowired
    private CurrentStockService currentStockService;

    @Autowired
    private PurchaseOrderMappingRepository purchaseOrderMappingRepository;

    @GetMapping
    public String list(Model model) {
        List<PurchaseOrder> purchaseOrders = service.getAllPurchaseOrders();
        model.addAttribute("purchaseOrders", purchaseOrders);

        Map<Long, List<String>> poItemNamesMap = new HashMap<>();
        for (PurchaseOrder po : purchaseOrders) {
            List<String> names = new ArrayList<>();
            if (po.getDetails() != null) {
                for (PurchaseOrderMapping detail : po.getDetails()) {
                    if (detail.getItem() != null && detail.getItem().getItemName() != null) {
                        names.add(detail.getItem().getItemName());
                    }
                }
            }
            poItemNamesMap.put(po.getOrderId(), names);
        }
        model.addAttribute("poItemNamesMap", poItemNamesMap);

        return "purchase-order-list";
    }

    @GetMapping("/new")
    public String newOrder(@RequestParam(value = "bookingNo", required = false) String bookingNo,
                           Model model) {
        PurchaseOrder po = new PurchaseOrder();
        model.addAttribute("purchaseOrder", po);
        List<Vendor> vendors = vendorService.getAllVendors();
        model.addAttribute("vendors", vendors);

        Long matchedVendorId = null;

        if (bookingNo != null && !bookingNo.trim().isEmpty()) {
            String cleanBNo = bookingNo.trim();
            model.addAttribute("bookingNo", cleanBNo);

            List<com.erp.erpsoftware.entity.BookingRequestItemMapping> itemMappings = bookingRequestService.getItemMappingsByBookingNo(cleanBNo);
            List<Map<String, Object>> preSelectedItems = new ArrayList<>();

            if (itemMappings != null) {
                for (com.erp.erpsoftware.entity.BookingRequestItemMapping mapping : itemMappings) {
                    if (mapping.getItemId() == null) continue;
                    Item item = itemService.getItemById(mapping.getItemId().longValue());
                    if (item == null) continue;

                    double rate = (item.getRate() != null && item.getRate() > 0) ? item.getRate() : 0.0;
                    if (mapping.getTotalCost() != null && mapping.getQuantity() != null && mapping.getQuantity() > 0) {
                        rate = mapping.getTotalCost() / mapping.getQuantity();
                    }

                    Map<String, Object> itemMap = new HashMap<>();
                    itemMap.put("itemId", item.getItemId());
                    itemMap.put("itemCode", item.getItemCode() != null ? item.getItemCode() : "N/A");
                    itemMap.put("itemName", item.getItemName() != null ? item.getItemName() : "N/A");
                    itemMap.put("quantity", mapping.getQuantity() != null ? mapping.getQuantity() : 1);
                    itemMap.put("rate", rate);
                    itemMap.put("totalCost", mapping.getTotalCost() != null ? mapping.getTotalCost() : (rate * (mapping.getQuantity() != null ? mapping.getQuantity() : 1)));
                    itemMap.put("typeName", item.getType() != null ? item.getType().getTypeName() : "N/A");
                    preSelectedItems.add(itemMap);

                    if (matchedVendorId == null) {
                        List<VendorItemMapping> vimList = vendorItemMappingRepository.findByItemId(item.getItemId());
                        if (vimList != null && !vimList.isEmpty() && vimList.get(0).getVendorId() != null) {
                            matchedVendorId = vimList.get(0).getVendorId();
                        }
                    }
                }
            }

            if (matchedVendorId == null && vendors != null && !vendors.isEmpty()) {
                List<com.erp.erpsoftware.entity.BookingRequest> requests = bookingRequestService.getRequestsByBookingNo(cleanBNo);
                if (requests != null && !requests.isEmpty()) {
                    String suppIdStr = requests.get(0).getSupplierId();
                    String suppNameStr = requests.get(0).getSupplierName();
                    if (suppIdStr != null && !suppIdStr.trim().isEmpty()) {
                        try {
                            Long sId = Long.parseLong(suppIdStr.trim());
                            for (Vendor v : vendors) {
                                if (sId.equals(v.getVendorId())) {
                                    matchedVendorId = v.getVendorId();
                                    break;
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                    if (matchedVendorId == null && suppNameStr != null && !suppNameStr.trim().isEmpty()) {
                        for (Vendor v : vendors) {
                            if (v.getVendorName() != null && v.getVendorName().toLowerCase().contains(suppNameStr.trim().toLowerCase())) {
                                matchedVendorId = v.getVendorId();
                                break;
                            }
                        }
                    }
                }
            }
            model.addAttribute("preSelectedItems", preSelectedItems);
        }

        // Fallback: Default to first vendor if still null
        if (matchedVendorId == null && vendors != null && !vendors.isEmpty()) {
            matchedVendorId = vendors.get(0).getVendorId();
        }

        if (matchedVendorId != null) {
            po.setVendorId(matchedVendorId);
            model.addAttribute("preSelectedVendorId", matchedVendorId);
        }

        return "purchase-order-form";
    }

    @PostMapping("/save")
    public String save(@RequestParam(value = "orderId", required = false) Long orderId,
                       @RequestParam(value = "vendorId", required = false) Long vendorId,
                       @RequestParam(value = "selectedItems", required = false) List<Long> selectedItems,
                       HttpServletRequest request,
                       RedirectAttributes redirectAttributes) {
        try {
            if (vendorId == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Please select a Vendor.");
                return "redirect:/purchaseorders/new";
            }
            if (selectedItems == null || selectedItems.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Please add at least one item to the Purchase Order.");
                return "redirect:/purchaseorders/new";
            }

            PurchaseOrder order;
            Map<Long, String> existingStatuses = new HashMap<>();
            Map<Long, Integer> alreadyReceivedMap = new HashMap<>();
            if (orderId != null) {
                order = service.getPurchaseOrderById(orderId);
                if (order.getDetails() != null) {
                    for (PurchaseOrderMapping d : order.getDetails()) {
                        existingStatuses.put(d.getItemId(), d.getReceiveOrderStatus());
                        int alreadyRec = d.getReceivedQuantity() != null ? d.getReceivedQuantity() : 0;
                        alreadyReceivedMap.put(d.getItemId(), alreadyRec);
                    }
                }
            } else {
                order = new PurchaseOrder();
            }
            order.setVendorId(vendorId);

            Vendor headerVendor = null;
            if (vendorId != null) {
                try {
                    headerVendor = vendorService.getVendorById(vendorId);
                } catch (Exception ignored) {}
            }

            List<PurchaseOrderMapping> details = new ArrayList<>();
            for (Long itemId : selectedItems) {
                String qtyStr = request.getParameter("quantity_" + itemId);
                Integer qty = (qtyStr != null && !qtyStr.trim().isEmpty()) ? Integer.parseInt(qtyStr.trim()) : 1;

                String rateStr = request.getParameter("rate_" + itemId);
                Double rate = (rateStr != null && !rateStr.trim().isEmpty()) ? Double.parseDouble(rateStr.trim()) : 0.0;

                // Read newly received quantity from form (edit mode)
                String newReceivedQtyStr = request.getParameter("receivedQty_" + itemId);
                Integer newReceivedQty = (newReceivedQtyStr != null && !newReceivedQtyStr.trim().isEmpty())
                        ? Integer.parseInt(newReceivedQtyStr.trim()) : 0;

                int alreadyReceived = alreadyReceivedMap.getOrDefault(itemId, 0);
                int totalReceivedQty = alreadyReceived + newReceivedQty;

                // Validate totalReceivedQty range [0, qty]
                if (totalReceivedQty < 0) totalReceivedQty = 0;
                if (totalReceivedQty > qty) {
                    totalReceivedQty = qty;
                }

                // Derive status: 1=Pending, 2=Partially Received, 3=Fully Received
                int statusInt;
                String receiveOrderStatus;
                if (totalReceivedQty == 0) {
                    statusInt = 1;
                    receiveOrderStatus = "create";
                } else if (totalReceivedQty < qty) {
                    statusInt = 2;
                    receiveOrderStatus = "Partially Received";
                } else {
                    statusInt = 3;
                    receiveOrderStatus = "Fully Received";
                }

                // Determine vendor for this item (from item mapping or header vendor)
                Long itemVendorId = null;
                String itemVendorName = null;
                List<VendorItemMapping> vimList = vendorItemMappingRepository.findByItemId(itemId);
                if (vimList != null && !vimList.isEmpty() && vimList.get(0).getVendorId() != null) {
                    itemVendorId = vimList.get(0).getVendorId();
                    try {
                        Vendor iv = vendorService.getVendorById(itemVendorId);
                        if (iv != null) {
                            itemVendorName = iv.getVendorName();
                        }
                    } catch (Exception ignored) {}
                }
                if (itemVendorId == null) {
                    itemVendorId = vendorId;
                    if (headerVendor != null) {
                        itemVendorName = headerVendor.getVendorName();
                    }
                }

                PurchaseOrderMapping detail = new PurchaseOrderMapping();
                detail.setItemId(itemId);
                detail.setVendorId(itemVendorId);
                detail.setVendorName(itemVendorName);
                detail.setQuantity(qty);
                detail.setRate(rate);
                detail.setRateCost(rate);
                detail.setTotalCost(rate * qty);
                detail.setIsValid(1);
                detail.setReceivedQuantity(totalReceivedQty);
                detail.setStatus(statusInt);
                detail.setReceiveOrderStatus(receiveOrderStatus);
                details.add(detail);
            }
            order.setDetails(details);
            service.save(order);

            redirectAttributes.addFlashAttribute("successMessage", "Purchase Order saved successfully!");
            return "redirect:/purchaseorders";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error saving Purchase Order: " + e.getMessage());
            return "redirect:/purchaseorders/new";
        }
    }

    @GetMapping("/view/{id}")
    public String viewOrder(@PathVariable Long id, Model model) {
        PurchaseOrder po = service.getPurchaseOrderById(id);
        if (po != null && po.getVendorId() != null && (po.getVendorName() == null || po.getVendorName().isEmpty())) {
            try {
                Vendor v = vendorService.getVendorById(po.getVendorId());
                if (v != null) {
                    po.setVendorName(v.getVendorName());
                    if (po.getTransactionAccount() == null || po.getTransactionAccount().isEmpty()) {
                        po.setTransactionAccount(v.getTransactionAccount());
                    }
                }
            } catch (Exception ignored) {}
        }
        model.addAttribute("po", po);
        return "purchase-order-view";
    }

    @GetMapping("/edit/{id}")
    public String editOrder(@PathVariable Long id, Model model) {
        model.addAttribute("purchaseOrder", service.getPurchaseOrderById(id));
        model.addAttribute("vendors", vendorService.getAllVendors());
        return "purchase-order-form";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        service.deletePurchaseOrder(id);
        return "redirect:/purchaseorders";
    }

    @GetMapping("/receive/{id}")
    public String receive(@PathVariable Long id) {
        service.updatePurchaseOrderStatus(id, "Received");
        return "redirect:/purchaseorders";
    }

    @GetMapping("/vendor/{id}")
    @ResponseBody
    public Map<String, Object> getVendor(@PathVariable Long id) {
        Vendor vendor = vendorService.getVendorById(id);
        Map<String, Object> map = new HashMap<>();
        if (vendor != null) {
            map.put("vendorId", vendor.getVendorId());
            map.put("vendorName", vendor.getVendorName());
            map.put("contactPerson", vendor.getContactPerson());
            map.put("mobile", vendor.getMobile());
            map.put("email", vendor.getEmail());
            map.put("address", vendor.getAddress());
            map.put("city", vendor.getCity());
            map.put("state", vendor.getState());
            map.put("pincode", vendor.getPincode());
            map.put("transactionAccount", vendor.getTransactionAccount());
        }
        return map;
    }

    @GetMapping("/vendor/{id}/mappings")
    @ResponseBody
    public List<Map<String, Object>> getVendorMappings(@PathVariable Long id) {
        List<VendorItemMapping> entities = vendorItemMappingRepository.findByVendorId(id);
        List<Map<String, Object>> mappings = new ArrayList<>();
        Set<Long> seenItemIds = new HashSet<>();
        if (entities != null) {
            for (VendorItemMapping entity : entities) {
                if (entity != null && entity.getItemId() != null && !seenItemIds.contains(entity.getItemId())) {
                    seenItemIds.add(entity.getItemId());
                    Map<String, Object> map = new HashMap<>();
                    map.put("mappingId", entity.getVenItemId());
                    map.put("rate", entity.getRate());
                    Item item = itemService.getItemById(entity.getItemId());
                    if (item != null) {
                        map.put("itemId", item.getItemId());
                        map.put("itemCode", item.getItemCode());
                        map.put("itemName", item.getItemName());
                        if (item.getType() != null) {
                            map.put("typeId", item.getType().getTypeId());
                            map.put("typeName", item.getType().getTypeName());
                        }
                    }
                    mappings.add(map);
                }
            }
        }
        return mappings;
    }

    @PostMapping("/update-item-status")
    @ResponseBody
    public Map<String, Object> updateItemStatus(
            @RequestParam("orderId") Long orderId,
            @RequestParam("status") String status) {
        Map<String, Object> response = new HashMap<>();
        try {
            service.updatePurchaseOrderStatus(orderId, status);
            response.put("success", true);
            response.put("message", "Status updated successfully");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    @GetMapping("/po-details/{id}")
    @ResponseBody
    public Map<String, Object> getPurchaseOrderDetailsJson(@PathVariable Long id) {
        PurchaseOrder order = service.getPurchaseOrderById(id);
        Map<String, Object> response = new HashMap<>();
        if (order == null) {
            return response;
        }

        response.put("orderId", order.getOrderId());
        response.put("purchaseOrderNo", order.getPurchaseOrderNo());
        response.put("vendorId", order.getVendorId());
        response.put("vendorName", order.getVendorName());
        response.put("transactionAccount", order.getTransactionAccount());
        response.put("totalCost", order.getTotalCost());
        response.put("totalQuantity", order.getTotalQuantity());
        response.put("status", order.getStatus());
        response.put("entryDate", order.getEntryDate());

        List<Map<String, Object>> itemDetailsList = new ArrayList<>();
        if (order.getDetails() != null) {
            for (PurchaseOrderMapping detail : order.getDetails()) {
                Map<String, Object> map = new HashMap<>();
                map.put("itemId", detail.getItemId());
                map.put("quantity", detail.getQuantity());
                map.put("receivedQuantity", detail.getReceivedQuantity() != null ? detail.getReceivedQuantity() : 0);
                map.put("rate", detail.getRate());
                map.put("totalCost", detail.getTotalCost());
                map.put("status", detail.getStatus());
                map.put("receiveOrderStatus", detail.getReceiveOrderStatus());

                if (detail.getItem() != null) {
                    map.put("itemCode", detail.getItem().getItemCode());
                    map.put("itemName", detail.getItem().getItemName());
                    map.put("itemType", detail.getItem().getType() != null ? detail.getItem().getType().getTypeName() : "N/A");
                } else if (detail.getItemId() != null) {
                    Item item = itemService.getItemById(detail.getItemId());
                    if (item != null) {
                        map.put("itemCode", item.getItemCode());
                        map.put("itemName", item.getItemName());
                        map.put("itemType", item.getType() != null ? item.getType().getTypeName() : "N/A");
                    } else {
                        map.put("itemCode", "N/A");
                        map.put("itemName", "Item #" + detail.getItemId());
                        map.put("itemType", "N/A");
                    }
                }
                itemDetailsList.add(map);
            }
        }
        response.put("details", itemDetailsList);
        return response;
    }

    @GetMapping("/pending-booking-summary")
    @ResponseBody
    public List<Map<String, Object>> getPendingBookingSummary() {
        List<Item> items = itemService.getAllItems();

        Map<Long, Integer> pendingBookingQtyMap = new HashMap<>();
        List<BookingRequestItemMapping> bookingMappings = bookingRequestItemMappingRepository.findAll();
        for (BookingRequestItemMapping m : bookingMappings) {
            if (m.getStatus() != null && "Pending".equalsIgnoreCase(m.getStatus())) {
                if (m.getItemId() != null && m.getQuantity() != null) {
                    pendingBookingQtyMap.merge((long) m.getItemId(), m.getQuantity(), Integer::sum);
                }
            }
        }

        Map<Long, Double> stockMap = new HashMap<>();
        for (CurrentStock cs : currentStockService.getAllStocks()) {
            if (cs.getItemId() != null) {
                stockMap.put(cs.getItemId(), cs.getCurrentStock() != null ? cs.getCurrentStock() : 0.0);
            }
        }

        Map<Long, Integer> pendingPOQtyMap = new HashMap<>();
        List<PurchaseOrderMapping> poDetails = purchaseOrderMappingRepository.findAll();
        for (PurchaseOrderMapping d : poDetails) {
            if (d.getIsValid() == null || d.getIsValid() != 0) {
                int status = d.getStatus() != null ? d.getStatus() : 1;
                if (status < 3) {
                    int qty = d.getQuantity() != null ? d.getQuantity() : 0;
                    int recQty = d.getReceivedQuantity() != null ? d.getReceivedQuantity() : 0;
                    int pending = Math.max(0, qty - recQty);
                    if (pending > 0 && d.getItemId() != null) {
                        pendingPOQtyMap.merge(d.getItemId(), pending, Integer::sum);
                    }
                }
            }
        }

        List<Map<String, Object>> summary = new ArrayList<>();
        for (Item item : items) {
            int bookingRequestQty = pendingBookingQtyMap.getOrDefault(item.getItemId(), 0);
            if (bookingRequestQty > 0) {
                Map<String, Object> row = new HashMap<>();
                row.put("itemId", item.getItemId());
                row.put("itemCode", item.getItemCode());
                row.put("itemName", item.getItemName());
                row.put("bookingRequest", bookingRequestQty);
                row.put("currentStock", stockMap.getOrDefault(item.getItemId(), 0.0));
                row.put("purchaseOrder", pendingPOQtyMap.getOrDefault(item.getItemId(), 0));
                summary.add(row);
            }
        }

        return summary;
    }
}
