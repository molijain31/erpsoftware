package com.erp.erpsoftware.repository;

import com.erp.erpsoftware.entity.AccountPaypal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountPaypalRepository extends JpaRepository<AccountPaypal, Long> {

    @Query("SELECT a FROM AccountPaypal a WHERE a.isValid = 1 ORDER BY a.accountId DESC")
    List<AccountPaypal> findAllActive();

    @Query("SELECT a FROM AccountPaypal a WHERE a.isValid = 1 AND a.vendorId = :vendorId ORDER BY a.accountId DESC")
    List<AccountPaypal> findActiveByVendorId(@org.springframework.data.repository.query.Param("vendorId") Long vendorId);
}
