package com.erp.erpsoftware.repository;

import com.erp.erpsoftware.entity.PurchaseOrderMapping;
import com.erp.erpsoftware.entity.PurchaseOrderMappingId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseOrderMappingRepository extends JpaRepository<PurchaseOrderMapping, PurchaseOrderMappingId> {

    /** All active details for a given order */
    @Query("SELECT d FROM PurchaseOrderMapping d WHERE d.orderId = :orderId AND (d.isValid != 0 OR d.isValid IS NULL) ORDER BY d.itemId ASC")
    List<PurchaseOrderMapping> findActiveDetailsByOrderId(@Param("orderId") Long orderId);

    /** All details (including soft-deleted) for an order */
    List<PurchaseOrderMapping> findByOrderId(Long orderId);

    /** Active details for a specific item across all orders */
    @Query("SELECT d FROM PurchaseOrderMapping d WHERE d.itemId = :itemId AND (d.isValid != 0 OR d.isValid IS NULL)")
    List<PurchaseOrderMapping> findActiveDetailsByItemId(@Param("itemId") Long itemId);

    /**
     * Active details by status (1=Created, 2=Partially Received, 3=Fully Received)
     */
    @Query("SELECT d FROM PurchaseOrderMapping d WHERE d.status = :status AND (d.isValid != 0 OR d.isValid IS NULL)")
    List<PurchaseOrderMapping> findActiveDetailsByStatus(@Param("status") Integer status);

    /** Active details for a given order and item */
    @Query("SELECT d FROM PurchaseOrderMapping d WHERE d.orderId = :orderId AND d.itemId = :itemId AND (d.isValid != 0 OR d.isValid IS NULL)")
    List<PurchaseOrderMapping> findActiveDetailsByOrderIdAndItemId(@Param("orderId") Long orderId,
            @Param("itemId") Long itemId);
}
