package com.erp.erpsoftware.repository;

import com.erp.erpsoftware.entity.Sales;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SalesRepository extends JpaRepository<Sales, Integer> {

    /** Total sent/issued quantity for an item across all sales orders */
    @Query("SELECT COALESCE(SUM(s.quantity), 0) FROM Sales s WHERE s.itemId = :itemId AND (s.isValid != 0 OR s.isValid IS NULL)")
    Double getTotalSentQuantityByItemId(@Param("itemId") Integer itemId);

    List<Sales> findByItemId(Integer itemId);
}