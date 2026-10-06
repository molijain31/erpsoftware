package com.erp.erpsoftware.repository;

import com.erp.erpsoftware.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    @Modifying
    @Transactional
    @Query(value = "UPDATE erp.supplier_master SET amount_to_be_adjust = :amount WHERE supplier_id = :supplierId", nativeQuery = true)
    void updateAmountToBeAdjust(@Param("supplierId") Long supplierId, @Param("amount") Double amount);
}