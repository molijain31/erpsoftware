package com.erp.erpsoftware.controller;

import com.erp.erpsoftware.entity.Item;
import com.erp.erpsoftware.entity.StockLedger;
import com.erp.erpsoftware.repository.BookingRequestItemMappingRepository;
import com.erp.erpsoftware.repository.BookingRequestRepository;
import com.erp.erpsoftware.repository.PurchaseOrderMappingRepository;
import com.erp.erpsoftware.repository.SalesOrderDetailRepository;
import com.erp.erpsoftware.repository.SalesRepository;
import com.erp.erpsoftware.service.CurrentStockService;
import com.erp.erpsoftware.service.ItemService;
import com.erp.erpsoftware.service.StockLedgerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.*;

@Controller
@RequestMapping("/reports")
public class ReportController {

    @Autowired
    private ItemService itemService;

    @Autowired
    private StockLedgerService stockLedgerService;

    @Autowired
    private CurrentStockService currentStockService;

    @Autowired
    private PurchaseOrderMappingRepository purchaseOrderMappingRepository;

    @Autowired
    private SalesRepository salesRepository;

    @Autowired
    private SalesOrderDetailRepository salesOrderDetailRepository;

    @Autowired
    private BookingRequestItemMappingRepository bookingRequestItemMappingRepository;

    @Autowired
    private BookingRequestRepository bookingRequestRepository;

    @Autowired
    private com.erp.erpsoftware.service.SupplierService supplierService;

    @Autowired
    private com.erp.erpsoftware.service.VendorService vendorService;

    @Autowired
    private com.erp.erpsoftware.service.SupplierAccountPaypalService supplierAccountPaypalService;

    @Autowired
    private com.erp.erpsoftware.service.AccountPaypalService accountPaypalService;



    @GetMapping("/ledger-stock")
    public String ledgerStockReport(Model model) {
        List<Long> ledgerItemIds = stockLedgerService.getDistinctItemIds();
        List<Item> items = new ArrayList<>();
        if (ledgerItemIds != null) {
            for (Long id : ledgerItemIds) {
                Item item = itemService.getItemById(id);
                if (item != null && (item.getIsValid() == null || item.getIsValid() != 0)) {
                    items.add(item);
                }
            }
        }
        model.addAttribute("items", items);
        return "ledger-stock-report";
    }

    @GetMapping("/ledger-stock/details/{itemId}")
    @ResponseBody
    public Map<String, Object> getItemLedgerDetails(@PathVariable Long itemId) {
        Map<String, Object> response = new HashMap<>();
        try {
            Item item = itemService.getItemById(itemId);
            if (item == null) {
                response.put("error", "Item not found");
                return response;
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            // 1. Fetch entries directly from the stock_ledger table
            List<StockLedger> dbLedgerList = stockLedgerService.getEntriesByItemId(itemId);
            List<Map<String, Object>> allEvents = new ArrayList<>();

            double totalReceived = 0.0;
            double totalSent = 0.0;
            String lastModified = "N/A";

            if (dbLedgerList != null && !dbLedgerList.isEmpty()) {
                // Sort in ASCENDING order by entry date (oldest first) to compute running balances
                List<StockLedger> ascList = new ArrayList<>(dbLedgerList);
                ascList.sort(Comparator.comparing(l -> l.getEntryDate() != null ? l.getEntryDate() : (l.getModifiedDate() != null ? l.getModifiedDate() : new Date(0))));

                double runningBalance = 0.0;
                List<Map<String, Object>> calculatedEventsAsc = new ArrayList<>();
                for (StockLedger ledger : ascList) {
                    Map<String, Object> ev = new HashMap<>();
                    Date dt = ledger.getEntryDate() != null ? ledger.getEntryDate() : (ledger.getModifiedDate() != null ? ledger.getModifiedDate() : new Date());
                    
                    double recv = ledger.getReceiveQuantity() != null ? ledger.getReceiveQuantity() : 0.0;
                    double sent = ledger.getSendQuantity() != null ? ledger.getSendQuantity() : 0.0;
                    
                    totalReceived += recv;
                    totalSent += sent;
                    runningBalance = runningBalance + recv - sent;

                    ev.put("date", sdf.format(dt));
                    ev.put("received", recv);
                    ev.put("sent", sent);
                    ev.put("updatedStock", (int) Math.round(runningBalance));
                    ev.put("rawDate", dt);
                    calculatedEventsAsc.add(ev);

                    // Persist running balance update back to DB for consistency
                    try {
                        ledger.setBalance(runningBalance);
                        stockLedgerService.save(ledger);
                    } catch (Exception ignored) {}
                }

                // Sort ASCENDING (oldest entry on top, newest at bottom) for table display
                calculatedEventsAsc.sort((a, b) -> ((Date) a.get("rawDate")).compareTo((Date) b.get("rawDate")));

                for (Map<String, Object> ev : calculatedEventsAsc) {
                    ev.remove("rawDate");
                    allEvents.add(ev);
                }

                // Last modified date should be the date of the latest transaction in the ledger
                Date latestDate = ascList.get(ascList.size() - 1).getModifiedDate();
                if (latestDate == null) {
                    latestDate = ascList.get(ascList.size() - 1).getEntryDate();
                }
                if (latestDate != null) {
                    lastModified = sdf.format(latestDate);
                }
            }

            if ("N/A".equals(lastModified)) {
                if (item.getModifiedDate() != null) {
                    lastModified = sdf.format(item.getModifiedDate());
                } else if (item.getEntryDate() != null) {
                    lastModified = sdf.format(item.getEntryDate());
                } else {
                    lastModified = sdf.format(new Date());
                }
            }

            response.put("itemId", item.getItemId());
            response.put("itemCode", item.getItemCode() != null ? item.getItemCode() : "N/A");
            response.put("itemName", item.getItemName() != null ? item.getItemName() : "N/A");
            response.put("itemType", item.getType() != null ? item.getType().getTypeName() : "N/A");
            response.put("received", totalReceived);
            response.put("sent", totalSent);
            response.put("stock", totalReceived - totalSent);
            response.put("transactions", allEvents);

            return response;
        } catch (Exception e) {
            e.printStackTrace();
            response.put("error", "Error loading details: " + e.getMessage());
            return response;
        }
    }

    @GetMapping("/ledger-stock/details/all")
    @ResponseBody
    public List<Map<String, Object>> getAllItemsLedgerDetails() {
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            List<Long> ledgerItemIds = stockLedgerService.getDistinctItemIds();
            if (ledgerItemIds != null) {
                for (Long id : ledgerItemIds) {
                    Item item = itemService.getItemById(id);
                    if (item != null && (item.getIsValid() == null || item.getIsValid() != 0)) {
                        Map<String, Object> details = getItemLedgerDetails(id);
                        if (details != null && !details.containsKey("error")) {
                            result.add(details);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}
