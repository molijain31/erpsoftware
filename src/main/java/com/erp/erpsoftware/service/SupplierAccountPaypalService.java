package com.erp.erpsoftware.service;

import com.erp.erpsoftware.entity.Supplier;
import com.erp.erpsoftware.entity.SupplierAccountPaypal;
import com.erp.erpsoftware.entity.BookingRequest;
import com.erp.erpsoftware.entity.SalesOrder;
import com.erp.erpsoftware.entity.Item;
import com.erp.erpsoftware.repository.SupplierAccountPaypalRepository;
import com.erp.erpsoftware.repository.BookingRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional
public class SupplierAccountPaypalService {

    @Autowired
    private SupplierAccountPaypalRepository repository;

    @Autowired
    private SupplierService supplierService;

    @Autowired
    private BookingRequestRepository bookingRequestRepository;

    @Autowired
    private BookingRequestService bookingRequestService;

    @Autowired
    private com.erp.erpsoftware.repository.SalesOrderRepository salesOrderRepository;

    @Autowired
    private ItemService itemService;

    @Autowired
    private com.erp.erpsoftware.repository.SupplierItemMappingRepository supplierItemMappingRepository;

    @Autowired
    private com.erp.erpsoftware.repository.SalesOrderDetailRepository detailRepository;

    @Autowired
    private com.erp.erpsoftware.repository.AccountLedgerRepository accountLedgerRepository;

    public List<SupplierAccountPaypal> getAllAccounts() {
        return getAllSupplierAccounts();
    }

