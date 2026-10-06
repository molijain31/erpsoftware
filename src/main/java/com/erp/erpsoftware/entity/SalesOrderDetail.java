package com.erp.erpsoftware.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

@Data
@Entity
@Table(name = "sales_order_mapping", schema = "erp")
@IdClass(SalesOrderDetailId.class)
public class SalesOrderDetail implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "saleorder_id")
    private Long orderId;

    @Id
    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "is_valid")
    private Integer isValid = 1;

    @Column(name = "quantity")
    private Integer quantity; // Booked quantity

    @Column(name = "rate")
    private Double rate;

    @Column(name = "total_cost")
    private Double totalCost;

    /**
     * Status of this detail line:
     * 1 = Pending (default)
     * 2 = Partially Delivered
     * 3 = Delivered
     */
    @Column(name = "delivery_order_status")
    private String status = "1";

    @Column(name = "delivered_quantity")
    private Integer deliveredQuantity = 0; // Sent quantity

    @Column(name = "actual_delivered_quantity")
    private Integer actualDeliveredQuantity = 0; // Actual delivered/received quantity at destination

    @Column(name = "damaged_quantity")
    private Integer damagedQuantity = 0;

    @Column(name = "returned_quantity")
    private Integer returnedQuantity = 0;

    @Column(name = "returned_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date returnedDate;

    public Integer getActualDeliveredQuantity() {
        return actualDeliveredQuantity != null ? actualDeliveredQuantity : (deliveredQuantity != null ? deliveredQuantity : 0);
    }

    public void setActualDeliveredQuantity(Integer actualDeliveredQuantity) {
        this.actualDeliveredQuantity = actualDeliveredQuantity != null ? actualDeliveredQuantity : 0;
    }

    public Integer getDamagedQuantity() {
        return damagedQuantity != null ? damagedQuantity : 0;
    }

    public void setDamagedQuantity(Integer damagedQuantity) {
        this.damagedQuantity = damagedQuantity != null ? damagedQuantity : 0;
    }

    public Integer getReturnedQuantity() {
        return returnedQuantity != null ? returnedQuantity : 0;
    }

    public void setReturnedQuantity(Integer returnedQuantity) {
        this.returnedQuantity = returnedQuantity != null ? returnedQuantity : 0;
    }

    public void setStatus(Integer status) {
        this.status = status != null ? String.valueOf(status) : "1";
    }

    public void setStatus(String status) {
        this.status = status != null ? status : "1";
    }

    public Integer getDeliveryOrderStatus() {
        return getStatus();
    }

    public void setDeliveryOrderStatus(Integer deliveryOrderStatus) {
        this.status = deliveryOrderStatus != null ? String.valueOf(deliveryOrderStatus) : "1";
    }

    @Column(name = "entry_date", updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date entryDate;

    @Column(name = "modified_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date modifiedDate;

    @Transient
    private Item item;

    @Transient
    private Integer slNo;

    public Integer getPendingQuantity() {
        int qty = quantity != null ? quantity : 0;
        int dQty = deliveredQuantity != null ? deliveredQuantity : 0;
        return Math.max(0, qty - dQty);
    }

    public Integer getStatus() {
        if (deliveredQuantity != null && quantity != null && quantity > 0) {
            if (deliveredQuantity == 0) return 1;
            if (deliveredQuantity < quantity) return 2;
            return 3;
        }
        if (status != null) {
            try {
                return Integer.parseInt(status);
            } catch (Exception ignored) {}
        }
        return 1;
    }

    @PrePersist
    protected void onCreate() {
        entryDate = new Date();
        modifiedDate = new Date();
        if (status == null) status = "1";
        if (deliveredQuantity == null) deliveredQuantity = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        modifiedDate = new Date();
        if (deliveredQuantity != null && quantity != null && quantity > 0) {
            if (deliveredQuantity == 0) {
                status = "1";
            } else if (deliveredQuantity < quantity) {
                status = "2";
            } else {
                status = "3";
            }
        }
    }

    // Explicit Getters and Setters
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }

    public Integer getIsValid() { return isValid; }
    public void setIsValid(Integer isValid) { this.isValid = isValid; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Double getRate() { return rate; }
    public void setRate(Double rate) { this.rate = rate; }

    public Double getTotalCost() { return totalCost; }
    public void setTotalCost(Double totalCost) { this.totalCost = totalCost; }

    public Integer getDeliveredQuantity() { return deliveredQuantity; }
    public void setDeliveredQuantity(Integer deliveredQuantity) { this.deliveredQuantity = deliveredQuantity; }

    public Date getReturnedDate() { return returnedDate; }
    public void setReturnedDate(Date returnedDate) { this.returnedDate = returnedDate; }

    public Date getEntryDate() { return entryDate; }
    public void setEntryDate(Date entryDate) { this.entryDate = entryDate; }

    public Date getModifiedDate() { return modifiedDate; }
    public void setModifiedDate(Date modifiedDate) { this.modifiedDate = modifiedDate; }

    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }

    public Integer getSlNo() { return slNo; }
    public void setSlNo(Integer slNo) { this.slNo = slNo; }
}
