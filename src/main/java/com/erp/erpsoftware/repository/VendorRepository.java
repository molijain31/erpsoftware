package com.erp.erpsoftware.repository;

import com.erp.erpsoftware.entity.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface VendorRepository extends JpaRepository<Vendor, Long> {

    @Query("SELECT v FROM Vendor v WHERE v.isValid != 0 OR v.isValid IS NULL ORDER BY v.vendorId ASC")
    List<Vendor> findAllActive();

    @Modifying
    @Transactional
    @Query(value = "UPDATE erp.vendor_master SET amount_to_be_adjust = :amount WHERE vendor_id = :vendorId", nativeQuery = true)
    void updateAmountToBeAdjust(@Param("vendorId") Long vendorId, @Param("amount") Double amount);
}
