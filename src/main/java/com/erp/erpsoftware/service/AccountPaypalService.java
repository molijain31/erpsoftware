package com.erp.erpsoftware.service;

import com.erp.erpsoftware.entity.AccountPaypal;
import com.erp.erpsoftware.entity.Vendor;
import com.erp.erpsoftware.repository.AccountPaypalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional
public class AccountPaypalService {

    @Autowired
    private AccountPaypalRepository repository;

    @Autowired
    private VendorService vendorService;

    @Autowired
    private com.erp.erpsoftware.repository.PurchaseOrderRepository purchaseOrderRepository;

    @Autowired
    private com.erp.erpsoftware.repository.PurchaseOrderMappingRepository detailRepository;

    @Autowired
    private ItemService itemService;

    public List<AccountPaypal> getAllAccounts() {
        return getAllVendorAccounts();
    }

    public List<AccountPaypal> getAllVendorAccounts() {
        List<Vendor> vendors = new ArrayList<>();
        try {
            vendors = vendorService.getAllVendors();
        } catch (Exception e) {
            System.err.println("Error reading vendors: " + e.getMessage());
        }

        List<AccountPaypal> savedAccounts = new ArrayList<>();
        try {
            savedAccounts = repository.findAllActive();
        } catch (Exception e) {
            System.err.println("Error reading accounts: " + e.getMessage());
        }

        Map<Long, AccountPaypal> accountMap = new HashMap<>();
        for (AccountPaypal acc : savedAccounts) {
            if (acc != null && acc.getVendorId() != null) {
                if (!accountMap.containsKey(acc.getVendorId())) {
                    accountMap.put(acc.getVendorId(), acc);
                } else {
                    AccountPaypal prev = accountMap.get(acc.getVendorId());
                    Date d1 = acc.getEntryDate() != null ? acc.getEntryDate() : new Date(0);
                    Date d2 = prev.getEntryDate() != null ? prev.getEntryDate() : new Date(0);
                    if (d1.after(d2)) {
                        accountMap.put(acc.getVendorId(), acc);
                    }
                }
            }
        }

        Map<String, AccountPaypal> vendorMap = new LinkedHashMap<>();
        Set<String> processedVendorNames = new HashSet<>();

        if (vendors != null) {
            for (Vendor v : vendors) {
                if (v == null || v.getVendorId() == null) continue;
                String name = v.getVendorName();
                if (name == null || name.trim().isEmpty()) {
                    name = "Vendor #" + v.getVendorId();
                }
                String normName = name.trim().toLowerCase();
                if (processedVendorNames.contains(normName)) {
                    continue;
                }
                processedVendorNames.add(normName);

                // Find primary vendor ID for this vendor name (prefer one with saved account)
                Long primaryVendorId = v.getVendorId();
                for (Vendor otherV : vendors) {
                    if (otherV != null && otherV.getVendorName() != null &&
                        otherV.getVendorName().trim().equalsIgnoreCase(normName)) {
                        if (accountMap.containsKey(otherV.getVendorId())) {
                            primaryVendorId = otherV.getVendorId();
                            break;
                        }
                    }
                }

                AccountPaypal item;
                if (accountMap.containsKey(primaryVendorId)) {
                    item = accountMap.get(primaryVendorId);
                } else {
                    item = new AccountPaypal();
                    item.setVendorId(primaryVendorId);
                    item.setAccountName(name);
                    item.setPaypalEmail(v.getTransactionAccount() != null ? v.getTransactionAccount() : "");
                    item.setEntryDate(v.getEntryDate());
                    item.setModifiedDate(v.getModifiedDate());
                    item.setIsValid(1);
                }
                item.setVendorId(primaryVendorId);
                item.setVendorName(name);
                if (item.getAccountName() == null || item.getAccountName().trim().isEmpty()) {
                    item.setAccountName(name);
                }

                try {
                    Map<String, Object> summary = getVendorFullyReceivedSummary(primaryVendorId);
                    if (summary != null) {
                        List<?> items = (List<?>) summary.get("items");
                        if (items != null && !items.isEmpty()) {
                            Number fQtyNum = (Number) summary.get("totalFullyReceivedQty");
                            Number fCostNum = (Number) summary.get("totalFullyReceivedCost");
                            Integer fQty = fQtyNum != null ? fQtyNum.intValue() : 0;
                            Double fCost = fCostNum != null ? fCostNum.doubleValue() : 0.0;

                            item.setFullyReceivedQty(fQty);
                            item.setFullyReceivedCost(fCost);

                            // Compute lastModPaid (total amount paid), pendingAmount, poNumbers, poLinks, latestDate
                            double totalPaid = 0.0;
                            double totalCostAll = 0.0;
                            List<String> poNos = new java.util.ArrayList<>();
                            List<String> poLinkHtmls = new java.util.ArrayList<>();
                            Date latestDate = item.getModifiedDate() != null ? item.getModifiedDate() : item.getEntryDate();

                            for (Object rawItem : items) {
                                if (rawItem instanceof Map) {
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> row = (Map<String, Object>) rawItem;
                                    Number paid = (Number) row.get("alreadyPaid");
                                    Number cost = (Number) row.get("totalCost");
                                    String poNo = (String) row.get("poNo");
                                    Number orderId = (Number) row.get("orderId");
                                    Date pDate = (Date) row.get("rawDate");
                                    if (pDate != null && (latestDate == null || pDate.after(latestDate))) {
                                        latestDate = pDate;
                                    }
                                    if (paid != null) totalPaid += paid.doubleValue();
                                    if (cost != null) totalCostAll += cost.doubleValue();
                                    if (poNo != null && !poNo.isEmpty()) {
                                        poNos.add(poNo);
                                        String idStr = orderId != null ? String.valueOf(orderId.longValue()) : "";
                                        String link = "<a href='javascript:void(0);' onclick='showPODetailsModal(" + idStr + ")' "
                                            + "style='color:#0f172a;font-weight:700;text-decoration:none;white-space:nowrap;cursor:pointer;' "
                                            + "class='po-link'>"
                                            + poNo + "</a>";
                                        poLinkHtmls.add(link);
                                    }
                                }
                            }

                            if (latestDate != null) {
                                item.setModifiedDate(latestDate);
                            }
                            item.setLastModPaid(totalPaid);
                            double pending = Math.max(0.0, totalCostAll - totalPaid);
                            item.setPendingAmount(pending);
                            item.setIsFullyPaid(pending < 0.01);
                            item.setPoNumbers(String.join(", ", poNos));
                            item.setPoLinks(String.join(", ", poLinkHtmls));

                            vendorMap.put(normName, item);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error calculating summary for vendor " + primaryVendorId + ": " + e.getMessage());
                }
            }
        }

        return new ArrayList<>(vendorMap.values());
    }

    public AccountPaypal getAccountById(Long id) {
        AccountPaypal account = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("PayPal Account not found with id: " + id));
        populateVendorInfo(account);
        return account;
    }

    private void populateVendorInfo(AccountPaypal account) {
        if (account != null && account.getVendorId() != null) {
            try {
                Vendor v = vendorService.getVendorById(account.getVendorId());
                if (v != null) {
                    account.setVendorName(v.getVendorName());
                }
            } catch (Exception ignored) {}
        }
    }

    public Map<String, Object> getVendorFullyReceivedSummary(Long vendorId) {
        Map<String, Object> result = new java.util.HashMap<>();
        int totalQty = 0;
        double totalCost = 0.0;
        double totalVendorPayment = 0.0;
        double remainingPayment = 0.0;
        List<Map<String, Object>> poDetails = new java.util.ArrayList<>();

        if (vendorId != null) {
            String vendorName = "";
            Set<Long> matchingVendorIds = new HashSet<>();
            matchingVendorIds.add(vendorId);
            try {
                Vendor v = vendorService.getVendorById(vendorId);
                if (v != null && v.getVendorName() != null) {
                    vendorName = v.getVendorName();
                    String normName = vendorName.trim().toLowerCase();
                    List<Vendor> allV = vendorService.getAllVendors();
                    if (allV != null) {
                        for (Vendor otherV : allV) {
                            if (otherV != null && otherV.getVendorName() != null &&
                                otherV.getVendorName().trim().equalsIgnoreCase(normName)) {
                                if (otherV.getVendorId() != null) {
                                    matchingVendorIds.add(otherV.getVendorId());
                                }
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}
            result.put("vendorName", vendorName);

            List<AccountPaypal> savedAccounts = new ArrayList<>();
            try {
                savedAccounts = repository.findAllActive();
            } catch (Exception e) {
                System.err.println("Error fetching active accounts: " + e.getMessage());
            }

            for (AccountPaypal acc : savedAccounts) {
                if (acc != null) {
                    boolean matches = false;
                    if (acc.getVendorId() != null && matchingVendorIds.contains(acc.getVendorId())) {
                        matches = true;
                    } else if (acc.getAccountName() != null && !vendorName.isEmpty() &&
                               acc.getAccountName().trim().equalsIgnoreCase(vendorName.trim())) {
                        matches = true;
                    }
                    if (matches) {
                        double amt = acc.getAmountReceived() != null ? acc.getAmountReceived() : 0.0;
                        totalVendorPayment += amt;
                    }
                }
            }

            List<com.erp.erpsoftware.entity.PurchaseOrder> orders = new ArrayList<>();
            try {
                orders = purchaseOrderRepository.findAllActive();
            } catch (Exception e) {
                System.err.println("Error fetching active purchase orders: " + e.getMessage());
            }

            List<com.erp.erpsoftware.entity.PurchaseOrder> vendorOrders = new java.util.ArrayList<>();
            Set<Long> processedOrderIds = new HashSet<>();

            for (com.erp.erpsoftware.entity.PurchaseOrder order : orders) {
                if (order != null && order.getOrderId() != null && !processedOrderIds.contains(order.getOrderId())) {
                    boolean matches = false;
                    if (order.getVendorId() != null && matchingVendorIds.contains(order.getVendorId())) {
                        matches = true;
                    } else if (order.getVendorName() != null && !vendorName.isEmpty() &&
                               order.getVendorName().trim().equalsIgnoreCase(vendorName.trim())) {
                        matches = true;
                    }

                    List<com.erp.erpsoftware.entity.PurchaseOrderMapping> details = new ArrayList<>();
                    try {
                        details = detailRepository.findActiveDetailsByOrderId(order.getOrderId());
                    } catch (Exception e) {
                        System.err.println("Error fetching details for order " + order.getOrderId() + ": " + e.getMessage());
                    }

                    if (!matches && details != null) {
                        for (com.erp.erpsoftware.entity.PurchaseOrderMapping d : details) {
                            if ((d.getVendorId() != null && matchingVendorIds.contains(d.getVendorId())) ||
                                (d.getVendorName() != null && !vendorName.isEmpty() && d.getVendorName().trim().equalsIgnoreCase(vendorName.trim()))) {
                                matches = true;
                                break;
                            }
                        }
                    }

                    if (matches) {
                        processedOrderIds.add(order.getOrderId());
                        int poTotalQty = 0;
                        double poTotalCost = 0.0;
                        if (details != null && !details.isEmpty()) {
                            for (com.erp.erpsoftware.entity.PurchaseOrderMapping d : details) {
                                int rQty = d.getReceivedQuantity() != null ? d.getReceivedQuantity() : 0;
                                int qty = d.getQuantity() != null ? d.getQuantity() : 0;
                                int usedQty = rQty > 0 ? rQty : qty;
                                poTotalQty += usedQty;
                                double rate = d.getRate() != null ? d.getRate() : (d.getRateCost() != null ? d.getRateCost() : 0.0);
                                double dCost = d.getTotalCost() != null ? d.getTotalCost() : (rate * usedQty);
                                poTotalCost += dCost;
                            }
                        }

                        if (poTotalCost <= 0.0 && order.getTotalCost() != null) {
                            poTotalCost = order.getTotalCost();
                        }
                        if (poTotalQty <= 0 && order.getTotalQuantity() != null) {
                            poTotalQty = order.getTotalQuantity();
                        }

                        order.setTotalCost(poTotalCost);
                        order.setTotalQuantity(poTotalQty);
                        if (poTotalCost > 0 || poTotalQty > 0) {
                            vendorOrders.add(order);
                        }
                    }
                }
            }

            vendorOrders.sort((a, b) -> {
                if (a.getOrderId() != null && b.getOrderId() != null) {
                    return a.getOrderId().compareTo(b.getOrderId());
                }
                return 0;
            });

            remainingPayment = totalVendorPayment;

            for (com.erp.erpsoftware.entity.PurchaseOrder order : vendorOrders) {
                double poCost = order.getTotalCost() != null ? order.getTotalCost() : 0.0;
                int poQty = order.getTotalQuantity() != null ? order.getTotalQuantity() : 0;

                totalQty += poQty;
                totalCost += poCost;

                double paidAmt = order.getPaymentAmount() != null ? order.getPaymentAmount() : 0.0;
                String statusStr = order.getPaymentStatus() != null && !order.getPaymentStatus().isEmpty() ? order.getPaymentStatus() : "1 - Unpaid";
                int statusCode = 1;

                if (paidAmt >= poCost - 0.01 && poCost > 0) {
                    statusCode = 3;
                    statusStr = "3 - Full Payment";
                } else if (paidAmt > 0) {
                    statusCode = 2;
                    statusStr = "2 - Partially Paid";
                } else {
                    statusCode = 1;
                    statusStr = "1 - Unpaid";
                }

                Date rawDate = order.getModifiedDate() != null ? order.getModifiedDate() : order.getEntryDate();
                String entryDateStr = order.getEntryDate() != null ? new java.text.SimpleDateFormat("dd-MMM-yyyy").format(order.getEntryDate()) : (order.getModifiedDate() != null ? new java.text.SimpleDateFormat("dd-MMM-yyyy").format(order.getModifiedDate()) : "—");

                Map<String, Object> row = new java.util.HashMap<>();
                row.put("poNo", order.getPurchaseOrderNo());
                row.put("purchaseOrderNo", order.getPurchaseOrderNo());
                row.put("purchaseNo", order.getPurchaseOrderNo());
                row.put("bookingNo", order.getPurchaseOrderNo());
                row.put("orderId", order.getOrderId());
                row.put("entryDate", entryDateStr);
                row.put("rawDate", rawDate);
                row.put("totalQuantity", poQty);
                row.put("totalCost", poCost);
                row.put("alreadyPaid", paidAmt);
                row.put("paymentAmount", paidAmt);
                row.put("paymentStatus", statusStr);
                row.put("statusCode", statusCode);
                row.put("paidDate", entryDateStr);
                poDetails.add(row);
            }
        }

        boolean allPoPaid = !poDetails.isEmpty();
        for (Map<String, Object> row : poDetails) {
            Number stCode = (Number) row.get("statusCode");
            if (stCode == null || stCode.intValue() < 3) {
                allPoPaid = false;
                break;
            }
        }

        double totalHistoricalPayment = 0.0;
        for (Map<String, Object> row : poDetails) {
            Number pAmt = (Number) row.get("alreadyPaid");
            if (pAmt != null) {
                totalHistoricalPayment += pAmt.doubleValue();
            }
        }
        double excessAdvance = Math.max(0.0, totalHistoricalPayment - totalCost);

        // Also read the latest stored balance (advance amount) from account_paypal for this vendor
        double storedAdvanceBalance = 0.0;
        if (vendorId != null) {
            List<AccountPaypal> vendorAccounts = new ArrayList<>();
            try {
                List<AccountPaypal> allAccts = repository.findAllActive();
                for (AccountPaypal acc : allAccts) {
                    if (acc != null && vendorId.equals(acc.getVendorId())) {
                        vendorAccounts.add(acc);
                    }
                }
                // Sum up all balance entries for this vendor
                for (AccountPaypal acc : vendorAccounts) {
                    if (acc.getBalance() != null && acc.getBalance() > 0) {
                        storedAdvanceBalance += acc.getBalance();
                    }
                }
            } catch (Exception e) {
                System.err.println("Error reading stored advance balance: " + e.getMessage());
            }
        }

        result.put("vendorId", vendorId);
        result.put("totalFullyReceivedQty", totalQty);
        result.put("totalFullyReceivedCost", totalCost);
        result.put("isFullyPaid", allPoPaid);
        result.put("items", poDetails);
        result.put("excessAdvanceBalance", excessAdvance);
        result.put("storedAdvanceBalance", storedAdvanceBalance);
        result.put("totalHistoricalPayment", totalHistoricalPayment);
        return result;
    }

    public AccountPaypal saveAccount(AccountPaypal account) {
        return saveAccount(account, null, null);
    }

    public AccountPaypal saveAccount(AccountPaypal account, List<Long> poOrderIds, List<Double> poPaymentAmounts) {
        if (account.getAccountName() == null || account.getAccountName().trim().isEmpty()) {
            if (account.getVendorId() != null) {
                try {
                    Vendor v = vendorService.getVendorById(account.getVendorId());
                    if (v != null && v.getVendorName() != null && !v.getVendorName().trim().isEmpty()) {
                        account.setAccountName(v.getVendorName());
                    }
                } catch (Exception ignored) {}
            }
            if (account.getAccountName() == null || account.getAccountName().trim().isEmpty()) {
                account.setAccountName("Vendor Account");
            }
        }

        Long vendorId = account.getVendorId();
        double sentAmount = account.getAmountReceived() != null ? account.getAmountReceived() : 0.0;

        if (poOrderIds != null && poPaymentAmounts != null && !poOrderIds.isEmpty()) {
            double totalAllocated = 0.0;
            for (int i = 0; i < poOrderIds.size(); i++) {
                Long orderId = poOrderIds.get(i);
                Double paidVal = (i < poPaymentAmounts.size() && poPaymentAmounts.get(i) != null) ? poPaymentAmounts.get(i) : 0.0;
                if (orderId != null && paidVal > 0) {
                    totalAllocated += paidVal;
                    try {
                        com.erp.erpsoftware.entity.PurchaseOrder po = purchaseOrderRepository.findById(orderId).orElse(null);
                        if (po != null) {
                            double curPaid = po.getPaymentAmount() != null ? po.getPaymentAmount() : 0.0;
                            double newPaid = curPaid + paidVal;
                            po.setPaymentAmount(newPaid);
                            double cost = po.getTotalCost() != null ? po.getTotalCost() : 0.0;
                            if (newPaid >= cost - 0.01 && cost > 0) {
                                po.setPaymentStatus("3 - Full Payment");
                            } else if (newPaid > 0) {
                                po.setPaymentStatus("2 - Partially Paid");
                            } else {
                                po.setPaymentStatus("1 - Unpaid");
                            }
                            purchaseOrderRepository.save(po);
                        }
                    } catch (Exception e) {
                        System.err.println("Error updating PO " + orderId + ": " + e.getMessage());
                    }
                }
            }
            double excess = Math.max(0.0, sentAmount - totalAllocated);
            account.setBalance(excess);
        } else if (vendorId != null && sentAmount > 0) {
            // Allocate FIFO across unpaid purchase orders
            try {
                List<com.erp.erpsoftware.entity.PurchaseOrder> orders = purchaseOrderRepository.findAllActive();
                List<com.erp.erpsoftware.entity.PurchaseOrder> vOrders = new ArrayList<>();
                for (com.erp.erpsoftware.entity.PurchaseOrder o : orders) {
                    if (o != null && vendorId.equals(o.getVendorId())) {
                        vOrders.add(o);
                    }
                }
                vOrders.sort((a, b) -> (a.getOrderId() != null && b.getOrderId() != null) ? a.getOrderId().compareTo(b.getOrderId()) : 0);

                double remaining = sentAmount;
                for (com.erp.erpsoftware.entity.PurchaseOrder po : vOrders) {
                    double cost = po.getTotalCost() != null ? po.getTotalCost() : 0.0;
                    double curPaid = po.getPaymentAmount() != null ? po.getPaymentAmount() : 0.0;
                    double unpaid = Math.max(0.0, cost - curPaid);
                    if (unpaid > 0 && remaining > 0) {
                        double allocate = Math.min(remaining, unpaid);
                        remaining -= allocate;
                        double newPaid = curPaid + allocate;
                        po.setPaymentAmount(newPaid);
                        if (newPaid >= cost - 0.01 && cost > 0) {
                            po.setPaymentStatus("3 - Full Payment");
                        } else {
                            po.setPaymentStatus("2 - Partially Paid");
                        }
                        purchaseOrderRepository.save(po);
                    }
                }
                account.setBalance(Math.max(0.0, remaining));
            } catch (Exception e) {
                System.err.println("Error FIFO allocating payment: " + e.getMessage());
            }
        }

        AccountPaypal saved = repository.save(account);
        return saved;
    }

    public void deleteAccount(Long id) {
        AccountPaypal account = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("PayPal Account not found with id: " + id));
        account.setIsValid(0);
        repository.save(account);
    }

    public Map<String, Object> getVendorPaymentHistory(Long vendorId) {
        Map<String, Object> result = new HashMap<>();
        String vendorName = "";
        if (vendorId != null) {
            try {
                Vendor v = vendorService.getVendorById(vendorId);
                if (v != null && v.getVendorName() != null) {
                    vendorName = v.getVendorName();
                }
            } catch (Exception ignored) {}
        }
        result.put("vendorId", vendorId);
        result.put("vendorName", vendorName);

        List<AccountPaypal> savedAccounts = repository.findAllActive();
        List<AccountPaypal> vendorPayments = new ArrayList<>();
        double totalPaidAmt = 0.0;
        if (savedAccounts != null && vendorId != null) {
            for (AccountPaypal acc : savedAccounts) {
                if (vendorId.equals(acc.getVendorId())) {
                    vendorPayments.add(acc);
                    if (acc.getAmountReceived() != null) {
                        totalPaidAmt += acc.getAmountReceived();
                    }
                }
            }
        }

        // Sort payments chronologically ascending to compute allocation over time
        vendorPayments.sort((a, b) -> {
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

        Map<String, Object> poSummary = getVendorFullyReceivedSummary(vendorId);
        result.put("summary", poSummary);

        List<Map<String, Object>> poItems = (List<Map<String, Object>>) poSummary.get("items");
        if (poItems == null) poItems = new ArrayList<>();

        // Group multiple payments under their respective Purchase Order
        List<Map<String, Object>> poListWithPayments = new ArrayList<>();
        for (Map<String, Object> it : poItems) {
            Map<String, Object> copy = new HashMap<>(it);
            copy.put("payments", new ArrayList<Map<String, Object>>());
            poListWithPayments.add(copy);
        }

        List<Map<String, Object>> advanceCredits = new ArrayList<>();

        int receiptNum = 1;
        for (AccountPaypal p : vendorPayments) {
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

            for (Map<String, Object> poObj : poListWithPayments) {
                double cost = poObj.get("totalCost") != null ? ((Number) poObj.get("totalCost")).doubleValue() : 0.0;
                double currentPaid = 0.0;
                List<Map<String, Object>> pList = (List<Map<String, Object>>) poObj.get("payments");
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

        result.put("poList", poListWithPayments);
        result.put("advanceCredits", advanceCredits);
        result.put("payments", vendorPayments);
        result.put("totalPaidAmount", totalPaidAmt);

        return result;
    }
}
