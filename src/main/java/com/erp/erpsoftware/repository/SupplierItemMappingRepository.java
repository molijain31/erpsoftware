package com.erp.erpsoftware.repository;

import com.erp.erpsoftware.entity.SupplierItemMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupplierItemMappingRepository extends JpaRepository<SupplierItemMapping, Long> {
    List<SupplierItemMapping> findBySupplierSupplierId(Long supplierId);
    List<SupplierItemMapping> findByItemItemId(Long itemId);
}
