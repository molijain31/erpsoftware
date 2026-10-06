package com.erp.erpsoftware.repository;

import com.erp.erpsoftware.entity.VendorItemMapping;
import com.erp.erpsoftware.entity.VendorItemMappingId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VendorItemMappingRepository extends JpaRepository<VendorItemMapping, VendorItemMappingId> {
    @Query("SELECT COALESCE(MAX(u.venItemId), 0) + 1 FROM VendorItemMapping u")
    long getNextId();

    List<VendorItemMapping> findByVendorId(Long id);

    List<VendorItemMapping> findByItemId(Long itemId);

    long countByVendorId(Long vendorId);
}
