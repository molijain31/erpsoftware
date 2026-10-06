package com.erp.erpsoftware.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

@Data
@Entity
@Table(name = "purchase_order_mapping", schema = "erp")
@IdClass(PurchaseOrderMappingId.class)
public class PurchaseOrderDetail implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "order_id")
    private Long orderId;

    @Id
    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "vendor_id")
    private Long vendorId;

    @Column(name = "vendor_name", length = 255)
    private String vendorName;

    @Transient
    private Integer slNo;

    @Column(name = "is_valid")
    private Integer isValid = 1;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "rate", nullable = false)
    private Double rate;

    @Column(name = "rate_cost")
    private Double rateCost;

    @Column(name = "total_cost", nullable = false)
    private Double totalCost;

    /**
     * Status of this detail line: 1 = Created (default) 2 = Partially Received
     * 3 = Fully Received
     */
    @Column(name = "status")
    private Integer status = 1;

    @Column(name = "received_quantity")
    private Integer receivedQuantity = 0;

    @Column(name = "receive_order_status", length = 50)
    private String receiveOrderStatus = "Pending";

    @Column(name = "entry_date", updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date entryDate;

    @Column(name = "modified_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date modifiedDate;

    @Transient
    private Item item;

    @PrePersist
    protected void onCreate() {
        entryDate = new Date();
        modifiedDate = new Date();
        if (status == null) {
            status = 1;
        }
        if (receivedQuantity == null) {
            receivedQuantity = 0;
        }
        if (receiveOrderStatus == null || receiveOrderStatus.isEmpty()) {
            receiveOrderStatus = "Pending";
        }
        // rateCost defaults to rate if not set
        if (rateCost == null && rate != null) {
            rateCost = rate;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        modifiedDate = new Date();
        // Derive status from receivedQuantity vs quantity
        if (receivedQuantity != null && quantity != null && quantity > 0) {
            if (receivedQuantity == 0) {
                status = 1;
            } else if (receivedQuantity < quantity) {
                status = 2;
            } else {
                status = 3;
            }
        }
    }

    // Explicit Getters and Setters
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }

    public Long getVendorId() { return vendorId; }
    public void setVendorId(Long vendorId) { this.vendorId = vendorId; }

    public String getVendorName() { return vendorName; }
    public void setVendorName(String vendorName) { this.vendorName = vendorName; }

    public Integer getSlNo() { return slNo; }
    public void setSlNo(Integer slNo) { this.slNo = slNo; }

    public Integer getIsValid() { return isValid; }
    public void setIsValid(Integer isValid) { this.isValid = isValid; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Double getRate() { return rate; }
    public void setRate(Double rate) { this.rate = rate; }

    public Double getRateCost() { return rateCost; }
    public void setRateCost(Double rateCost) { this.rateCost = rateCost; }

    public Double getTotalCost() { return totalCost; }
    public void setTotalCost(Double totalCost) { this.totalCost = totalCost; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public Integer getReceivedQuantity() { return receivedQuantity; }
    public void setReceivedQuantity(Integer receivedQuantity) { this.receivedQuantity = receivedQuantity; }

    public String getReceiveOrderStatus() { return receiveOrderStatus; }
    public void setReceiveOrderStatus(String receiveOrderStatus) { this.receiveOrderStatus = receiveOrderStatus; }

    public Date getEntryDate() { return entryDate; }
    public void setEntryDate(Date entryDate) { this.entryDate = entryDate; }

    public Date getModifiedDate() { return modifiedDate; }
    public void setModifiedDate(Date modifiedDate) { this.modifiedDate = modifiedDate; }

    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }
}
