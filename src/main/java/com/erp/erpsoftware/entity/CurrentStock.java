package com.erp.erpsoftware.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.Date;

@Data
@Entity
@Table(name = "current_stock", schema = "erp")
@SuppressWarnings("JpaDataSourceORMInspection")
public class CurrentStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stock_id")
    private Integer stockId;

    @Column(name = "item_id")
    private Long itemId;

    @Transient
    private Item item;

    @Column(name = "current_stock")
    private Double currentStock;

    @Column(name = "entry_date", updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date entryDate;

    @Column(name = "modified_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date modifiedDate;

    @PrePersist
    protected void onCreate() {
        entryDate = new Date();
        modifiedDate = new Date();
        if (currentStock == null) currentStock = 0.0;
    }

    @PreUpdate
    protected void onUpdate() {
        modifiedDate = new Date();
    }

    // Explicit Getters and Setters
    public Integer getStockId() { return stockId; }
    public void setStockId(Integer stockId) { this.stockId = stockId; }

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }

    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }

    public Double getCurrentStock() { return currentStock; }
    public void setCurrentStock(Double currentStock) { this.currentStock = currentStock; }

    public Date getEntryDate() { return entryDate; }
    public void setEntryDate(Date entryDate) { this.entryDate = entryDate; }

    public Date getModifiedDate() { return modifiedDate; }
    public void setModifiedDate(Date modifiedDate) { this.modifiedDate = modifiedDate; }
}