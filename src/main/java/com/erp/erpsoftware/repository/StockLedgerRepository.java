package com.erp.erpsoftware.repository;

import com.erp.erpsoftware.entity.StockLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockLedgerRepository extends JpaRepository<StockLedger, Long> {

    @Query("SELECT COALESCE(SUM(s.sendQuantity), 0.0) FROM StockLedger s WHERE s.transactionId = :transactionId AND s.itemId = :itemId AND s.transactionType = 'SALES_ORDER'")
    double sumSendQuantityByOrderIdAndItemId(@Param("transactionId") Long transactionId, @Param("itemId") Long itemId);

    /** All ledger entries for a specific item, newest first */
    List<StockLedger> findByItemIdOrderByEntryDateDesc(Long itemId);

    /** All ledger entries for a specific item, oldest first */
    List<StockLedger> findByItemIdOrderByEntryDateAsc(Long itemId);

    /** All ledger entries for a specific transaction (e.g., a purchase order) */
    List<StockLedger> findByTransactionId(Long transactionId);

    /** All ledger entries by transaction type */
    List<StockLedger> findByTransactionType(String transactionType);

    /** Entries for a specific item and transaction type */
    List<StockLedger> findByItemIdAndTransactionType(Long itemId, String transactionType);

    /**
     * Net stock for an item:
     * SUM(receive_quantity) - SUM(send_quantity)
     */
    @Query("SELECT COALESCE(SUM(s.receiveQuantity), 0) - COALESCE(SUM(s.sendQuantity), 0) " +
           "FROM StockLedger s WHERE s.itemId = :itemId")
    Double getNetStockByItemId(@Param("itemId") Long itemId);

    /** Total received for an item */
    @Query("SELECT COALESCE(SUM(s.receiveQuantity), 0) FROM StockLedger s WHERE s.itemId = :itemId")
    Double getTotalReceiveByItemId(@Param("itemId") Long itemId);

    /** Total sent/issued for an item */
    @Query("SELECT COALESCE(SUM(s.sendQuantity), 0) FROM StockLedger s WHERE s.itemId = :itemId")
    Double getTotalSendByItemId(@Param("itemId") Long itemId);

    @Query("SELECT DISTINCT s.itemId FROM StockLedger s")
    List<Long> findDistinctItemIds();
}
