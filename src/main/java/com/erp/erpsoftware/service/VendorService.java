package com.erp.erpsoftware.service;

import com.erp.erpsoftware.bean.VendorItemMappingDTO;
import com.erp.erpsoftware.entity.Vendor;
import com.erp.erpsoftware.entity.VendorItemMapping;
import com.erp.erpsoftware.repository.VendorItemMappingRepository;
import com.erp.erpsoftware.repository.VendorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class VendorService {
    @Autowired
    private VendorRepository repository;
    
    @Autowired
    private VendorItemMappingRepository vendorItemMappingRepository;

    public List<Vendor> getAllVendors() {
      List<VendorItemMapping> vendorItemMappings = vendorItemMappingRepository.findAll(); // Ensure mappings are loaded
        Map<Long, Long> vendorIdMappingSum = vendorItemMappings.stream().collect(
                Collectors.groupingBy(VendorItemMapping::getVendorId, Collectors.counting())
        );

        List<Vendor> vendors = repository.findAllActive();
        for (Vendor vendor : vendors) {
            Long sum = vendorIdMappingSum.get(vendor.getVendorId());
            vendor.setActiveMappingsCount(sum != null ? sum : 1L);
        }

        return repository.findAllActive();
    }

    public Vendor saveVendor(Vendor vendor) {
        if (vendor.getIsValid() == null) {
            vendor.setIsValid(1);
        }

        Vendor savedVendor = repository.save(vendor);
        Long vendorId = savedVendor.getVendorId();
        
        List<VendorItemMappingDTO> submitted = vendor.getMappings();

        if (vendorId != null) {
            List<VendorItemMapping> existing = vendorItemMappingRepository.findByVendorId(vendorId);
            vendorItemMappingRepository.deleteAll(existing);
        }
        
        List<VendorItemMapping> validMappings = new java.util.ArrayList<>();
        if (submitted != null && !submitted.isEmpty()) {
            // Deduplicate submitted mappings by itemId
            List<VendorItemMappingDTO> uniqueSubmitted = new java.util.ArrayList<>();
            java.util.Set<Long> seenItemIds = new java.util.HashSet<>();
            for (VendorItemMappingDTO dto : submitted) {
                if (dto != null && dto.getItem() != null && dto.getItem().getItemId() != null) {
                    Long itemId = dto.getItem().getItemId();
                    if (!seenItemIds.contains(itemId)) {
                        seenItemIds.add(itemId);
                        uniqueSubmitted.add(dto);
                    }
                }
            }

            long nextId = vendorItemMappingRepository.getNextId();
            for (VendorItemMappingDTO dto : uniqueSubmitted) {
                VendorItemMapping mapping = new VendorItemMapping();
                mapping.setVenItemId(nextId++);
                mapping.setVendorId(vendorId);
                mapping.setItemId(dto.getItem().getItemId());
                mapping.setRate(dto.getRate());
                mapping.setFromDate(dto.getFromDate());
                mapping.setUptoDate(dto.getUptoDate());
                validMappings.add(mapping);
            }

            vendorItemMappingRepository.saveAll(validMappings);
        }
        
        return savedVendor;
    }

    public Vendor getVendorById(Long id) {
        return repository.findById(id).orElseThrow();
    }

    public void deleteVendor(Long id) {
        Vendor vendor = repository.findById(id).orElseThrow();
        vendor.setIsValid(0);
        repository.save(vendor);
    }
}
