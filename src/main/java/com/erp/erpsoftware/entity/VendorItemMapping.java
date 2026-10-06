package com.erp.erpsoftware.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Date;
import org.springframework.format.annotation.DateTimeFormat;

@Entity
@IdClass(VendorItemMappingId.class)
@Table(name = "vendor_item_mapping", schema = "erp")
public class VendorItemMapping implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "ven_item_id")
    private Long venItemId;

    @Id
    @Column(name = "vendor_id")
    private Long vendorId;

    @Id
    @Column(name = "item_id")
    private Long itemId;

    @Column(name = "rate", nullable = false)
    private Double rate;

    @Column(name = "from_date")
    @Temporal(TemporalType.DATE)
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date fromDate;

    @Column(name = "upto_date")
    @Temporal(TemporalType.DATE)
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date uptoDate;

    // Explicit Getters and Setters
    public Long getVenItemId() { return venItemId; }
    public void setVenItemId(Long venItemId) { this.venItemId = venItemId; }

    public Long getVendorId() { return vendorId; }
    public void setVendorId(Long vendorId) { this.vendorId = vendorId; }

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }

    public Double getRate() { return rate; }
    public void setRate(Double rate) { this.rate = rate; }

    public Date getFromDate() { return fromDate; }
    public void setFromDate(Date fromDate) { this.fromDate = fromDate; }

    public Date getUptoDate() { return uptoDate; }
    public void setUptoDate(Date uptoDate) { this.uptoDate = uptoDate; }
}
