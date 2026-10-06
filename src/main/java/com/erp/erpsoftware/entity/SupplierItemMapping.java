package com.erp.erpsoftware.entity;

import jakarta.persistence.*;
import java.util.Date;
import org.springframework.format.annotation.DateTimeFormat;

@Entity
@Table(name = "supplier_item_mapping", schema = "erp")
public class SupplierItemMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mapping_id")
    private Long mappingId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "type_id", nullable = false)
    private Type type;

    @Column(name = "rate", nullable = false)
    private Double rate;

    @Column(name = "purchase_rate", nullable = false)
    private Double purchaseRate;

    @Column(name = "from_date")
    @Temporal(TemporalType.DATE)
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date fromDate;

    @Column(name = "upto_date")
    @Temporal(TemporalType.DATE)
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date uptoDate;

    @PrePersist
    @PreUpdate
    protected void syncRates() {
        if (purchaseRate == null && rate != null) {
            purchaseRate = rate;
        } else if (rate == null && purchaseRate != null) {
            rate = purchaseRate;
        }
    }

    // Explicit Getters and Setters
    public Long getMappingId() { return mappingId; }
    public void setMappingId(Long mappingId) { this.mappingId = mappingId; }

    public Supplier getSupplier() { return supplier; }
    public void setSupplier(Supplier supplier) { this.supplier = supplier; }

    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }

    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }

    public Double getRate() { return rate; }
    public void setRate(Double rate) { this.rate = rate; }

    public Double getPurchaseRate() { return purchaseRate; }
    public void setPurchaseRate(Double purchaseRate) { this.purchaseRate = purchaseRate; }

    public Date getFromDate() { return fromDate; }
    public void setFromDate(Date fromDate) { this.fromDate = fromDate; }

    public Date getUptoDate() { return uptoDate; }
    public void setUptoDate(Date uptoDate) { this.uptoDate = uptoDate; }
}
