package com.erp.erpsoftware.repository;

import com.erp.erpsoftware.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long> {

    @org.springframework.data.jpa.repository.Query("SELECT i FROM Item i WHERE i.isValid != 0 OR i.isValid IS NULL ORDER BY i.itemId ASC")
    List<Item> findAllActive();
}
