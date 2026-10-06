package com.erp.erpsoftware.entity;

import java.util.Date;

import jakarta.persistence.*;

@Entity
@Table(name = "item_master", schema = "erp")
@SuppressWarnings("JpaDataSourceORMInspection")
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Long itemId;

    @Column(name = "item_code", nullable = false, length = 50)
    private String itemCode;

    @Column(name = "item_name", nullable = false, length = 100)
    private String itemName;

    @Column(name = "item_description", length = 255)
    private String itemDescription;

    @Column(name = "unit_id")
    private Integer unitId;

    @Column(name = "type_id")
    private Integer typeId;

    @Transient
    private Unit unit = new Unit();

    @Transient
    private Type type = new Type();

    @Column(name = "rate", nullable = false)
    private Double rate = 0.0;

    @Column(name = "is_valid")
    private Integer isValid = 1;

    @Column(name = "entry_date")
    private Date entryDate;

    @Column(name = "modified_date")
    private Date modifiedDate;

    // Explicit Getters and Setters
    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }

    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public String getItemDescription() { return itemDescription; }
    public void setItemDescription(String itemDescription) { this.itemDescription = itemDescription; }

    public Integer getUnitId() { return unitId; }
    public void setUnitId(Integer unitId) { this.unitId = unitId; }

    public Integer getTypeId() { return typeId; }
    public void setTypeId(Integer typeId) { this.typeId = typeId; }

    public Unit getUnit() { return unit; }
    public void setUnit(Unit unit) { this.unit = unit; }

    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }

    public Double getRate() { return rate; }
    public void setRate(Double rate) { this.rate = rate; }

    public Integer getIsValid() { return isValid; }
    public void setIsValid(Integer isValid) { this.isValid = isValid; }

    public Date getEntryDate() { return entryDate; }
    public void setEntryDate(Date entryDate) { this.entryDate = entryDate; }

    public Date getModifiedDate() { return modifiedDate; }
    public void setModifiedDate(Date modifiedDate) { this.modifiedDate = modifiedDate; }
}
