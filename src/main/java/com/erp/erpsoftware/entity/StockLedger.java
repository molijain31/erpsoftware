package com.erp.erpsoftware.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.Date;

/**
 * Stock Ledger — records every stock movement (IN or OUT).
 * transactionType: "IN" = purchase received, "OUT" = issued/dispatched
 */
@Data
@Entity
@Table(name = "stock_ledger", schema = "erp")
@SuppressWarnings("JpaDataSourceORMInspection")
public class StockLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stock_ledger_id")
    private Long stockLedgerId;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    /** Quantity received (IN movement). 0 if this is an OUT transaction. */
    @Column(name = "receive_quantity")
    private Double receiveQuantity = 0.0;

    /** Quantity sent/issued (OUT movement). 0 if this is an IN transaction. */
    @Column(name = "send_quantity")
    private Double sendQuantity = 0.0;

    /**
     * Reference ID of the source transaction:
     * e.g., order_id for purchase receipts, sales_id for dispatch.
     */
    @Column(name = "transaction_id")
    private Long transactionId;

    /** Type of transaction: PURCHASE_RECEIVED, SALES_ISSUED, BOOKING_REACHED, etc. */
    @Column(name = "transaction_type", length = 100, nullable = false)
    private String transactionType;

    /** Stock balance after this transaction entry */
    @Column(name = "balance")
    private Double balance = 0.0;

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
        if (receiveQuantity == null) receiveQuantity = 0.0;
        if (sendQuantity == null) sendQuantity = 0.0;
        if (balance == null) balance = 0.0;
    }

    @PreUpdate
    protected void onUpdate() {
        modifiedDate = new Date();
    }

    // Explicit Getters and Setters
    public Long getStockLedgerId() { return stockLedgerId; }
    public void setStockLedgerId(Long stockLedgerId) { this.stockLedgerId = stockLedgerId; }

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }

    public Double getReceiveQuantity() { return receiveQuantity; }
    public void setReceiveQuantity(Double receiveQuantity) { this.receiveQuantity = receiveQuantity; }

    public Double getSendQuantity() { return sendQuantity; }
    public void setSendQuantity(Double sendQuantity) { this.sendQuantity = sendQuantity; }

    public Long getTransactionId() { return transactionId; }
    public void setTransactionId(Long transactionId) { this.transactionId = transactionId; }

    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }

    public Double getBalance() { return balance; }
    public void setBalance(Double balance) { this.balance = balance; }

    public Date getEntryDate() { return entryDate; }
    public void setEntryDate(Date entryDate) { this.entryDate = entryDate; }

    public Date getModifiedDate() { return modifiedDate; }
    public void setModifiedDate(Date modifiedDate) { this.modifiedDate = modifiedDate; }
}
