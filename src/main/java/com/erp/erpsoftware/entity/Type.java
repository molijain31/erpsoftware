package com.erp.erpsoftware.entity;

import jakarta.persistence.*;

import java.util.Date;

@Entity
@Table(name = "type_master", schema = "erp")
@SuppressWarnings("JpaDataSourceORMInspection")
public class Type {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "type_id")
    private Integer typeId;

    @Column(name = "type_name", nullable = false, length = 100)
    private String typeName;

    @Column(name = "is_valid")
    private Integer isValid = 1;

    @Column(name = "entry_date", updatable = false)
    @Temporal(TemporalType.DATE)
    private Date entrydate;

    @Column(name = "modified_date")
    @Temporal(TemporalType.DATE)
    private Date modifieddate;

    @PrePersist
    protected void onCreate() {
        entrydate = new Date();
        modifieddate = new Date();
    }

    @PreUpdate
    protected void onUpdate() {
        modifieddate = new Date();
    }

    // Explicit Getters and Setters
    public Integer getTypeId() { return typeId; }
    public void setTypeId(Integer typeId) { this.typeId = typeId; }

    public String getTypeName() { return typeName; }
    public void setTypeName(String typeName) { this.typeName = typeName; }

    public Integer getIsValid() { return isValid; }
    public void setIsValid(Integer isValid) { this.isValid = isValid; }

    public Date getEntrydate() { return entrydate; }
    public void setEntrydate(Date entrydate) { this.entrydate = entrydate; }

    public Date getModifieddate() { return modifieddate; }
    public void setModifieddate(Date modifieddate) { this.modifieddate = modifieddate; }
}
