package com.erp.erpsoftware.repository;

import com.erp.erpsoftware.entity.Type;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface TypeRepository extends JpaRepository<Type, Integer> {

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM erp.item_master WHERE type_id = ?1", nativeQuery = true)
    void deleteReferencedItems(Integer typeId);

    @Query("SELECT t FROM Type t WHERE t.isValid = 1 ORDER BY t.typeId ASC")
    List<Type> findAllActive();
}
