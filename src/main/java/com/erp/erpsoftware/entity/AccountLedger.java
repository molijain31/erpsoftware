package com.erp.erpsoftware.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.Date;

/**
 * Account Ledger Entity — records payment transactions and financial ledger line items
 * for Suppliers and Vendors.
 */
@Data
@Entity
@Table(name = "account_ledger", schema = "erp")
@SuppressWarnings("JpaDataSourceORMInspection")
public class AccountLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ledger_id")
    private Long ledgerId;

    /** Type of account: "SUPPLIER" or "VENDOR" */
    @Column(name = "entity_type", length = 20, nullable = false)
    private String entityType;

    @Column(name = "supplier_id")
    private Long supplierId;

    @Column(name = "vendor_id")
    private Long vendorId;

    /** Reference number, e.g. BR-03-1449-557 or PO-0001 */
    @Column(name = "booking_no", length = 100)
    private String bookingNo;

    /** Detail description, e.g. Supplier Name or Vendor Name or item details */
    @Column(name = "account_detail", length = 255)
    private String accountDetail;

    /** Total cost / payment asked amount (₹) */
    @Column(name = "total_cost")
    private Double totalCost = 0.0;

    /** Received or sent payment amount (₹) */
    @Column(name = "payment_amount")
    private Double paymentAmount = 0.0;

    /** Payment status string, e.g. "3 - Full Payment", "2 - Partially Paid", "1 - Unpaid" */
    @Column(name = "payment_status", length = 50)
    private String paymentStatus = "1 - Unpaid";

    /** Status code: 3 = Full Payment, 2 = Partially Paid, 1 = Unpaid */
    @Column(name = "status_code")
    private Integer statusCode = 1;

    /** Active flag: 1 = Active, 0 = Deleted */
    @Column(name = "is_valid")
    private Integer isValid = 1;

    @Column(name = "paid_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date paidDate;

    @Column(name = "entry_date", updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date entryDate;

    @Column(name = "modified_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date modifiedDate;

    @PrePersist
    protected void onCreate() {
        if (entryDate == null) entryDate = new Date();
        if (modifiedDate == null) modifiedDate = new Date();
        if (isValid == null) isValid = 1;
        if (totalCost == null) totalCost = 0.0;
        if (paymentAmount == null) paymentAmount = 0.0;
        if (statusCode == null) statusCode = 1;
        if (paymentStatus == null || paymentStatus.isEmpty()) paymentStatus = "1 - Unpaid";
    }

    @PreUpdate
    protected void onUpdate() {
        modifiedDate = new Date();
    }

    // Explicit Getters and Setters
    public Long getLedgerId() { return ledgerId; }
    public void setLedgerId(Long ledgerId) { this.ledgerId = ledgerId; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }

    public Long getVendorId() { return vendorId; }
    public void setVendorId(Long vendorId) { this.vendorId = vendorId; }

    public String getBookingNo() { return bookingNo; }
    public void setBookingNo(String bookingNo) { this.bookingNo = bookingNo; }

    public String getAccountDetail() { return accountDetail; }
    public void setAccountDetail(String accountDetail) { this.accountDetail = accountDetail; }

    public Double getTotalCost() { return totalCost; }
    public void setTotalCost(Double totalCost) { this.totalCost = totalCost; }

    public Double getPaymentAmount() { return paymentAmount; }
    public void setPaymentAmount(Double paymentAmount) { this.paymentAmount = paymentAmount; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public Integer getStatusCode() { return statusCode; }
    public void setStatusCode(Integer statusCode) { this.statusCode = statusCode; }

    public Integer getIsValid() { return isValid; }
    public void setIsValid(Integer isValid) { this.isValid = isValid; }

    public Date getPaidDate() { return paidDate; }
    public void setPaidDate(Date paidDate) { this.paidDate = paidDate; }

    public Date getEntryDate() { return entryDate; }
    public void setEntryDate(Date entryDate) { this.entryDate = entryDate; }

    public Date getModifiedDate() { return modifiedDate; }
    public void setModifiedDate(Date modifiedDate) { this.modifiedDate = modifiedDate; }
}
