package com.erp.erpsoftware.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;

@Data
@Entity
@Table(name = "purchase_order", schema = "erp")
@SuppressWarnings("JpaDataSourceORMInspection")
public class PurchaseOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "purchase_order_no", length = 20)
    private String purchaseOrderNo;

    @Column(name = "vendor_id", nullable = false)
    private Long vendorId;

    @Column(name = "total_cost")
    private Double totalCost;

    /**
     * Status of the purchase order:
     * 1 = Created (default)
     * 2 = Partially Received
     * 3 = Fully Received
     */
    @Column(name = "status")
    private Integer status = 1;

    @Column(name = "payment_status", length = 50)
    private String paymentStatus = "Unpaid";

    @Column(name = "payment_amount")
    private Double paymentAmount = 0.0;

    @Column(name = "entry_date", updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date entryDate;

    @Column(name = "modified_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date modifiedDate;

    // ── Transient (not persisted) ────────────────────────────

    @Transient
    private String vendorName;

    @Transient
    private String transactionAccount;

    @Column(name = "total_quantity")
    private Integer totalQuantity;

    @Transient
    private List<PurchaseOrderMapping> details = new ArrayList<>();

    // ── Lifecycle hooks ──────────────────────────────────────

    @PrePersist
    protected void onCreate() {
        entryDate = new Date();
        modifiedDate = new Date();
        if (status == null) status = 1;
        if (paymentStatus == null || paymentStatus.isEmpty()) paymentStatus = "Unpaid";
        if (paymentAmount == null) paymentAmount = 0.0;
        if (purchaseOrderNo == null || purchaseOrderNo.isEmpty()) {
            int seed = (int)(System.currentTimeMillis() % 90000) + 10000;
            purchaseOrderNo = String.format("PO-%05d", seed);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        modifiedDate = new Date();
    }

    public String getPurchaseOrderNo() {
        if (purchaseOrderNo == null || purchaseOrderNo.isEmpty()) {
            return orderId != null ? "PO-" + String.format("%05d", orderId) : "";
        }
        return purchaseOrderNo;
    }

    public void setPurchaseOrderNo(String purchaseOrderNo) {
        this.purchaseOrderNo = purchaseOrderNo;
    }

    public String getPaymentStatus() {
        if (paymentStatus == null || paymentStatus.trim().isEmpty()) {
            return "Unpaid";
        }
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    // Explicit Getters and Setters
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public Long getVendorId() { return vendorId; }
    public void setVendorId(Long vendorId) { this.vendorId = vendorId; }

    public Double getTotalCost() { return totalCost; }
    public void setTotalCost(Double totalCost) { this.totalCost = totalCost; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public Double getPaymentAmount() { return paymentAmount; }
    public void setPaymentAmount(Double paymentAmount) { this.paymentAmount = paymentAmount; }

    public Date getEntryDate() { return entryDate; }
    public void setEntryDate(Date entryDate) { this.entryDate = entryDate; }

    public Date getModifiedDate() { return modifiedDate; }
    public void setModifiedDate(Date modifiedDate) { this.modifiedDate = modifiedDate; }

    public String getVendorName() { return vendorName; }
    public void setVendorName(String vendorName) { this.vendorName = vendorName; }

    public String getTransactionAccount() { return transactionAccount; }
    public void setTransactionAccount(String transactionAccount) { this.transactionAccount = transactionAccount; }

    public Integer getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(Integer totalQuantity) { this.totalQuantity = totalQuantity; }

    public List<PurchaseOrderMapping> getDetails() { return details; }
    public void setDetails(List<PurchaseOrderMapping> details) { this.details = details; }
}
