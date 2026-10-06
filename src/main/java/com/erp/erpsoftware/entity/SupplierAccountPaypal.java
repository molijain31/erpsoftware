package com.erp.erpsoftware.entity;

import jakarta.persistence.*;

import java.util.Date;

@Entity
@Table(name = "account_paypal", schema = "erp")
@SuppressWarnings("JpaDataSourceORMInspection")
public class SupplierAccountPaypal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "account_name", nullable = false, length = 100)
    private String accountName;

    @Column(name = "payment_mode", length = 50)
    private String paymentMode = "UPI";

    @Column(name = "cheque_book", length = 100)
    private String chequeBook;

    @Column(name = "neft_no", length = 100)
    private String neftNo;

    @Column(name = "rtgs_no", length = 100)
    private String rtgsNo;

    @Column(name = "amount_received")
    private Double amountReceived = 0.0;

    @Transient
    private String paypalEmail = "";

    @Column(name = "account_type", length = 50)
    private String accountType = "Business";

    @Column(name = "transaction_type", length = 50)
    private String transactionType = "Receive";

    @Column(name = "currency", length = 10)
    private String currency = "INR";

    @Column(name = "balance")
    private Double balance = 0.0;

    @Column(name = "supplier_id")
    private Long supplierId;

    @Column(name = "remarks", length = 255)
    private String remarks;

    @Column(name = "is_valid")
    private Integer isValid = 1;

    @Column(name = "entry_date", updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date entryDate;

    @Column(name = "modified_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date modifiedDate;

    @Transient
    private String supplierName;

    @Transient
    private Integer fullyReceivedQty = 0;

    @Transient
    private Double fullyReceivedCost = 0.0;

    @Transient
    private Boolean isFullyPaid = false;

    @Transient
    private String purchaseOrderNo;

    @Transient
    private Double lastTransactionPaid = 0.0;

    @Transient
    private Double pendingAmount = 0.0;

    @Transient
    private String coLinks;

    @PrePersist
    protected void onCreate() {
        entryDate = new Date();
        modifiedDate = new Date();
        if (isValid == null) isValid = 1;
        if (amountReceived == null) amountReceived = 0.0;
        if (balance == null) balance = amountReceived;
        if (paymentMode == null || paymentMode.isEmpty()) paymentMode = "UPI";
        if (paypalEmail == null) paypalEmail = "";
        if (accountName == null || accountName.trim().isEmpty()) {
            accountName = (supplierName != null && !supplierName.trim().isEmpty()) ? supplierName : "Supplier Account";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        modifiedDate = new Date();
        if (balance == null) balance = amountReceived;
        if (accountName == null || accountName.trim().isEmpty()) {
            accountName = (supplierName != null && !supplierName.trim().isEmpty()) ? supplierName : "Supplier Account";
        }
    }

    // Explicit Getters and Setters
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public String getAccountName() { return accountName; }
    public void setAccountName(String accountName) { this.accountName = accountName; }

    public String getPaymentMode() { return paymentMode; }
    public void setPaymentMode(String paymentMode) { this.paymentMode = paymentMode; }

    public String getChequeBook() { return chequeBook; }
    public void setChequeBook(String chequeBook) { this.chequeBook = chequeBook; }

    public String getNeftNo() { return neftNo; }
    public void setNeftNo(String neftNo) { this.neftNo = neftNo; }

    public String getRtgsNo() { return rtgsNo; }
    public void setRtgsNo(String rtgsNo) { this.rtgsNo = rtgsNo; }

    public Double getAmountReceived() { return amountReceived; }
    public void setAmountReceived(Double amountReceived) { this.amountReceived = amountReceived; }

    public String getPaypalEmail() { return paypalEmail; }
    public void setPaypalEmail(String paypalEmail) { this.paypalEmail = paypalEmail; }

    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }

    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public Double getBalance() { return balance; }
    public void setBalance(Double balance) { this.balance = balance; }

    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }

    @Transient
    private String coOrderNo;

    @Transient
    private java.util.List<java.util.Map<String, String>> coOrderList = new java.util.ArrayList<>();

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public Integer getIsValid() { return isValid; }
    public void setIsValid(Integer isValid) { this.isValid = isValid; }

    public Date getEntryDate() { return entryDate; }
    public void setEntryDate(Date entryDate) { this.entryDate = entryDate; }

    public Date getModifiedDate() { return modifiedDate; }
    public void setModifiedDate(Date modifiedDate) { this.modifiedDate = modifiedDate; }

    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

    public Integer getFullyReceivedQty() { return fullyReceivedQty; }
    public void setFullyReceivedQty(Integer fullyReceivedQty) { this.fullyReceivedQty = fullyReceivedQty; }

    public Double getFullyReceivedCost() { return fullyReceivedCost; }
    public void setFullyReceivedCost(Double fullyReceivedCost) { this.fullyReceivedCost = fullyReceivedCost; }

    public Boolean getIsFullyPaid() { return isFullyPaid; }
    public void setIsFullyPaid(Boolean isFullyPaid) { this.isFullyPaid = isFullyPaid; }

    public String getPurchaseOrderNo() { return purchaseOrderNo; }
    public void setPurchaseOrderNo(String purchaseOrderNo) { this.purchaseOrderNo = purchaseOrderNo; }

    public String getCoOrderNo() { return coOrderNo; }
    public void setCoOrderNo(String coOrderNo) { this.coOrderNo = coOrderNo; }

    public java.util.List<java.util.Map<String, String>> getCoOrderList() { return coOrderList; }
    public void setCoOrderList(java.util.List<java.util.Map<String, String>> coOrderList) { this.coOrderList = coOrderList; }

    public Double getLastTransactionPaid() { return lastTransactionPaid; }
    public void setLastTransactionPaid(Double lastTransactionPaid) { this.lastTransactionPaid = lastTransactionPaid; }

    public Double getPendingAmount() { return pendingAmount; }
    public void setPendingAmount(Double pendingAmount) { this.pendingAmount = pendingAmount; }

    public String getCoLinks() { return coLinks; }
    public void setCoLinks(String coLinks) { this.coLinks = coLinks; }
}
