package com.erp.erpsoftware.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Data
@Entity
@IdClass(BookingRequestItemMappingId.class)
@Table(name = "booking_request_item_mapping", schema = "erp")
public class BookingRequestItemMapping {

    @Id
    @Column(name = "booking_no")
    private String bookingNo;

    @Id
    @Column(name = "item_id")
    private Integer itemId;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "total_cost")
    private Double totalCost;

    @Column(name = "entry_date")
    private Date entryDate;

    @Column(name = "modified_date")
    private Date modifiedDate;

    @Column(name = "status")
    private String status = "Pending";

    @Transient
    private BookingRequest bookingRequest;

    @Transient
    private Item item;

    // Explicit Getters and Setters
    public String getBookingNo() { return bookingNo; }
    public void setBookingNo(String bookingNo) { this.bookingNo = bookingNo; }

    public Integer getItemId() { return itemId; }
    public void setItemId(Integer itemId) { this.itemId = itemId; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Double getTotalCost() { return totalCost; }
    public void setTotalCost(Double totalCost) { this.totalCost = totalCost; }

    public Date getEntryDate() { return entryDate; }
    public void setEntryDate(Date entryDate) { this.entryDate = entryDate; }

    public Date getModifiedDate() { return modifiedDate; }
    public void setModifiedDate(Date modifiedDate) { this.modifiedDate = modifiedDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BookingRequest getBookingRequest() { return bookingRequest; }
    public void setBookingRequest(BookingRequest bookingRequest) { this.bookingRequest = bookingRequest; }

    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }
}
