package com.erp.erpsoftware.repository;

import com.erp.erpsoftware.entity.AccountLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountLedgerRepository extends JpaRepository<AccountLedger, Long> {

    @Query("SELECT a FROM AccountLedger a WHERE a.entityType = :entityType AND a.supplierId = :supplierId AND (a.isValid IS NULL OR a.isValid = 1) ORDER BY a.ledgerId DESC")
    List<AccountLedger> findActiveBySupplierId(@Param("entityType") String entityType, @Param("supplierId") Long supplierId);

    @Query("SELECT a FROM AccountLedger a WHERE a.entityType = :entityType AND a.vendorId = :vendorId AND (a.isValid IS NULL OR a.isValid = 1) ORDER BY a.ledgerId DESC")
    List<AccountLedger> findActiveByVendorId(@Param("entityType") String entityType, @Param("vendorId") Long vendorId);

    @Query("SELECT a FROM AccountLedger a WHERE a.bookingNo = :bookingNo AND (a.isValid IS NULL OR a.isValid = 1)")
    List<AccountLedger> findByBookingNo(@Param("bookingNo") String bookingNo);
}
