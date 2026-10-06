package com.erp.erpsoftware.service;

import com.erp.erpsoftware.entity.Type;
import com.erp.erpsoftware.repository.TypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class TypeService {
    @Autowired
    private TypeRepository repository;

    public List<Type> getAllTypes() {
        return repository.findAllActive();
    }

    public Type saveType(Type type) {
        Date now = new Date();
        if (type.getTypeId() == null) {
            type.setEntrydate(now);
        } else {
            Type existing = repository.findById(type.getTypeId()).orElseThrow();
            type.setEntrydate(existing.getEntrydate() != null ? existing.getEntrydate() : now);
            type.setModifieddate(now);
        }
        if (type.getIsValid() == null) {
            type.setIsValid(1);
        }
        return repository.save(type);
    }

    public Type getTypeById(Integer id) {
        return repository.findById(id).orElseThrow();
    }

    @org.springframework.transaction.annotation.Transactional
    public void deleteType(Integer id) {
        Type type = repository.findById(id).orElseThrow();
        type.setIsValid(0);
        repository.save(type);
    }
}
