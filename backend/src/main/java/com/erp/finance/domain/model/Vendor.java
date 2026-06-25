package com.erp.finance.domain.model;

import com.erp.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "vendor", schema = "finance")
public class Vendor extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "vendor_seq")
    @SequenceGenerator(name = "vendor_seq", sequenceName = "finance.vendor_id_seq", allocationSize = 50)
    private Long id;

    @Column(name = "code", nullable = false, length = 30)
    private String code;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "business_no", length = 30)
    private String businessNo;

    @Column(name = "contact_name", length = 100)
    private String contactName;

    @Column(name = "contact_email", length = 200)
    private String contactEmail;

    @Column(name = "contact_phone", length = 30)
    private String contactPhone;

    @Column(name = "payment_terms", nullable = false)
    private int paymentTerms;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    // 외상매입금 통제계정(대변) — AP 보조원장이 자동 전기되는 GL 통제계정(실무: reconciliation account).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payables_account_id")
    private Account payablesAccount;

    protected Vendor() {}

    public static Vendor of(String code, String name, String businessNo,
                             String contactName, String contactEmail, String contactPhone,
                             int paymentTerms) {
        Vendor v = new Vendor();
        v.code = code;
        v.name = name;
        v.businessNo = businessNo;
        v.contactName = contactName;
        v.contactEmail = contactEmail;
        v.contactPhone = contactPhone;
        v.paymentTerms = paymentTerms;
        v.isActive = true;
        return v;
    }

    public void update(String name, String businessNo, String contactName,
                       String contactEmail, String contactPhone, int paymentTerms) {
        this.name = name;
        this.businessNo = businessNo;
        this.contactName = contactName;
        this.contactEmail = contactEmail;
        this.contactPhone = contactPhone;
        this.paymentTerms = paymentTerms;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void assignPayablesAccount(Account payablesAccount) {
        this.payablesAccount = payablesAccount;
    }

    public Long getId() { return id; }
    public Account getPayablesAccount() { return payablesAccount; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getBusinessNo() { return businessNo; }
    public String getContactName() { return contactName; }
    public String getContactEmail() { return contactEmail; }
    public String getContactPhone() { return contactPhone; }
    public int getPaymentTerms() { return paymentTerms; }
    public boolean isActive() { return isActive; }
}
