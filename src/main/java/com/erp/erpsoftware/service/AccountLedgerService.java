package com.erp.erpsoftware.service;

import com.erp.erpsoftware.entity.AccountLedger;
import com.erp.erpsoftware.repository.AccountLedgerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.erp.erpsoftware.entity.SupplierAccountPaypal;
import com.erp.erpsoftware.entity.AccountPaypal;
import com.erp.erpsoftware.entity.Supplier;
import com.erp.erpsoftware.entity.Vendor;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;
import java.util.Objects;

@Service
@Transactional
public class AccountLedgerService {

    @Autowired
    private SupplierService supplierService;

    @Autowired
    private VendorService vendorService;

    @Autowired
    private SupplierAccountPaypalService supplierAccountPaypalService;

    @Autowired
    private AccountPaypalService accountPaypalService;

    @Autowired
    private AccountLedgerRepository accountLedgerRepository;

    @Autowired
    private com.erp.erpsoftware.repository.SalesOrderRepository salesOrderRepository;

    public List<Supplier> getAllSuppliers() {
        try {
            List<SupplierAccountPaypal> accounts = supplierAccountPaypalService.getAllSupplierAccounts();
            Set<Long> validSupplierIds = new HashSet<>();
            if (accounts != null) {
                for (SupplierAccountPaypal acc : accounts) {
                    if (acc != null && acc.getSupplierId() != null) {
                        validSupplierIds.add(acc.getSupplierId());
                    }
                }
            }
            List<Supplier> allSuppliers = supplierService.getAllSuppliers();
            if (allSuppliers == null) return new ArrayList<>();
            return allSuppliers.stream()
                    .filter(s -> s != null && s.getSupplierId() != null && validSupplierIds.contains(s.getSupplierId()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error fetching suppliers for Account Ledger: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<Vendor> getAllVendors() {
        try {
            List<AccountPaypal> accounts = accountPaypalService.getAllVendorAccounts();
            Set<Long> validVendorIds = new HashSet<>();
            if (accounts != null) {
                for (AccountPaypal acc : accounts) {
                    if (acc != null && acc.getVendorId() != null) {
                        validVendorIds.add(acc.getVendorId());
                    }
                }
            }
            List<Vendor> allVendors = vendorService.getAllVendors();
            if (allVendors == null) return new ArrayList<>();
            return allVendors.stream()
                    .filter(v -> v != null && v.getVendorId() != null && validVendorIds.contains(v.getVendorId()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error fetching vendors for Account Ledger: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public Map<String, Object> getAccountLedgerData(String type, Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            if ("supplier".equalsIgnoreCase(type)) {
                Map<String, Object> summary = supplierAccountPaypalService.getSupplierReceivedSummary(id);
                // Also ensure account_ledger table has these entries synced
                syncSupplierLedgerFromSummary(id, summary);
                
                List<AccountLedger> dbEntries = accountLedgerRepository.findActiveBySupplierId("SUPPLIER", id);
                if (dbEntries != null && !dbEntries.isEmpty()) {
                    List<Map<String, Object>> dbItems = new ArrayList<>();
                    for (AccountLedger al : dbEntries) {
                        Map<String, Object> row = new HashMap<>();
                        row.put("bookingNo", al.getBookingNo());
                        row.put("accountDetail", al.getAccountDetail());
                        row.put("totalCost", al.getTotalCost());
                        row.put("paymentAmount", al.getPaymentAmount());
                        row.put("paymentStatus", al.getPaymentStatus());
                        row.put("statusCode", al.getStatusCode());

                        String eDateStr = "—";
                        Date d = al.getEntryDate() != null ? al.getEntryDate() : (al.getPaidDate() != null ? al.getPaidDate() : al.getModifiedDate());
                        if (d != null) {
                            eDateStr = new java.text.SimpleDateFormat("dd-MMM-yyyy").format(d);
                        }
                        row.put("entryDate", eDateStr);
                        row.put("paidDate", eDateStr);

                        String salesOrderNo = "—";
                        if (al.getBookingNo() != null && !al.getBookingNo().trim().isEmpty()) {
                            try {
                                List<com.erp.erpsoftware.entity.SalesOrder> sos = salesOrderRepository.findByBookingNo(al.getBookingNo().trim());
                                if (sos != null && !sos.isEmpty() && sos.get(0).getSalesOrderNo() != null) {
                                    salesOrderNo = sos.get(0).getSalesOrderNo();
                                }
                            } catch (Exception ignored) {}
                        }
                        row.put("salesOrderNo", salesOrderNo);
                        row.put("salesNo", salesOrderNo);

                        dbItems.add(row);
                    }
                    summary.put("items", dbItems);
                }

                response.put("type", "supplier");
                response.put("id", id);
                response.put("summary", summary);
            } else if ("vendor".equalsIgnoreCase(type)) {
                Map<String, Object> summary = accountPaypalService.getVendorFullyReceivedSummary(id);
                // Also ensure account_ledger table has these entries synced
                syncVendorLedgerFromSummary(id, summary);

                List<AccountLedger> dbEntries = accountLedgerRepository.findActiveByVendorId("VENDOR", id);
                if (dbEntries != null && !dbEntries.isEmpty()) {
                    List<Map<String, Object>> dbItems = new ArrayList<>();
                    for (AccountLedger al : dbEntries) {
                        Map<String, Object> row = new HashMap<>();
                        row.put("poNo", al.getBookingNo());
                        row.put("purchaseOrderNo", al.getBookingNo());
                        row.put("purchaseNo", al.getBookingNo());
                        row.put("bookingNo", al.getBookingNo());
                        row.put("accountDetail", al.getAccountDetail());
                        row.put("totalCost", al.getTotalCost());
                        row.put("paymentAmount", al.getPaymentAmount());
                        row.put("paymentStatus", al.getPaymentStatus());
                        row.put("statusCode", al.getStatusCode());

                        String eDateStr = "—";
                        Date d = al.getEntryDate() != null ? al.getEntryDate() : (al.getPaidDate() != null ? al.getPaidDate() : al.getModifiedDate());
                        if (d != null) {
                            eDateStr = new java.text.SimpleDateFormat("dd-MMM-yyyy").format(d);
                        }
                        row.put("entryDate", eDateStr);
                        row.put("paidDate", eDateStr);

                        dbItems.add(row);
                    }
                    summary.put("items", dbItems);
                }

                response.put("type", "vendor");
                response.put("id", id);
                response.put("summary", summary);
            } else {
                response.put("error", "Invalid type specified. Must be supplier or vendor.");
            }
        } catch (Exception e) {
            response.put("error", "Error loading account ledger data: " + e.getMessage());
        }
        return response;
    }

    public void recordSupplierLedgerEntry(Long supplierId, String bookingNo, String accountDetail, Double totalCost, Double paymentAmount, String paymentStatus, Integer statusCode) {
        if (bookingNo == null || bookingNo.trim().isEmpty()) return;
        String cleanBNo = bookingNo.trim();
        List<AccountLedger> existing = accountLedgerRepository.findByBookingNo(cleanBNo);
        AccountLedger entry;
        if (existing != null && !existing.isEmpty()) {
            entry = existing.get(0);
        } else {
            entry = new AccountLedger();
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

    public void recordVendorLedgerEntry(Long vendorId, String poNo, String accountDetail, Double totalCost, Double paymentAmount, String paymentStatus, Integer statusCode) {
        if (poNo == null || poNo.trim().isEmpty()) return;
        String cleanPoNo = poNo.trim();
        List<AccountLedger> existing = accountLedgerRepository.findByBookingNo(cleanPoNo);
        AccountLedger entry;
        if (existing != null && !existing.isEmpty()) {
            entry = existing.get(0);
        } else {
            entry = new AccountLedger();
            entry.setEntryDate(new Date());
        }
        entry.setEntityType("VENDOR");
        entry.setVendorId(vendorId);
        entry.setBookingNo(cleanPoNo);
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

    private void syncSupplierLedgerFromSummary(Long supplierId, Map<String, Object> summary) {
        if (summary == null) return;
        String sName = (String) summary.get("supplierName");
        List<Map<String, Object>> items = (List<Map<String, Object>>) summary.get("items");
        if (items != null) {
            for (Map<String, Object> item : items) {
                String bNo = (String) item.get("bookingNo");
                Double cost = item.get("totalCost") instanceof Number ? ((Number) item.get("totalCost")).doubleValue() : 0.0;
                Double paid = item.get("paymentAmount") instanceof Number ? ((Number) item.get("paymentAmount")).doubleValue() : (item.get("receivedAmount") instanceof Number ? ((Number) item.get("receivedAmount")).doubleValue() : 0.0);
                String status = (String) item.get("paymentStatus");
                Number codeNum = (Number) item.get("statusCode");
                Integer code = codeNum != null ? codeNum.intValue() : 1;
                recordSupplierLedgerEntry(supplierId, bNo, sName, cost, paid, status, code);
            }
        }
    }

    private void syncVendorLedgerFromSummary(Long vendorId, Map<String, Object> summary) {
        if (summary == null) return;
        String vName = (String) summary.get("vendorName");
        List<Map<String, Object>> items = (List<Map<String, Object>>) summary.get("items");
        if (items != null) {
            for (Map<String, Object> item : items) {
                String poNo = (String) item.get("poNo");
                if (poNo == null) poNo = (String) item.get("bookingNo");
                Double cost = item.get("totalCost") instanceof Number ? ((Number) item.get("totalCost")).doubleValue() : 0.0;
                Double paid = item.get("paymentAmount") instanceof Number ? ((Number) item.get("paymentAmount")).doubleValue() : (item.get("receivedAmount") instanceof Number ? ((Number) item.get("receivedAmount")).doubleValue() : 0.0);
                String status = (String) item.get("paymentStatus");
                Number codeNum = (Number) item.get("statusCode");
                Integer code = codeNum != null ? codeNum.intValue() : 1;
                recordVendorLedgerEntry(vendorId, poNo, vName, cost, paid, status, code);
            }
        }
    }
}

