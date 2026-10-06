package com.erp.erpsoftware.repository;

import com.erp.erpsoftware.entity.SalesOrderDetail;
import com.erp.erpsoftware.entity.SalesOrderDetailId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SalesOrderDetailRepository extends JpaRepository<SalesOrderDetail, SalesOrderDetailId> {

    @Query("SELECT d FROM SalesOrderDetail d WHERE d.orderId = :orderId AND (d.isValid != 0 OR d.isValid IS NULL) ORDER BY d.itemId ASC")
    List<SalesOrderDetail> findActiveDetailsByOrderId(@Param("orderId") Long orderId);

    List<SalesOrderDetail> findByOrderId(Long orderId);

    @Query("SELECT d FROM SalesOrderDetail d WHERE d.itemId = :itemId AND (d.isValid != 0 OR d.isValid IS NULL)")
    List<SalesOrderDetail> findActiveDetailsByItemId(@Param("itemId") Long itemId);

    @Query("SELECT d FROM SalesOrderDetail d WHERE d.status = :status AND (d.isValid != 0 OR d.isValid IS NULL)")
    List<SalesOrderDetail> findActiveDetailsByStatus(@Param("status") Integer status);

    @Query("SELECT d FROM SalesOrderDetail d WHERE d.orderId = :orderId AND d.itemId = :itemId AND (d.isValid != 0 OR d.isValid IS NULL)")
    List<SalesOrderDetail> findActiveDetailsByOrderIdAndItemId(@Param("orderId") Long orderId, @Param("itemId") Long itemId);
}
