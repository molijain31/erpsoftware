package com.erp.erpsoftware.entity;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

@Entity
@Table(name = "supplier_master", schema = "erp")
@SuppressWarnings("JpaDataSourceORMInspection")
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "supplier_id")
    private Long supplierId;

    @Column(name = "supplier_name", nullable = false, length = 100)
    private String supplierName;

    @Column(name = "supplier_code", length = 20)
    private String supplierCode;

    @Column(name = "contact_person", nullable = false, length = 100)
    private String contactPerson;

    @Column(name = "mobile", nullable = false, length = 15)
    private String mobile;

    @Column(name = "contact_person1", length = 100)
    private String contactPerson1;

    @Column(name = "contact_person2", length = 100)
    private String contactPerson2;

    @Column(name = "contact_person3", length = 100)
    private String contactPerson3;

    @Column(name = "mobile1", length = 15)
    private String mobile1;

    @Column(name = "mobile2", length = 15)
    private String mobile2;

    @Column(name = "mobile3", length = 15)
    private String mobile3;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "gst_no", length = 20)
    private String gstNo;

    @Column(name = "pan_no", length = 20)
    private String panNo;

    @Column(name = "state", length = 50)
    private String state;

    @Column(name = "city", length = 50)
    private String city;

    @Column(name = "pincode", length = 10)
    private String pincode;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "account_no", length = 30)
    private String accountNo;

    @Column(name = "ifsc_code", length = 20)
    private String ifscCode;

    @Column(name = "branch_name", length = 100)
    private String branchName;

    @Column(name = "opening_balance")
    private Double openingBalance;

    @Column(name = "amount_to_be_adjust")
    private Double amountToBeAdjust = 0.0;

    @Column(name = "is_valid")
    private Integer isValid = 1;

    @Column(name = "entry_date", updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date entryDate;

    @Column(name = "modified_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date modifiedDate;

    @OneToMany(mappedBy = "supplier", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SupplierItemMapping> mappings = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        entryDate = new Date();
        modifiedDate = new Date();
        if (isValid == null) {
            isValid = 1;
        }
        if (contactPerson1 != null && (contactPerson == null || contactPerson.isEmpty())) {
            contactPerson = contactPerson1;
        }
        if (mobile1 != null && (mobile == null || mobile.isEmpty())) {
            mobile = mobile1;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        modifiedDate = new Date();
        if (contactPerson1 != null) {
            contactPerson = contactPerson1;
        }
        if (mobile1 != null) {
            mobile = mobile1;
        }
    }

    // Explicit Getters and Setters
    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }

    public String getSupplierName() {
        if (supplierName != null && !supplierName.trim().isEmpty() && !"null".equalsIgnoreCase(supplierName.trim())) {
            return supplierName.trim();
        }
        if (contactPerson1 != null && !contactPerson1.trim().isEmpty()) {
            return contactPerson1.trim();
        }
        if (contactPerson != null && !contactPerson.trim().isEmpty()) {
            return contactPerson.trim();
        }
        if (supplierId != null) {
            return "Supplier #" + supplierId;
        }
        return "";
    }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

    public String getSupplierCode() { return supplierCode; }
    public void setSupplierCode(String supplierCode) { this.supplierCode = supplierCode; }

    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }

    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }

    public String getContactPerson1() {
        if (contactPerson1 == null || contactPerson1.isEmpty()) {
            return contactPerson;
        }
        return contactPerson1;
    }
    public void setContactPerson1(String contactPerson1) { this.contactPerson1 = contactPerson1; }

    public String getContactPerson2() { return contactPerson2; }
    public void setContactPerson2(String contactPerson2) { this.contactPerson2 = contactPerson2; }

    public String getContactPerson3() { return contactPerson3; }
    public void setContactPerson3(String contactPerson3) { this.contactPerson3 = contactPerson3; }

    public String getMobile1() {
        if (mobile1 == null || mobile1.isEmpty()) {
            return mobile;
        }
        return mobile1;
    }
    public void setMobile1(String mobile1) { this.mobile1 = mobile1; }

    public String getMobile2() { return mobile2; }
    public void setMobile2(String mobile2) { this.mobile2 = mobile2; }

    public String getMobile3() { return mobile3; }
    public void setMobile3(String mobile3) { this.mobile3 = mobile3; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getGstNo() { return gstNo; }
    public void setGstNo(String gstNo) { this.gstNo = gstNo; }

    public String getPanNo() { return panNo; }
    public void setPanNo(String panNo) { this.panNo = panNo; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getPincode() { return pincode; }
    public void setPincode(String pincode) { this.pincode = pincode; }
    public void setPincode(int pincode) { this.pincode = String.valueOf(pincode); }
    public void setPincode(Integer pincode) { this.pincode = pincode != null ? String.valueOf(pincode) : null; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public String getAccountNo() { return accountNo; }
    public void setAccountNo(String accountNo) { this.accountNo = accountNo; }
    public String getAccountNumber() { return accountNo; }

    public String getIfscCode() { return ifscCode; }
    public void setIfscCode(String ifscCode) { this.ifscCode = ifscCode; }

    public String getBranchName() { return branchName; }
    public void setBranchName(String branchName) { this.branchName = branchName; }
    public String getBranch() { return branchName; }

    public String getSupplierType() { return ""; }

    public Double getOpeningBalance() { return openingBalance; }
    public void setOpeningBalance(Double openingBalance) { this.openingBalance = openingBalance; }

    public Double getAmountToBeAdjust() { return amountToBeAdjust != null ? amountToBeAdjust : 0.0; }
    public void setAmountToBeAdjust(Double amountToBeAdjust) { this.amountToBeAdjust = amountToBeAdjust; }

    public Integer getIsValid() { return isValid; }
    public void setIsValid(Integer isValid) { this.isValid = isValid; }

    public Date getEntryDate() { return entryDate; }
    public void setEntryDate(Date entryDate) { this.entryDate = entryDate; }

    public Date getModifiedDate() { return modifiedDate; }
    public void setModifiedDate(Date modifiedDate) { this.modifiedDate = modifiedDate; }

    public List<SupplierItemMapping> getMappings() { return mappings; }
    public void setMappings(List<SupplierItemMapping> mappings) { this.mappings = mappings; }
}
