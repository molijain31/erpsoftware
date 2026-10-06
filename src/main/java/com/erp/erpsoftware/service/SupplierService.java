package com.erp.erpsoftware.service;

import com.erp.erpsoftware.entity.Supplier;
import com.erp.erpsoftware.entity.SupplierItemMapping;
import com.erp.erpsoftware.repository.SupplierRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class SupplierService {
    @Autowired
    private SupplierRepository repository;

    public List<Supplier> getAllSuppliers() {
        return repository.findAll();
    }

    public Supplier saveSupplier(Supplier supplier) {
        System.out.println("=== SUBMITTED SUPPLIER MAPPINGS ===");
        if (supplier.getMappings() != null) {
            System.out.println("Mappings list size: " + supplier.getMappings().size());
            for (int i = 0; i < supplier.getMappings().size(); i++) {
                SupplierItemMapping m = supplier.getMappings().get(i);
                if (m != null) {
                    System.out.println("  [" + i + "]: mappingId=" + m.getMappingId() +
                            ", item=" + (m.getItem() != null ? m.getItem().getItemId() : "null") +
                            ", type=" + (m.getType() != null ? m.getType().getTypeId() : "null") +
                            ", rate=" + m.getRate());
                } else {
                    System.out.println("  [" + i + "]: null");
                }
            }
        } else {
            System.out.println("Mappings list is NULL");
        }
        System.out.println("====================================");

        if (supplier.getMappings() != null) {
            supplier.getMappings().removeIf(mapping -> 
                mapping == null ||
                mapping.getItem() == null || mapping.getItem().getItemId() == null ||
                mapping.getType() == null || mapping.getType().getTypeId() == null
            );
        }
        if (supplier.getSupplierId() == null) {
            if (supplier.getMappings() != null) {
                for (SupplierItemMapping mapping : supplier.getMappings()) {
                    mapping.setSupplier(supplier);
                }
            }
            return repository.save(supplier);
        } else {
            Supplier existing = repository.findById(supplier.getSupplierId())
                    .orElseThrow(() -> new IllegalArgumentException("Supplier not found with ID: " + supplier.getSupplierId()));

            existing.setSupplierName(supplier.getSupplierName());
            existing.setContactPerson1(supplier.getContactPerson1());
            existing.setContactPerson2(supplier.getContactPerson2());
            existing.setContactPerson3(supplier.getContactPerson3());
            existing.setMobile1(supplier.getMobile1());
            existing.setMobile2(supplier.getMobile2());
            existing.setMobile3(supplier.getMobile3());
            existing.setContactPerson(supplier.getContactPerson1());
            existing.setMobile(supplier.getMobile1());
            existing.setEmail(supplier.getEmail());
            existing.setAddress(supplier.getAddress());
            existing.setGstNo(supplier.getGstNo());
            existing.setCity(supplier.getCity());
            existing.setState(supplier.getState());
            existing.setPincode(supplier.getPincode());

            if (supplier.getMappings() == null) {
                existing.getMappings().clear();
            } else {
                Set<Long> keepIds = supplier.getMappings().stream()
                        .map(SupplierItemMapping::getMappingId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());

                existing.getMappings().removeIf(m -> m.getMappingId() != null && !keepIds.contains(m.getMappingId()));

                for (SupplierItemMapping submitted : supplier.getMappings()) {
                    if (submitted.getMappingId() == null) {
                        submitted.setSupplier(existing);
                        existing.getMappings().add(submitted);
                    } else {
                        SupplierItemMapping existingMapping = existing.getMappings().stream()
                                .filter(m -> m.getMappingId().equals(submitted.getMappingId()))
                                .findFirst()
                                .orElse(null);
                        if (existingMapping != null) {
                            existingMapping.setItem(submitted.getItem());
                            existingMapping.setType(submitted.getType());
                            existingMapping.setRate(submitted.getRate());
                            existingMapping.setFromDate(submitted.getFromDate());
                            existingMapping.setUptoDate(submitted.getUptoDate());
                        }
                    }
                }
            }
            return repository.save(existing);
        }
    }

    public Supplier getSupplierById(Long id) {
        return repository.findById(id).orElseThrow();
    }


    public Supplier updateSupplier(Supplier supplier) {
        return repository.save(supplier);
    }


    public void deleteSupplier(Long id) {

        repository.deleteById(id);
    }
}
