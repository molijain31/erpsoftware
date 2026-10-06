package com.erp.erpsoftware.service;
import com.erp.erpsoftware.entity.Unit;
import com.erp.erpsoftware.repository.UnitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class UnitService {

    @Autowired
    private UnitRepository repository;

    public List<Unit> getAllUnits() {
        return repository.findByUnitIdNotNullOrderByUnitIdAsc();
    }

    public Unit getUnitById(Integer id) {
        return repository.findById(id).orElse(null);
    }

    public void saveUnit(Unit unit) {
        Date currentDate = new Date();
        if (unit.getUnitId() == null) {
            unit.setUnitId(repository.getNextId());
            unit.setEntryDate(currentDate);
            unit.setModifiedDate(null);
            if (unit.getIsValid() == null) {
                unit.setIsValid(1); // default active
            }
        } else {
            // Keep the original entry date if it exists
            Unit existing = repository.findById(unit.getUnitId()).orElse(null);
            unit.setEntryDate(existing != null ? existing.getEntryDate() : currentDate);
        }
        unit.setModifiedDate(currentDate);
        repository.save(unit);
    }

    public void deleteUnit(Integer id) {
        Unit unit = repository.findById(id).orElse(null);
        if (unit != null) {
            unit.setIsValid(0);
            repository.save(unit);
        }
    }
}

