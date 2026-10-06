package com.erp.erpsoftware.service;

import com.erp.erpsoftware.entity.CurrentStock;
import com.erp.erpsoftware.entity.Item;
import com.erp.erpsoftware.entity.StockLedger;
import com.erp.erpsoftware.repository.CurrentStockRepository;
import com.erp.erpsoftware.repository.StockLedgerRepository;
import com.erp.erpsoftware.repository.ItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class CurrentStockService {

    @Autowired
    private CurrentStockRepository repository;

    @Autowired
    private StockLedgerRepository ledgerRepository;

    @Autowired
    private ItemRepository itemRepository;

    /** Get all current stock records */
    public List<CurrentStock> getAllStocks() {
        List<CurrentStock> stocks = repository.findAll();
        if (stocks != null && !stocks.isEmpty()) {
            List<Item> items = itemRepository.findAll();
            Map<Long, Item> itemMap = items.stream()
                    .collect(Collectors.toMap(Item::getItemId, item -> item, (k1, k2) -> k1));
            for (CurrentStock stock : stocks) {
                if (stock.getItemId() != null) {
                    stock.setItem(itemMap.get(stock.getItemId()));
                }
            }
        }
        return stocks;
    }

    /** Get stock for a specific item */
    public CurrentStock getStockByItemId(Long itemId) {
        CurrentStock stock = repository.findByItemId(itemId);
        if (stock != null) {
            if (stock.getCurrentStock() == null || stock.getCurrentStock() < 0) {
                stock.setCurrentStock(0.0);
                repository.save(stock);
            }
            if (stock.getItemId() != null) {
                itemRepository.findById(stock.getItemId()).ifPresent(stock::setItem);
            }
        }
        return stock;
    }

    /**
     * ADD stock — upsert by itemId.
     * Creates a new record if none exists.
     */
    public CurrentStock addStock(Long itemId, Double receivedQty) {
        CurrentStock stock = repository.findByItemId(itemId);
        if (stock == null) {
            stock = new CurrentStock();
            stock.setItemId(itemId);
            stock.setCurrentStock(receivedQty);
        } else {
            double current = stock.getCurrentStock() != null ? stock.getCurrentStock() : 0.0;
            if (current < 0) current = 0.0;
            stock.setCurrentStock(current + receivedQty);
        }
        return repository.save(stock);
    }

    /**
     * SUBTRACT stock — deducts sentQty from current stock.
     * Returns false if stock is insufficient.
     */
    public boolean deductStock(Long itemId, Double sentQty) {
        CurrentStock stock = repository.findByItemId(itemId);
        if (stock == null) return false;
        double current = stock.getCurrentStock() != null ? stock.getCurrentStock() : 0.0;
        if (current < sentQty) return false;
        stock.setCurrentStock(Math.max(0.0, current - sentQty));
        repository.save(stock);
        return true;
    }

    /** Save or update a stock record directly */
    public CurrentStock save(CurrentStock stock) {
        if (stock != null && stock.getCurrentStock() != null && stock.getCurrentStock() < 0) {
            stock.setCurrentStock(0.0);
        }
        return repository.save(stock);
    }

    public void deductStock(Long itemId, double qty) {
        deductStock(itemId, qty, null, "MANUAL");
    }

    public void deductStock(Long itemId, double qty, Long transactionId, String transactionType) {
        CurrentStock stock = repository.findByItemId(itemId);
        if (stock == null) {
            stock = new CurrentStock();
            stock.setItemId(itemId);
            stock.setCurrentStock(0.0);
            repository.save(stock);
        }
        double current = stock.getCurrentStock() != null ? stock.getCurrentStock() : 0.0;
        if (current < 0) current = 0.0;
        
        stock.setCurrentStock(Math.max(0.0, current - qty));
        repository.save(stock);

        // Log to Stock Ledger
        StockLedger ledger = new StockLedger();
        ledger.setItemId(itemId);
        ledger.setReceiveQuantity(0.0);
        ledger.setSendQuantity(qty);
        ledger.setTransactionId(transactionId);
        ledger.setTransactionType(transactionType);
        ledger.setEntryDate(new java.util.Date());
        ledgerRepository.save(ledger);
    }

    public void addStock(Long itemId, double qty) {
        addStock(itemId, qty, null, "MANUAL");
    }

    public void addStock(Long itemId, double qty, Long transactionId, String transactionType) {
        CurrentStock stock = repository.findByItemId(itemId);
        if (stock == null) {
            stock = new CurrentStock();
            stock.setItemId(itemId);
            stock.setCurrentStock(qty);
        } else {
            double current = stock.getCurrentStock() != null ? stock.getCurrentStock() : 0.0;
            stock.setCurrentStock(current + qty);
        }
        repository.save(stock);

        // Log to Stock Ledger
        StockLedger ledger = new StockLedger();
        ledger.setItemId(itemId);
        ledger.setReceiveQuantity(qty);
        ledger.setSendQuantity(0.0);
        ledger.setTransactionId(transactionId);
        ledger.setTransactionType(transactionType);
        ledger.setEntryDate(new java.util.Date());
        ledgerRepository.save(ledger);
    }
}