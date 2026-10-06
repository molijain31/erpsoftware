package com.erp.erpsoftware.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Data
@Entity
@Table(name = "user_master", schema = "erp")
@SuppressWarnings("JpaDataSourceORMInspection")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "name", nullable = false, length = 100)
    private String userName;

    @Column(name = "type")
    private Integer userType;

    @Column(name = "loginid", length = 50)
    private String loginID;

    @Column(name = "password", nullable = false, length = 100)
    private String password;

    @Column(name = "mobile_no", length = 50)
    private String mobileNo;

    @Column(name = "email", length = 50)
    private String email;

    @Column(name = "state", length = 50)
    private String state;

    @Column(name = "district", length = 50)
    private String district;

    @Temporal(TemporalType.DATE)
    @Column(name = "\"entry date\"")
    private Date entryDate;

    @Temporal(TemporalType.DATE)
    @Column(name = "\"modified date\"")
    private Date modifiedDate;

    // Explicit Getters and Setters
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public Integer getUserType() { return userType; }
    public void setUserType(Integer userType) { this.userType = userType; }

    public String getLoginID() { return loginID; }
    public void setLoginID(String loginID) { this.loginID = loginID; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getMobileNo() { return mobileNo; }
    public void setMobileNo(String mobileNo) { this.mobileNo = mobileNo; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public Date getEntryDate() { return entryDate; }
    public void setEntryDate(Date entryDate) { this.entryDate = entryDate; }

    public Date getModifiedDate() { return modifiedDate; }
    public void setModifiedDate(Date modifiedDate) { this.modifiedDate = modifiedDate; }
}