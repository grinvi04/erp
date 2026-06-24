package com.erp.crm.domain.model;

import com.erp.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity(name = "CrmAccount")
@Table(name = "account", schema = "crm")
public class Account extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "crm_account_seq")
    @SequenceGenerator(name = "crm_account_seq", sequenceName = "crm.account_id_seq", allocationSize = 50)
    private Long id;

    @Column(name = "code", nullable = false, length = 30)
    private String code;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "business_no", length = 30)
    private String businessNo;

    @Column(name = "industry", length = 100)
    private String industry;

    @Column(name = "website", length = 300)
    private String website;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "employee_count")
    private Integer employeeCount;

    @Column(name = "annual_revenue", precision = 20, scale = 2)
    private BigDecimal annualRevenue;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 30)
    private AccountType accountType;

    @Column(name = "owner_id", nullable = false, length = 100)
    private String ownerId;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    protected Account() {}

    public static Account of(String code, String name, String businessNo, String industry,
                             String website, String phone, String address, Integer employeeCount,
                             BigDecimal annualRevenue, AccountType accountType, String ownerId) {
        Account a = new Account();
        a.code = code;
        a.name = name;
        a.businessNo = businessNo;
        a.industry = industry;
        a.website = website;
        a.phone = phone;
        a.address = address;
        a.employeeCount = employeeCount;
        a.annualRevenue = annualRevenue;
        a.accountType = accountType;
        a.ownerId = ownerId;
        a.isActive = true;
        return a;
    }

    public void update(String name, String businessNo, String industry, String website,
                       String phone, String address, Integer employeeCount,
                       BigDecimal annualRevenue, AccountType accountType, String ownerId) {
        this.name = name;
        this.businessNo = businessNo;
        this.industry = industry;
        this.website = website;
        this.phone = phone;
        this.address = address;
        this.employeeCount = employeeCount;
        this.annualRevenue = annualRevenue;
        this.accountType = accountType;
        this.ownerId = ownerId;
    }

    public void deactivate() { this.isActive = false; }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getBusinessNo() { return businessNo; }
    public String getIndustry() { return industry; }
    public String getWebsite() { return website; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public Integer getEmployeeCount() { return employeeCount; }
    public BigDecimal getAnnualRevenue() { return annualRevenue; }
    public AccountType getAccountType() { return accountType; }
    public String getOwnerId() { return ownerId; }
    public boolean isActive() { return isActive; }
}
