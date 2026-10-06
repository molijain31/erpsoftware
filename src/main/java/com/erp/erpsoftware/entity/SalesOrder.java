package com.erp.erpsoftware.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;

@Data
@Entity
@Table(name = "sales_order", schema = "erp")
@SuppressWarnings("JpaDataSourceORMInspection")
public class SalesOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "salesorder_id")
    private Long orderId;

    @Column(name = "sales_order_no", length = 20)
    private String salesOrderNo;

    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    @Column(name = "total_cost")
    private Double totalCost;

    @Column(name = "total_quantity")
    private Integer totalQuantity;

    /**
     * Status of the sales order:
     * 1 = Pending (default)
     * 2 = Partially Delivered
     * 3 = Delivered
     */
    @Column(name = "status")
    private Integer status = 1;

    /** Numeric reference to booking_request.request_id */
    @Column(name = "booking_id")
    private String bookingId;

    /** The original BR-... booking number string */
    @Column(name = "booking_no")
    private String bookingNo;

    @Column(name = "received_amount")
    private Double receivedAmount;

    @Column(name = "payment_status", length = 50)
    private String paymentStatus;

    @Column(name = "entry_date", updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date entryDate;

    @Column(name = "modified_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date modifiedDate;

    // ── Transient (not persisted) ────────────────────────────

    @Transient
    private String supplierName;

    @Transient
    private List<SalesOrderDetail> details = new ArrayList<>();

    // ── Lifecycle hooks ──────────────────────────────────────

    @PrePersist
    protected void onCreate() {
        if (entryDate == null) entryDate = new Date();
        modifiedDate = new Date();
        if (status == null) status = 1;
        if (receivedAmount == null) receivedAmount = 0.0;
        if (paymentStatus == null || paymentStatus.trim().isEmpty()) paymentStatus = "1 - Unpaid";
        if (salesOrderNo == null || salesOrderNo.trim().isEmpty() || salesOrderNo.startsWith("SO-")) {
            int seed = (int)(System.currentTimeMillis() % 90000) + (int)(Math.random() * 9000) + 10000;
            salesOrderNo = String.format("CO-%05d", seed);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        modifiedDate = new Date();
        if (receivedAmount == null) receivedAmount = 0.0;
        if (paymentStatus == null || paymentStatus.trim().isEmpty()) paymentStatus = "1 - Unpaid";
    }

    public String getSalesOrderNo() {
        if (salesOrderNo == null || salesOrderNo.trim().isEmpty() || salesOrderNo.startsWith("SO-")) {
            return orderId != null ? String.format("CO-%05d", orderId) : "";
        }
        return salesOrderNo;
    }

    public void setSalesOrderNo(String salesOrderNo) {
        this.salesOrderNo = salesOrderNo;
    }

    // Explicit Getters and Setters
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }

    public Double getTotalCost() { return totalCost; }
    public void setTotalCost(Double totalCost) { this.totalCost = totalCost; }

    public Integer getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(Integer totalQuantity) { this.totalQuantity = totalQuantity; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public String getBookingNo() { return bookingNo; }
    public void setBookingNo(String bookingNo) { this.bookingNo = bookingNo; }

    public Double getReceivedAmount() { return receivedAmount; }
    public void setReceivedAmount(Double receivedAmount) { this.receivedAmount = receivedAmount; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public Date getEntryDate() { return entryDate; }
    public void setEntryDate(Date entryDate) { this.entryDate = entryDate; }

    public Date getModifiedDate() { return modifiedDate; }
    public void setModifiedDate(Date modifiedDate) { this.modifiedDate = modifiedDate; }

    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

    public List<SalesOrderDetail> getDetails() { return details; }
    public void setDetails(List<SalesOrderDetail> details) { this.details = details; }
}
