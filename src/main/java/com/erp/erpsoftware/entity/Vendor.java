package com.erp.erpsoftware.entity;

import com.erp.erpsoftware.bean.VendorItemMappingDTO;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "vendor_master", schema = "erp")
public class Vendor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vendor_id")
    private Long vendorId;

    @Column(name = "vendor_name", nullable = false, length = 100)
    private String vendorName;

    @Column(name = "contact_person", length = 100)
    private String contactPerson;

    @Column(name = "mobile", length = 15)
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

    @Column(name = "transaction_account", length = 100)
    private String transactionAccount;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "pincode")
    private Integer pincode;

    @Column(name = "amount_to_be_adjust")
    private Double amountToBeAdjust = 0.0;

    @Column(name = "is_valid")
    private Integer isValid = 1;

    @Column(name = "entry_date", updatable = false)
    @Temporal(TemporalType.DATE)
    private Date entryDate;

    @Column(name = "modified_date")
    @Temporal(TemporalType.DATE)
    private Date modifiedDate;

    @Transient
    private List<VendorItemMappingDTO> mappings = new ArrayList<>();

    @Transient
    private Long activeMappingsCount;

    @PrePersist
    protected void onCreate() {
        entryDate = new Date();
        modifiedDate = new Date();
        if (contactPerson1 != null) {
            contactPerson = contactPerson1;
        }
        if (mobile1 != null) {
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

    public Vendor() {}

    // Explicit Getters and Setters
    public Long getVendorId() { return vendorId; }
    public void setVendorId(Long vendorId) { this.vendorId = vendorId; }

    public String getVendorName() { return vendorName; }
    public void setVendorName(String vendorName) { this.vendorName = vendorName; }

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

    public String getTransactionAccount() { return transactionAccount; }
    public void setTransactionAccount(String transactionAccount) { this.transactionAccount = transactionAccount; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public Integer getPincode() { return pincode; }
    public void setPincode(Integer pincode) { this.pincode = pincode; }
    public void setPincode(String pincode) {
        try {
            this.pincode = pincode != null ? Integer.parseInt(pincode) : null;
        } catch (NumberFormatException e) {
            this.pincode = null;
        }
    }

    public Double getAmountToBeAdjust() { return amountToBeAdjust != null ? amountToBeAdjust : 0.0; }
    public void setAmountToBeAdjust(Double amountToBeAdjust) { this.amountToBeAdjust = amountToBeAdjust; }

    public Integer getIsValid() { return isValid; }
    public void setIsValid(Integer isValid) { this.isValid = isValid; }

    public Date getEntryDate() { return entryDate; }
    public void setEntryDate(Date entryDate) { this.entryDate = entryDate; }

    public Date getModifiedDate() { return modifiedDate; }
    public void setModifiedDate(Date modifiedDate) { this.modifiedDate = modifiedDate; }

    public List<VendorItemMappingDTO> getMappings() { return mappings; }
    public void setMappings(List<VendorItemMappingDTO> mappings) { this.mappings = mappings; }

    public Long getActiveMappingsCount() { return activeMappingsCount; }
    public void setActiveMappingsCount(Long activeMappingsCount) { this.activeMappingsCount = activeMappingsCount; }
}
