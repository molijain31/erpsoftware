package com.erp.erpsoftware.entity;

import jakarta.persistence.Column;
import java.io.Serializable;
import java.util.Objects;

public class VendorItemMappingId implements Serializable {
    private static final long serialVersionUID = 1L;

    @Column(name = "vendor_id")
    private Long vendorId;
    
    @Column(name = "item_id")
    private Long itemId;

    public VendorItemMappingId() {
    }

    public VendorItemMappingId(Long vendorId, Long itemId) {
        this.vendorId = vendorId;
        this.itemId = itemId;
    }

    public Long getVendorId() {
        return vendorId;
    }

    public void setVendorId(Long vendorId) {
        this.vendorId = vendorId;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VendorItemMappingId that = (VendorItemMappingId) o;
        return Objects.equals(vendorId, that.vendorId) && Objects.equals(itemId, that.itemId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(vendorId, itemId);
    }
}
