package com.erp.erpsoftware.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.ArrayList;

@Data
@Entity
@Table(name = "booking_request", schema = "erp")
@SuppressWarnings("JpaDataSourceORMInspection")
public class BookingRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "booking_id")
    private Long bookingId;

    @Column(name = "booking_no")
    private String bookingNo;

    @Column(name = "total_quantity")
    private Integer totalQuantity;

    @Column(name = "total_cost")
    private Double totalCost;

    @Column(name = "status")
    private String status = "Pending";

    @Column(name = "entry_date")
    private Date entryDate;

    @Column(name = "modified_date")
    private Date modifiedDate;

    @Column(name = "supplier_id")
    private String supplierId;


    @Transient
    private String paymentStatus = "1 - Unpaid";

    @Transient
    private Double paymentAmount = 0.0;

    @Transient
    private List<BookingRequestItemMapping> items = new ArrayList<>();

    public String getSupplierName() {
        return "";
    }

    // Explicit Getters and Setters
    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }

    public String getBookingNo() { return bookingNo; }
    public void setBookingNo(String bookingNo) { this.bookingNo = bookingNo; }

    public Integer getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(Integer totalQuantity) { this.totalQuantity = totalQuantity; }

    public Double getTotalCost() { return totalCost; }
    public void setTotalCost(Double totalCost) { this.totalCost = totalCost; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Date getEntryDate() { return entryDate; }
    public void setEntryDate(Date entryDate) { this.entryDate = entryDate; }

    public Date getModifiedDate() { return modifiedDate; }
    public void setModifiedDate(Date modifiedDate) { this.modifiedDate = modifiedDate; }

    public String getSupplierId() { return supplierId; }
    public void setSupplierId(String supplierId) { this.supplierId = supplierId; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public Double getPaymentAmount() { return paymentAmount; }
    public void setPaymentAmount(Double paymentAmount) { this.paymentAmount = paymentAmount; }

    public List<BookingRequestItemMapping> getItems() { return items; }
    public void setItems(List<BookingRequestItemMapping> items) { this.items = items; }
}