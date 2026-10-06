package com.erp.erpsoftware.repository;

import com.erp.erpsoftware.entity.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    @Query("SELECT p FROM PurchaseOrder p ORDER BY p.orderId DESC")
    List<PurchaseOrder> findAllActive();
}
