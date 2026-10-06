package com.erp.erpsoftware.repository;

import com.erp.erpsoftware.entity.SupplierAccountPaypal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupplierAccountPaypalRepository extends JpaRepository<SupplierAccountPaypal, Long> {

    @Query("SELECT s FROM SupplierAccountPaypal s WHERE s.isValid = 1 ORDER BY s.accountId DESC")
    List<SupplierAccountPaypal> findAllActive();

    @Query("SELECT s FROM SupplierAccountPaypal s WHERE s.isValid = 1 AND s.supplierId = :supplierId ORDER BY s.accountId DESC")
    List<SupplierAccountPaypal> findActiveBySupplierId(@org.springframework.data.repository.query.Param("supplierId") Long supplierId);
}
