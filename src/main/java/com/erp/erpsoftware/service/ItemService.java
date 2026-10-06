package com.erp.erpsoftware.service;

import com.erp.erpsoftware.entity.Item;
import com.erp.erpsoftware.entity.Unit;
import com.erp.erpsoftware.entity.Type;
import com.erp.erpsoftware.repository.ItemRepository;
import com.erp.erpsoftware.repository.UnitRepository;
import com.erp.erpsoftware.repository.TypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class ItemService {

    @Autowired
    private ItemRepository repository;

    @Autowired
    private UnitRepository unitRepository;

    @Autowired
    private TypeRepository typeRepository;

    private void populateTransientFields(Item item) {
        if (item != null) {
            if (item.getUnitId() != null) {
                Unit u = unitRepository.findById(item.getUnitId()).orElse(null);
                if (u != null) {
                    item.setUnit(u);
                }
            }
            if (item.getTypeId() != null) {
                Type t = typeRepository.findById(item.getTypeId()).orElse(null);
                if (t != null) {
                    item.setType(t);
                }
            }
        }
    }

    public List<Item> getAllItems() {
        List<Item> items = repository.findAllActive();
        for (Item item : items) {
            populateTransientFields(item);
        }
        return items;
    }

    public Item getItemById(Long id) {
        Item item = repository.findById(id).orElse(null);
        populateTransientFields(item);
        return item;
    }

    public Item saveItem(Item item) {
        Date currentDate = new Date();
        if (item.getUnit() != null) {
            item.setUnitId(item.getUnit().getUnitId());
        }
        if (item.getType() != null) {
            item.setTypeId(item.getType().getTypeId());
        }
        if (item.getItemId() == null) {
            item.setEntryDate(currentDate);
            if (item.getIsValid() == null) {
                item.setIsValid(1); // default active
            }
        } else {
            Item existing = repository.findById(item.getItemId()).orElse(null);
            item.setEntryDate(existing != null ? existing.getEntryDate() : currentDate);
        }
        item.setModifiedDate(currentDate);
        Item saved = repository.save(item);
        populateTransientFields(saved);
        return saved;
    }

    public void deleteItem(Long id) {
        Item item = repository.findById(id).orElse(null);
        if (item != null) {
            item.setIsValid(0);
            repository.save(item);
        }
    }
}
