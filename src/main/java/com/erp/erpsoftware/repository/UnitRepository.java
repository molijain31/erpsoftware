package com.erp.erpsoftware.repository;


import com.erp.erpsoftware.entity.Unit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.Nullable;

import java.util.List;

public interface UnitRepository extends JpaRepository<Unit, Integer> {
    @Query("SELECT COALESCE(MAX(u.unitId), 0) + 1 FROM Unit u")
    int getNextId();

    @Query("SELECT u FROM Unit u WHERE u.unitId IS NOT NULL AND (u.isValid != 0 OR u.isValid IS NULL) ORDER BY u.unitId ASC")
    List<Unit> findByUnitIdNotNullOrderByUnitIdAsc();


}

