package com.erp.erpsoftware.bean;


import java.io.Serializable;
import java.util.Date;

import com.erp.erpsoftware.entity.Item;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VendorItemMappingDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long venItemId;
    private Long vendorId;
    private Item item = new Item();
    private Double rate;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date fromDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date uptoDate;

    // Explicit Getters and Setters
    public Long getVenItemId() { return venItemId; }
    public void setVenItemId(Long venItemId) { this.venItemId = venItemId; }

    public Long getVendorId() { return vendorId; }
    public void setVendorId(Long vendorId) { this.vendorId = vendorId; }

    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }

    public Double getRate() { return rate; }
    public void setRate(Double rate) { this.rate = rate; }

    public Date getFromDate() { return fromDate; }
    public void setFromDate(Date fromDate) { this.fromDate = fromDate; }

    public Date getUptoDate() { return uptoDate; }
    public void setUptoDate(Date uptoDate) { this.uptoDate = uptoDate; }
}

