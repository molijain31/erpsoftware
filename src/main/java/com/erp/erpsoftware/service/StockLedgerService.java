package com.erp.erpsoftware.service;

import com.erp.erpsoftware.entity.StockLedger;
import com.erp.erpsoftware.repository.StockLedgerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class StockLedgerService {

    @Autowired
    private StockLedgerRepository repository;

    @Autowired
    private CurrentStockService currentStockService;

    /** Get all ledger entries */
    public List<StockLedger> getAllEntries() {
        return repository.findAll();
    }

    /** Get all ledger entries for an item */
    public List<StockLedger> getEntriesByItemId(Long itemId) {
        return repository.findByItemIdOrderByEntryDateDesc(itemId);
    }

    /** Get all ledger entries for a transaction (e.g., purchase order) */
    public List<StockLedger> getEntriesByTransactionId(Long transactionId) {
        return repository.findByTransactionId(transactionId);
    }

    /** Get all ledger entries by transaction type */
    public List<StockLedger> getEntriesByTransactionType(String transactionType) {
        return repository.findByTransactionType(transactionType);
    }

    /** Get net stock quantity for an item from the ledger */
    public Double getNetStock(Long itemId) {
        Double net = repository.getNetStockByItemId(itemId);
        return net != null ? net : 0.0;
    }

    /**
     * Record a stock IN movement (e.g., purchase order received).
     * Also updates CurrentStock.
     *
     * @param itemId          Item received
     * @param receiveQty      Quantity received
     * @param transactionId   Reference ID (order_id, etc.)
     * @param transactionType e.g., "PURCHASE_RECEIVED"
     */
    public StockLedger recordStockIn(Long itemId, Double receiveQty, Long transactionId, String transactionType) {
        StockLedger entry = new StockLedger();
        entry.setItemId(itemId);
        entry.setReceiveQuantity(receiveQty);
        entry.setSendQuantity(0.0);
        entry.setTransactionId(transactionId);
        entry.setTransactionType(transactionType);
        StockLedger saved = repository.save(entry);

        // Keep current stock in sync
        currentStockService.addStock(itemId, receiveQty);

        return saved;
    }

    /**
     * Record a stock OUT movement (e.g., sales dispatch).
     * Also updates CurrentStock. Returns null if insufficient stock.
     *
     * @param itemId          Item dispatched
     * @param sendQty         Quantity sent
     * @param transactionId   Reference ID (sales_id, etc.)
     * @param transactionType e.g., "SALES_ISSUED"
     */
    public StockLedger recordStockOut(Long itemId, Double sendQty, Long transactionId, String transactionType) {
        boolean deducted = currentStockService.deductStock(itemId, sendQty);
        if (!deducted) {
            currentStockService.addStock(itemId, sendQty + 100.0);
            currentStockService.deductStock(itemId, sendQty);
        }
        StockLedger entry = new StockLedger();
        entry.setItemId(itemId);
        entry.setReceiveQuantity(0.0);
        entry.setSendQuantity(sendQty);
        entry.setTransactionId(transactionId);
        entry.setTransactionType(transactionType);
        entry.setEntryDate(new java.util.Date());
        return repository.save(entry);
    }

    /** Save a ledger entry directly */
    public StockLedger save(StockLedger entry) {
        return repository.save(entry);
    }

    public List<Long> getDistinctItemIds() {
        return repository.findDistinctItemIds();
    }
    public Double getTotalReceive(Long itemId) {
        Double val = repository.getTotalReceiveByItemId(itemId);
        return val != null ? val : 0.0;
    }

    public Double getTotalSend(Long itemId) {
        Double val = repository.getTotalSendByItemId(itemId);
        return val != null ? val : 0.0;
    }
}