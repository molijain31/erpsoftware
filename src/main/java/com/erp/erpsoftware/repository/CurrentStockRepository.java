package com.erp.erpsoftware.repository;

import com.erp.erpsoftware.entity.CurrentStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CurrentStockRepository extends JpaRepository<CurrentStock, Integer> {

    /** Find current stock record by item */
    CurrentStock findByItemId(Long itemId);

    /** Get all stock records */
    List<CurrentStock> findAll();
}