    public List<SupplierAccountPaypal> getAllSupplierAccounts() {
        List<Supplier> suppliers = new ArrayList<>();
        try {
            suppliers = supplierService.getAllSuppliers();
        } catch (Exception e) {
            System.err.println("Error reading suppliers: " + e.getMessage());
        }

        List<SupplierAccountPaypal> savedAccounts = new ArrayList<>();
        try {
            savedAccounts = repository.findAllActive();
        } catch (Exception e) {
            System.err.println("Error reading supplier accounts: " + e.getMessage());
        }

        Map<Long, SupplierAccountPaypal> accountMap = new HashMap<>();
        for (SupplierAccountPaypal acc : savedAccounts) {
            if (acc.getSupplierId() != null) {
                accountMap.putIfAbsent(acc.getSupplierId(), acc);
            }
        }

        List<SupplierAccountPaypal> result = new ArrayList<>();
        if (suppliers != null) {
            for (Supplier s : suppliers) {
                SupplierAccountPaypal item;
                if (accountMap.containsKey(s.getSupplierId())) {
                    item = accountMap.get(s.getSupplierId());
                } else {
                    item = new SupplierAccountPaypal();
                    item.setSupplierId(s.getSupplierId());
                    item.setAccountName(s.getSupplierName());
                    item.setPaypalEmail(s.getEmail() != null ? s.getEmail() : "");
                    item.setEntryDate(s.getEntryDate());
                    item.setModifiedDate(s.getModifiedDate());
                    item.setIsValid(1);
                }
                String name = s.getSupplierName();
                if (name == null || name.trim().isEmpty()) {
                    name = item.getAccountName();
                }
                if (name == null || name.trim().isEmpty()) {
                    name = "Supplier #" + s.getSupplierId();
                }
                item.setSupplierName(name);
                if (item.getAccountName() == null || item.getAccountName().trim().isEmpty()) {
                    item.setAccountName(name);
                }

                try {
                    Map<String, Object> summary = getSupplierReceivedSummary(s.getSupplierId());
                    if (summary != null) {
                        Integer fQty = (Integer) summary.get("totalFullyReceivedQty");
                        Double fCost = (Double) summary.get("totalFullyReceivedCost");
                        List<?> items = (List<?>) summary.get("items");

                        if (items != null && !items.isEmpty() && hasCreatedSalesOrder(s.getSupplierId(), summary)) {
                            item.setFullyReceivedQty(fQty != null ? fQty : 0);
                            item.setFullyReceivedCost(fCost != null ? fCost : 0.0);
                            Boolean fullyPaid = (Boolean) summary.get("isFullyPaid");
                            double totalHistorical = summary.get("totalHistoricalPayment") != null ? ((Number) summary.get("totalHistoricalPayment")).doubleValue() : 0.0;
                            double pending = Math.max(0.0, (fCost != null ? fCost : 0.0) - totalHistorical);

                            if (Boolean.TRUE.equals(fullyPaid) || pending < 0.01) {
                                item.setPendingAmount(0.0);
                                item.setIsFullyPaid(true);
                            } else {
                                item.setPendingAmount(pending);
                                item.setIsFullyPaid(false);
                            }

                            // Populate CO Order numbers and clickable list for each individual CO
                            Set<String> coSet = new LinkedHashSet<>();
                            List<Map<String, String>> coList = new ArrayList<>();
                            Set<String> seenCo = new HashSet<>();

                            for (Object itObj : items) {
                                if (itObj instanceof Map) {
                                    Map<String, Object> itMap = (Map<String, Object>) itObj;
                                    String defaultBNo = (String) itMap.get("bookingNo");
                                    if (defaultBNo == null) defaultBNo = "";

                                    String rawCo = (String) itMap.get("salesOrderNo");
                                    if (rawCo == null || rawCo.trim().isEmpty() || "-".equals(rawCo.trim()) || "—".equals(rawCo.trim())) {
                                        rawCo = (String) itMap.get("salesNo");
                                    }
                                    if (rawCo == null || rawCo.trim().isEmpty() || "-".equals(rawCo.trim()) || "—".equals(rawCo.trim())) {
                                        rawCo = (String) itMap.get("challanNo");
                                    }

                                    if (rawCo != null && !rawCo.trim().isEmpty() && !"-".equals(rawCo.trim()) && !"—".equals(rawCo.trim())) {
                                        String[] splitParts = rawCo.split(",");
                                        for (String part : splitParts) {
                                            String singleCo = part.trim();
                                            if (!singleCo.isEmpty() && !"-".equals(singleCo) && !"—".equals(singleCo)) {
                                                coSet.add(singleCo);
                                                if (seenCo.add(singleCo)) {
                                                    // Resolve the specific bookingNo for this individual CO order
                                                    String specificBNo = defaultBNo.trim();
                                                    try {
                                                        List<SalesOrder> sos = salesOrderRepository.findBySalesOrderNo(singleCo);
                                                        if (sos != null && !sos.isEmpty() && sos.get(0).getBookingNo() != null && !sos.get(0).getBookingNo().trim().isEmpty()) {
                                                            specificBNo = sos.get(0).getBookingNo().trim();
                                                        }
                                                    } catch (Exception ignored) {}

                                                    Map<String, String> coMap = new HashMap<>();
                                                    coMap.put("coNo", singleCo);
                                                    coMap.put("bookingNo", specificBNo);
                                                    String urlTarget = "/sales/new";
                                                    if (!specificBNo.isEmpty()) {
                                                        try {
                                                            urlTarget += "?bookingNo=" + java.net.URLEncoder.encode(specificBNo, java.nio.charset.StandardCharsets.UTF_8);
                                                        } catch (Exception ignored) {
                                                            urlTarget += "?bookingNo=" + specificBNo;
                                                        }
                                                    }
                                                    coMap.put("url", urlTarget);
                                                    coList.add(coMap);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            String coDisplay = coSet.isEmpty() ? "-" : String.join(", ", coSet);
                            item.setCoOrderNo(coDisplay);
                            item.setPurchaseOrderNo(coDisplay);
                            item.setCoOrderList(coList);

                            List<String> coLinkHtmls = new ArrayList<>();
                            for (Map<String, String> coMapItem : coList) {
                                String cNo = coMapItem.get("coNo");
                                String bNo = coMapItem.get("bookingNo");
                                if (cNo != null && !cNo.isEmpty()) {
                                    String link = "<a href='javascript:void(0);' onclick='showCODetailsModal(\"" + cNo + "\", \"" + (bNo != null ? bNo : "") + "\")' "
                                        + "style='color:#0f172a;font-weight:700;text-decoration:none;white-space:nowrap;cursor:pointer;' "
                                        + "class='co-link'>"
                                        + cNo + "</a>";
                                    coLinkHtmls.add(link);
                                }
                            }
                            item.setCoLinks(String.join(", ", coLinkHtmls));
                            
                            result.add(item);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error fetching summary for supplier " + s.getSupplierId() + ": " + e.getMessage());
                }
            }
        }

        // Deduplicate by normalized supplier name — keep the latest entry per supplier
        Map<String, SupplierAccountPaypal> deduplicatedMap = new LinkedHashMap<>();
        for (SupplierAccountPaypal acc : result) {
            String normName = acc.getSupplierName() != null ? acc.getSupplierName().trim().toLowerCase() : "";
            if (normName.isEmpty()) continue;
            if (!deduplicatedMap.containsKey(normName)) {
                deduplicatedMap.put(normName, acc);
            } else {
                SupplierAccountPaypal existing = deduplicatedMap.get(normName);
                Date d1 = acc.getModifiedDate() != null ? acc.getModifiedDate() : (acc.getEntryDate() != null ? acc.getEntryDate() : new Date(0));
                Date d2 = existing.getModifiedDate() != null ? existing.getModifiedDate() : (existing.getEntryDate() != null ? existing.getEntryDate() : new Date(0));
                if (d1.after(d2)) {
                    deduplicatedMap.put(normName, acc);
                }
            }
        }
        return new ArrayList<>(deduplicatedMap.values());
    }

    private boolean hasCreatedSalesOrder(Long supplierId, Map<String, Object> summary) {
        if (summary == null) return false;
        List<Map<String, Object>> items = (List<Map<String, Object>>) summary.get("items");
        if (items != null) {
            for (Map<String, Object> item : items) {
                String bNo = (String) item.get("bookingNo");
                if (bNo != null && !bNo.trim().isEmpty()) {
                    try {
                        List<SalesOrder> sos = salesOrderRepository.findByBookingNo(bNo.trim());
                        if (sos != null && !sos.isEmpty()) {
                            for (SalesOrder so : sos) {
                                if (so.getOrderId() != null) {
                                    List<com.erp.erpsoftware.entity.SalesOrderDetail> details = detailRepository.findActiveDetailsByOrderId(so.getOrderId());
                                    if (details != null && !details.isEmpty()) {
                                        return true;
                                    }
                                }
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }
        }
        
        if (supplierId != null) {
            try {
                List<SalesOrder> allSos = salesOrderRepository.findAllActive();
                if (allSos != null) {
                    for (SalesOrder so : allSos) {
                        if (supplierId.equals(so.getSupplierId()) && so.getOrderId() != null) {
                            List<com.erp.erpsoftware.entity.SalesOrderDetail> details = detailRepository.findActiveDetailsByOrderId(so.getOrderId());
                            if (details != null && !details.isEmpty()) {
                                return true;
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}
        }
        return false;
    }

    public SupplierAccountPaypal getAccountById(Long id) {
        SupplierAccountPaypal account = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Supplier PayPal Account not found with id: " + id));
        populateSupplierInfo(account);
        return account;
    }

    private void populateSupplierInfo(SupplierAccountPaypal account) {
        if (account.getSupplierId() != null) {
            try {
                Supplier supplier = supplierService.getSupplierById(account.getSupplierId());
                if (supplier != null) {
                    account.setSupplierName(supplier.getSupplierName());
                }
            } catch (Exception e) {
                account.setSupplierName("Unknown (ID: " + account.getSupplierId() + ")");
            }

            try {
                Map<String, Object> summary = getSupplierReceivedSummary(account.getSupplierId());
                account.setFullyReceivedQty((Integer) summary.get("totalFullyReceivedQty"));
                account.setFullyReceivedCost((Double) summary.get("totalFullyReceivedCost"));
                Boolean fullyPaid = (Boolean) summary.get("isFullyPaid");
                account.setIsFullyPaid(fullyPaid != null ? fullyPaid : false);
            } catch (Exception e) {
                System.err.println("Error populating supplier info: " + e.getMessage());
            }
        }
    }

    public Map<String, Object> getSupplierReceivedSummary(Long supplierId) {
        Map<String, Object> result = new HashMap<>();
        int totalQty = 0;
        double totalCost = 0.0;
        double totalSupplierPayment = 0.0;
        double remainingPayment = 0.0;
        List<Map<String, Object>> brDetails = new ArrayList<>();

        Supplier supplier = null;
        String sName = "";
        if (supplierId != null) {
            try {
                supplier = supplierService.getSupplierById(supplierId);
                if (supplier != null) sName = supplier.getSupplierName();
            } catch (Exception ignored) {}
        }
        result.put("supplierName", sName);

        List<SupplierAccountPaypal> savedAccounts = new ArrayList<>();
        try {
            savedAccounts = repository.findAllActive();
        } catch (Exception e) {
            System.err.println("Error reading supplier accounts: " + e.getMessage());
        }

        for (SupplierAccountPaypal acc : savedAccounts) {
            if (supplierId != null && supplierId.equals(acc.getSupplierId())) {
                double amt = acc.getAmountReceived() != null ? acc.getAmountReceived() : 0.0;
                totalSupplierPayment += amt;
            }
        }

        List<BookingRequest> allRequests = new ArrayList<>();
        try {
            allRequests = bookingRequestRepository.findAll();
        } catch (Exception e) {
            System.err.println("Error reading booking requests: " + e.getMessage());
        }

        Set<Long> mappedItemIds = new HashSet<>();
        if (supplierId != null) {
            try {
                List<com.erp.erpsoftware.entity.SupplierItemMapping> mappings = supplierItemMappingRepository.findBySupplierSupplierId(supplierId);
                if (mappings != null) {
                    for (com.erp.erpsoftware.entity.SupplierItemMapping m : mappings) {
                        if (m.getItem() != null && m.getItem().getItemId() != null) {
                            mappedItemIds.add(m.getItem().getItemId());
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        Map<String, List<BookingRequest>> groupedByBookingNo = new LinkedHashMap<>();
        for (BookingRequest br : allRequests) {
            boolean matches = false;
            String brSuppId = br.getSupplierId();
            if (brSuppId != null && !brSuppId.trim().isEmpty() && !"null".equalsIgnoreCase(brSuppId.trim())) {
                if (supplierId != null && brSuppId.trim().equals(String.valueOf(supplierId))) {
                    matches = true;
                }
            } else {
                if (sName != null && !sName.isEmpty() && (sName.equalsIgnoreCase(br.getSupplierName()) || (br.getSupplierName() != null && br.getSupplierName().toLowerCase().contains(sName.toLowerCase())))) {
                    matches = true;
                }
                if (!matches && br.getBookingNo() != null && !mappedItemIds.isEmpty()) {
                    List<com.erp.erpsoftware.entity.BookingRequestItemMapping> brMappings =
                            bookingRequestService.getItemMappingsByBookingNo(br.getBookingNo());
                    if (brMappings != null) {
                        for (com.erp.erpsoftware.entity.BookingRequestItemMapping m : brMappings) {
                            if (m.getItemId() != null && mappedItemIds.contains(m.getItemId().longValue())) {
                                matches = true;
                                break;
                            }
                        }
                    }
                }
            }

            if (matches) {
                String bNo = br.getBookingNo() != null && !br.getBookingNo().trim().isEmpty() 
                        ? br.getBookingNo().trim() 
                        : ("BR-ID-" + br.getBookingNo());
                groupedByBookingNo.computeIfAbsent(bNo, k -> new ArrayList<>()).add(br);
            }
        }

        List<Map<String, Object>> orderList = new ArrayList<>();
        for (Map.Entry<String, List<BookingRequest>> entry : groupedByBookingNo.entrySet()) {
            String bNo = entry.getKey();
            List<BookingRequest> items = entry.getValue();

            int brQty = 0;
            double brCost = 0.0;
            // Use item mappings for cost calculation
            List<com.erp.erpsoftware.entity.BookingRequestItemMapping> mappings =
                    bookingRequestService != null ? bookingRequestService.getItemMappingsByBookingNo(bNo) : null;
            if (mappings != null && !mappings.isEmpty()) {
                for (com.erp.erpsoftware.entity.BookingRequestItemMapping m : mappings) {
                    int q = m.getQuantity() != null ? m.getQuantity() : 0;
                    brQty += q;
                    try {
                        Item item = itemService.getItemById(m.getItemId().longValue());
                        if (item != null && item.getRate() != null) {
                            brCost += item.getRate() * q;
                        }
                    } catch (Exception ignored) {}
                }
            } else {
                // Fallback: sum from booking_request total_quantity/total_cost header
                for (BookingRequest r : items) {
                    brQty += r.getTotalQuantity() != null ? r.getTotalQuantity() : 0;
                    brCost += r.getTotalCost() != null ? r.getTotalCost() : 0.0;
                }
            }

            Map<String, Object> orderObj = new HashMap<>();
            orderObj.put("bookingNo", bNo);
            orderObj.put("totalQuantity", brQty);
            orderObj.put("totalCost", brCost);
            orderObj.put("items", items);
            orderList.add(orderObj);
        }

        orderList.sort((a, b) -> Double.compare(
                (Double) b.get("totalCost"),
                (Double) a.get("totalCost")
        ));

        // Check if any booking request for this supplier has stored payment details in sales_order or booking_request
        boolean hasStoredPayments = false;
        for (Map<String, Object> order : orderList) {
            List<BookingRequest> bList = (List<BookingRequest>) order.get("items");
            if (bList != null) {
                for (BookingRequest br : bList) {
                    if (br.getPaymentAmount() != null && br.getPaymentAmount() > 0) {
                        hasStoredPayments = true;
                        break;
                    }
                    if (br.getPaymentStatus() != null && !br.getPaymentStatus().isEmpty() && !br.getPaymentStatus().contains("1") && !br.getPaymentStatus().equalsIgnoreCase("Unpaid")) {
                        hasStoredPayments = true;
                        break;
                    }
                }
            }
            String bNo = (String) order.get("bookingNo");
            if (bNo != null && !bNo.trim().isEmpty()) {
                String cleanBNo = bNo.trim();
                String altBNo = cleanBNo.startsWith("BR-") ? cleanBNo.substring(3) : ("BR-" + cleanBNo);
                try {
                    List<SalesOrder> existingSos = salesOrderRepository.findByBookingNo(cleanBNo);
                    if (existingSos == null || existingSos.isEmpty()) {
                        existingSos = salesOrderRepository.findByBookingNo(altBNo);
                    }
                    if (existingSos != null && !existingSos.isEmpty()) {
                        for (SalesOrder existingSo : existingSos) {
                            if (existingSo.getReceivedAmount() != null && existingSo.getReceivedAmount() > 0) {
                                hasStoredPayments = true;
                                break;
                            } else if (existingSo.getPaymentStatus() != null && !existingSo.getPaymentStatus().isEmpty() && !existingSo.getPaymentStatus().contains("1") && !existingSo.getPaymentStatus().equalsIgnoreCase("Unpaid")) {
                                hasStoredPayments = true;
                                break;
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
        }

        remainingPayment = totalSupplierPayment;

        for (Map<String, Object> order : orderList) {
            String bNo = (String) order.get("bookingNo");
            int brQty = (Integer) order.get("totalQuantity");
            double brCost = (Double) order.get("totalCost");

            totalQty += brQty;
            totalCost += brCost;

            double paidAmt = 0.0;
            int statusCode = 1;
            String statusStr = "1 - Unpaid";

            if (hasStoredPayments) {
                // Read stored payment details from sales_order / booking_request without database update
                List<BookingRequest> bList = (List<BookingRequest>) order.get("items");
                if (bList != null && !bList.isEmpty()) {
                    for (BookingRequest br : bList) {
                        if (br.getPaymentAmount() != null && br.getPaymentAmount() > paidAmt) {
                            paidAmt = br.getPaymentAmount();
                        }
                        if (br.getPaymentStatus() != null && !br.getPaymentStatus().isEmpty() && !br.getPaymentStatus().contains("1")) {
                            statusStr = br.getPaymentStatus();
                        }
                    }
                }
                if (bNo != null && !bNo.trim().isEmpty()) {
                    String cleanBNo = bNo.trim();
                    String altBNo = cleanBNo.startsWith("BR-") ? cleanBNo.substring(3) : ("BR-" + cleanBNo);
                    try {
                        List<SalesOrder> existingSos = salesOrderRepository.findByBookingNo(cleanBNo);
                        if (existingSos == null || existingSos.isEmpty()) {
                            existingSos = salesOrderRepository.findByBookingNo(altBNo);
                        }
                        if (existingSos != null && !existingSos.isEmpty()) {
                            for (SalesOrder existingSo : existingSos) {
                                if (existingSo.getReceivedAmount() != null && existingSo.getReceivedAmount() > paidAmt) {
                                    paidAmt = existingSo.getReceivedAmount();
                                }
                                if (existingSo.getPaymentStatus() != null && !existingSo.getPaymentStatus().isEmpty() && !existingSo.getPaymentStatus().contains("1")) {
                                    statusStr = existingSo.getPaymentStatus();
                                }
                            }
                        }
                    } catch (Exception ignored) {}
                }

                if (paidAmt >= brCost - 0.01 && brCost > 0) {
                    statusCode = 3;
                    statusStr = "3 - Full Payment";
                } else if (paidAmt > 0) {
                    statusCode = 2;
                    statusStr = "2 - Partially Paid";
                } else {
                    statusCode = 1;
                    statusStr = "1 - Unpaid";
                }
            } else {
                // Initial fallback: totalSupplierPayment > 0 and no stored per-BR payment details yet
                if (brCost > 0 && remainingPayment > 0) {
                    paidAmt = Math.min(remainingPayment, brCost);
                    remainingPayment -= paidAmt;
                }

                if (paidAmt >= brCost - 0.01 && brCost > 0) {
                    statusCode = 3;
                    statusStr = "3 - Full Payment";
                } else if (paidAmt > 0) {
                    statusCode = 2;
                    statusStr = "2 - Partially Paid";
                } else {
                    statusCode = 1;
                    statusStr = "1 - Unpaid";
                }

                try {
                    if (bNo != null && !bNo.trim().isEmpty()) {
                        String cleanBNo = bNo.trim();
                        bookingRequestRepository.updatePaymentDetails(cleanBNo, paidAmt, statusStr);
                        int updatedRows = salesOrderRepository.updatePaymentDetails(cleanBNo, paidAmt, statusStr);
                        String altBNo = cleanBNo.startsWith("BR-") ? cleanBNo.substring(3) : ("BR-" + cleanBNo);
                        if (updatedRows == 0) {
                            salesOrderRepository.updatePaymentDetails(altBNo, paidAmt, statusStr);
                        }
                        bookingRequestRepository.updatePaymentDetails(altBNo, paidAmt, statusStr);
                    }
                } catch (Exception e) {
                    System.err.println("Error updating booking request / sales order payment status: " + e.getMessage());
                }
            }

            String salesOrderNo = "—";
            if (bNo != null && !bNo.trim().isEmpty()) {
                try {
                    List<SalesOrder> existingSos = salesOrderRepository.findByBookingNo(bNo.trim());
                    if (existingSos != null && !existingSos.isEmpty()) {
                        List<String> coList = new ArrayList<>();
                        for (SalesOrder so : existingSos) {
                            if (so.getSalesOrderNo() != null && !so.getSalesOrderNo().trim().isEmpty()) {
                                coList.add(so.getSalesOrderNo().trim());
                            }
                        }
                        if (!coList.isEmpty()) {
                            salesOrderNo = String.join(", ", coList);
                        }
                    }
                } catch (Exception ignored) {}
            }

            String entryDateStr = "—";
            try {
                if (bNo != null && !bNo.trim().isEmpty()) {
                    List<SalesOrder> existingSos = salesOrderRepository.findByBookingNo(bNo.trim());
                    if (existingSos != null && !existingSos.isEmpty()) {
                        Date d = existingSos.get(0).getEntryDate() != null ? existingSos.get(0).getEntryDate() : existingSos.get(0).getModifiedDate();
                        if (d != null) {
                            entryDateStr = new java.text.SimpleDateFormat("dd-MMM-yyyy").format(d);
                        }
                    }
                }
            } catch (Exception ignored) {}
            if ("—".equals(entryDateStr)) {
                try {
                    if (bNo != null && !bNo.trim().isEmpty()) {
                        List<BookingRequest> brs = bookingRequestRepository.findByBookingNo(bNo.trim());
                        if (brs != null && !brs.isEmpty() && brs.get(0).getEntryDate() != null) {
                            entryDateStr = new java.text.SimpleDateFormat("dd-MMM-yyyy").format(brs.get(0).getEntryDate());
                        }
                    }
                } catch (Exception ignored) {}
            }

            Map<String, Object> row = new HashMap<>();
            row.put("bookingNo", bNo);
            row.put("salesOrderNo", salesOrderNo);
            row.put("salesNo", salesOrderNo);
            row.put("challanNo", salesOrderNo);
            row.put("totalQuantity", brQty);
            row.put("totalCost", brCost);
            row.put("alreadyPaid", paidAmt);
            row.put("paymentAmount", paidAmt);
            row.put("paymentStatus", statusStr);
            row.put("statusCode", statusCode);
            row.put("entryDate", entryDateStr);
            row.put("paidDate", entryDateStr);
            brDetails.add(row);
        }

        double totalPaidForItems = 0.0;
        boolean allPaid = !brDetails.isEmpty();
        for (Map<String, Object> row : brDetails) {
            Double p = (Double) row.get("paymentAmount");
            Double c = (Double) row.get("totalCost");
            if (p != null) totalPaidForItems += p;
            if (c != null && c > 0) {
                if (p == null || p < c - 0.01) {
                    allPaid = false;
                }
            }
        }

        double actualHistorical = Math.max(totalSupplierPayment, totalPaidForItems);
        result.put("supplierId", supplierId);
        result.put("totalFullyReceivedQty", totalQty);
        result.put("totalFullyReceivedCost", totalCost);
        result.put("isFullyPaid", allPaid);
        result.put("items", brDetails);
        result.put("excessAdvanceBalance", remainingPayment > 0 ? remainingPayment : 0.0);
        result.put("totalHistoricalPayment", actualHistorical);
        return result;
    }

    public SupplierAccountPaypal saveAccount(SupplierAccountPaypal account) {
        return saveAccount(account, null, null, null);
    }

    public SupplierAccountPaypal saveAccount(SupplierAccountPaypal account, List<String> brBookingNos, List<Double> brPaymentAmounts) {
        return saveAccount(account, brBookingNos, brPaymentAmounts, null);
    }

    public SupplierAccountPaypal saveAccount(SupplierAccountPaypal account, List<String> brBookingNos, List<Double> brPaymentAmounts, Double adjustAmount) {
        if (account.getAccountName() == null || account.getAccountName().trim().isEmpty()) {
            if (account.getSupplierId() != null) {
                try {
                    Supplier s = supplierService.getSupplierById(account.getSupplierId());
                    if (s != null && s.getSupplierName() != null && !s.getSupplierName().trim().isEmpty()) {
                        account.setAccountName(s.getSupplierName());
                    }
                } catch (Exception ignored) {}
            }
            if (account.getAccountName() == null || account.getAccountName().trim().isEmpty()) {
                account.setAccountName("Supplier Account");
            }
        }

        Long supplierId = account.getSupplierId();
        if (supplierId != null) {
            try {
                Map<String, Object> summaryBefore = getSupplierReceivedSummary(supplierId);
                double totalBrCost = summaryBefore.get("totalFullyReceivedCost") != null ? ((Number) summaryBefore.get("totalFullyReceivedCost")).doubleValue() : 0.0;
                double totalHistorical = summaryBefore.get("totalHistoricalPayment") != null ? ((Number) summaryBefore.get("totalHistoricalPayment")).doubleValue() : 0.0;
                double paidAmt = account.getAmountReceived() != null ? account.getAmountReceived() : 0.0;
                double totalWithThis = totalHistorical + paidAmt;
                double excess = Math.max(0.0, totalWithThis - totalBrCost);
                account.setBalance(excess);
            } catch (Exception e) {
                System.err.println("Error calculating adjust balance on save: " + e.getMessage());
            }
        }

        SupplierAccountPaypal saved = repository.save(account);
        if (supplierId != null) {
            if (brBookingNos != null && brPaymentAmounts != null && brBookingNos.size() == brPaymentAmounts.size() && !brBookingNos.isEmpty()) {
                updateSpecificBrPayments(supplierId, brBookingNos, brPaymentAmounts);
            } else {
                updateTopDownBrPayments(supplierId, account.getAmountReceived());
            }
        }
        return saved;
    }

    public void updateSpecificBrPayments(Long supplierId, List<String> brBookingNos, List<Double> brPaymentAmounts) {
        Map<String, Object> summary = getSupplierReceivedSummary(supplierId);
        String sName = (String) summary.get("supplierName");
        List<Map<String, Object>> items = (List<Map<String, Object>>) summary.get("items");
        Map<String, Double> brCostMap = new HashMap<>();
        if (items != null) {
            for (Map<String, Object> item : items) {
                String bNo = (String) item.get("bookingNo");
                Double cost = (Double) item.get("totalCost");
                if (bNo != null) {
                    brCostMap.put(bNo.trim(), cost != null ? cost : 0.0);
                }
            }
        }

        for (int i = 0; i < brBookingNos.size(); i++) {
            String bNo = brBookingNos.get(i);
            Double paidAmt = brPaymentAmounts.get(i);
            if (bNo == null || bNo.trim().isEmpty()) continue;
            if (paidAmt == null || paidAmt < 0) paidAmt = 0.0;

            String cleanBNo = bNo.trim();
            Double brCost = brCostMap.getOrDefault(cleanBNo, 0.0);

            String statusStr;
            int statusCode;
            if (paidAmt >= brCost - 0.01 && brCost > 0) {
                statusCode = 3;
                statusStr = "3 - Full Payment";
            } else if (paidAmt > 0) {
                statusCode = 2;
                statusStr = "2 - Partially Paid";
            } else {
                statusCode = 1;
                statusStr = "1 - Unpaid";
            }

            try {
                String altBNo = cleanBNo.startsWith("BR-") ? cleanBNo.substring(3) : ("BR-" + cleanBNo);
                bookingRequestRepository.updatePaymentDetails(cleanBNo, paidAmt, statusStr);
                bookingRequestRepository.updatePaymentDetails(altBNo, paidAmt, statusStr);
                
                salesOrderRepository.updatePaymentDetailsFlex(cleanBNo, altBNo, paidAmt, statusStr);
                List<SalesOrder> sos = salesOrderRepository.findByBookingNoFlex(cleanBNo, altBNo);
                if (sos != null) {
                    for (SalesOrder so : sos) {
                        so.setReceivedAmount(paidAmt);
                        so.setPaymentStatus(statusStr);
                        salesOrderRepository.save(so);
                    }
                }

                if (accountLedgerRepository != null) {
                    recordSupplierLedgerEntry(supplierId, cleanBNo, sName, brCost, paidAmt, statusStr, statusCode);
                }
            } catch (Exception e) {
                System.err.println("Error updating payment details for " + cleanBNo + ": " + e.getMessage());
            }
        }
    }

    public void updateTopDownBrPayments(Long supplierId, Double totalPayment) {
        Map<String, Object> summary = getSupplierReceivedSummary(supplierId);
        String sName = (String) summary.get("supplierName");
        List<Map<String, Object>> items = (List<Map<String, Object>>) summary.get("items");
        if (items == null || items.isEmpty()) return;

        double remaining = totalPayment != null ? totalPayment : 0.0;
        for (Map<String, Object> item : items) {
            String bNo = (String) item.get("bookingNo");
            Double brCost = (Double) item.get("totalCost");
            if (bNo == null || bNo.trim().isEmpty()) continue;
            if (brCost == null) brCost = 0.0;

            double paidAmt = 0.0;
            if (brCost > 0 && remaining > 0) {
                paidAmt = Math.min(remaining, brCost);
                remaining -= paidAmt;
            }

            String statusStr;
            int statusCode;
            if (paidAmt >= brCost - 0.01 && brCost > 0) {
                statusCode = 3;
                statusStr = "3 - Full Payment";
            } else if (paidAmt > 0) {
                statusCode = 2;
                statusStr = "2 - Partially Paid";
            } else {
                statusCode = 1;
                statusStr = "1 - Unpaid";
            }

            String cleanBNo = bNo.trim();
            try {
                String altBNo = cleanBNo.startsWith("BR-") ? cleanBNo.substring(3) : ("BR-" + cleanBNo);
                bookingRequestRepository.updatePaymentDetails(cleanBNo, paidAmt, statusStr);
                bookingRequestRepository.updatePaymentDetails(altBNo, paidAmt, statusStr);

                salesOrderRepository.updatePaymentDetailsFlex(cleanBNo, altBNo, paidAmt, statusStr);
                List<SalesOrder> sos = salesOrderRepository.findByBookingNoFlex(cleanBNo, altBNo);
                if (sos != null) {
                    for (SalesOrder so : sos) {
                        so.setReceivedAmount(paidAmt);
                        so.setPaymentStatus(statusStr);
                        salesOrderRepository.save(so);
                    }
                }

                if (accountLedgerRepository != null) {
                    recordSupplierLedgerEntry(supplierId, cleanBNo, sName, brCost, paidAmt, statusStr, statusCode);
                }
            } catch (Exception e) {
                System.err.println("Error updating top-down payment details for " + cleanBNo + ": " + e.getMessage());
            }
        }
    }

    private void recordSupplierLedgerEntry(Long supplierId, String bookingNo, String accountDetail, Double totalCost, Double paymentAmount, String paymentStatus, Integer statusCode) {
        if (bookingNo == null || bookingNo.trim().isEmpty()) return;
        String cleanBNo = bookingNo.trim();
        List<com.erp.erpsoftware.entity.AccountLedger> existing = accountLedgerRepository.findByBookingNo(cleanBNo);
        com.erp.erpsoftware.entity.AccountLedger entry;
        if (existing != null && !existing.isEmpty()) {
            entry = existing.get(0);
        } else {
            entry = new com.erp.erpsoftware.entity.AccountLedger();
            entry.setEntryDate(new Date());
        }
        entry.setEntityType("SUPPLIER");
        entry.setSupplierId(supplierId);
        entry.setBookingNo(cleanBNo);
        if (accountDetail != null && !accountDetail.trim().isEmpty()) {
            entry.setAccountDetail(accountDetail);
        }
        entry.setTotalCost(totalCost != null ? totalCost : 0.0);
        entry.setPaymentAmount(paymentAmount != null ? paymentAmount : 0.0);
        entry.setPaymentStatus(paymentStatus != null ? paymentStatus : "1 - Unpaid");
        entry.setStatusCode(statusCode != null ? statusCode : 1);
        entry.setIsValid(1);
        entry.setModifiedDate(new Date());
        accountLedgerRepository.save(entry);
    }

    public List<SupplierAccountPaypal> getAccountsBySupplierId(Long supplierId) {
        return repository.findActiveBySupplierId(supplierId);
    }

    public Map<String, Object> getSupplierPaymentHistory(Long supplierId) {
        Map<String, Object> result = new HashMap<>();
        String supplierName = "";
        if (supplierId != null) {
            try {
                Supplier s = supplierService.getSupplierById(supplierId);
                if (s != null && s.getSupplierName() != null) {
                    supplierName = s.getSupplierName();
                }
            } catch (Exception ignored) {}
        }
        result.put("supplierId", supplierId);
        result.put("supplierName", supplierName);

        List<SupplierAccountPaypal> savedAccounts = repository.findAllActive();
        List<SupplierAccountPaypal> supplierPayments = new ArrayList<>();
        double totalPaidAmt = 0.0;
        if (savedAccounts != null && supplierId != null) {
            for (SupplierAccountPaypal acc : savedAccounts) {
                if (supplierId.equals(acc.getSupplierId())) {
                    supplierPayments.add(acc);
                    if (acc.getAmountReceived() != null) {
                        totalPaidAmt += acc.getAmountReceived();
                    }
                }
            }
        }
        // Sort payments chronologically ascending to compute allocation over time
        supplierPayments.sort((a, b) -> {
            Date da = a.getEntryDate() != null ? a.getEntryDate() : a.getModifiedDate();
            Date db = b.getEntryDate() != null ? b.getEntryDate() : b.getModifiedDate();
            if (da != null && db != null) {
                int cmp = da.compareTo(db);
                if (cmp != 0) return cmp;
            }
            Long idA = a.getAccountId() != null ? a.getAccountId() : 0L;
            Long idB = b.getAccountId() != null ? b.getAccountId() : 0L;
            return idA.compareTo(idB);
        });

        Map<String, Object> brSummary = getSupplierReceivedSummary(supplierId);
        result.put("summary", brSummary);

        List<Map<String, Object>> brItems = (List<Map<String, Object>>) brSummary.get("items");
        if (brItems == null) brItems = new ArrayList<>();

        // Group multiple payments under their respective Booking Request / Challan Order
        List<Map<String, Object>> brListWithPayments = new ArrayList<>();
        for (Map<String, Object> it : brItems) {
            Map<String, Object> copy = new HashMap<>(it);
            copy.put("payments", new ArrayList<Map<String, Object>>());
            brListWithPayments.add(copy);
        }

        List<Map<String, Object>> advanceCredits = new ArrayList<>();

        int receiptNum = 1;
        for (SupplierAccountPaypal p : supplierPayments) {
            String rNo = "Receipt #" + receiptNum++;
            Date rawDate = p.getEntryDate() != null ? p.getEntryDate() : p.getModifiedDate();
            String pDate = rawDate != null ? new java.text.SimpleDateFormat("dd-MMM-yyyy").format(rawDate) : "—";
            String mode = p.getPaymentMode() != null ? p.getPaymentMode() : "UPI";
            String ref = p.getNeftNo() != null && !p.getNeftNo().trim().isEmpty() ? p.getNeftNo() :
                        (p.getRtgsNo() != null && !p.getRtgsNo().trim().isEmpty() ? p.getRtgsNo() :
                        (p.getChequeBook() != null && !p.getChequeBook().trim().isEmpty() ? p.getChequeBook() : "—"));
            double remPayment = p.getAmountReceived() != null ? p.getAmountReceived() : 0.0;
            double originalSent = remPayment;
            List<Map<String, Object>> thisReceiptInstallments = new ArrayList<>();

            for (Map<String, Object> brObj : brListWithPayments) {
                double cost = brObj.get("totalCost") != null ? ((Number) brObj.get("totalCost")).doubleValue() : 0.0;
                double currentPaid = 0.0;
                List<Map<String, Object>> pList = (List<Map<String, Object>>) brObj.get("payments");
                for (Map<String, Object> prevP : pList) {
                    currentPaid += ((Number) prevP.get("amount")).doubleValue();
                }
                double remainingCost = Math.max(0.0, cost - currentPaid);

                if (remainingCost > 0 && remPayment > 0) {
                    double alloc = Math.min(remPayment, remainingCost);
                    remPayment -= alloc;

                    Map<String, Object> installment = new HashMap<>();
                    installment.put("receiptNo", rNo);
                    installment.put("paymentDate", pDate);
                    installment.put("paymentMode", mode);
                    installment.put("refNo", ref);
                    installment.put("amount", alloc);
                    installment.put("adjustAmount", 0.0);
                    installment.put("totalReceiptAmount", originalSent);
                    pList.add(installment);
                    thisReceiptInstallments.add(installment);
                }
            }

            if (remPayment > 0.01) {
                if (!thisReceiptInstallments.isEmpty()) {
                    thisReceiptInstallments.get(thisReceiptInstallments.size() - 1).put("adjustAmount", remPayment);
                }
                Map<String, Object> adv = new HashMap<>();
                adv.put("receiptNo", rNo);
                adv.put("paymentDate", pDate);
                adv.put("paymentMode", mode);
                adv.put("refNo", ref);
                adv.put("advanceAmount", remPayment);
                adv.put("totalReceiptAmount", originalSent);
                advanceCredits.add(adv);
            }
        }

        result.put("brList", brListWithPayments);
        result.put("advanceCredits", advanceCredits);
        result.put("payments", supplierPayments);
        result.put("totalPaidAmount", totalPaidAmt);

        return result;
    }

    public void deleteAccount(Long id) {
        SupplierAccountPaypal account = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Supplier PayPal Account not found with id: " + id));
        account.setIsValid(0);
        repository.save(account);
    }
}
