package com.erp.erpsoftware.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;

@Data
@Entity
@Table(name = "unit_master", schema = "erp")
@SuppressWarnings("JpaDataSourceORMInspection")
public class Unit {

    @Id
    @Column(name = "unit_id")
    private Integer unitId;

    @Column(name = "unit_code", nullable = false, length = 50)
    private String unitCode;

    @Column(name = "unit_name", nullable = false, length = 100)
    private String unitName;

    @Column(name = "unit_type", nullable = false, length = 50)
    private String unitType;

    @Column(name = "is_valid")
    private Integer isValid = 1;


    @Column(name = "entry_date", nullable = false)
    @Temporal(TemporalType.DATE)
    private Date entryDate;

    @Column(name = "modified_date", nullable = false)
    @Temporal(TemporalType.DATE)
    private Date modifiedDate;

    // Explicit Getters and Setters
    public Integer getUnitId() { return unitId; }
    public void setUnitId(Integer unitId) { this.unitId = unitId; }

    public String getUnitCode() { return unitCode; }
    public void setUnitCode(String unitCode) { this.unitCode = unitCode; }

    public String getUnitName() { return unitName; }
    public void setUnitName(String unitName) { this.unitName = unitName; }

    public String getUnitType() { return unitType; }
    public void setUnitType(String unitType) { this.unitType = unitType; }

    public Integer getIsValid() { return isValid; }
    public void setIsValid(Integer isValid) { this.isValid = isValid; }

    public Date getEntryDate() { return entryDate; }
    public void setEntryDate(Date entryDate) { this.entryDate = entryDate; }

    public Date getModifiedDate() { return modifiedDate; }
    public void setModifiedDate(Date modifiedDate) { this.modifiedDate = modifiedDate; }
}