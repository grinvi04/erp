package com.erp.crm.domain.model;

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
@Table(name = "contact", schema = "crm")
public class Contact extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "crm_contact_seq")
    @SequenceGenerator(name = "crm_contact_seq", sequenceName = "crm.contact_id_seq", allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false, updatable = false)
    private Account account;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "title", length = 100)
    private String title;

    @Column(name = "department", length = 100)
    private String department;

    @Column(name = "email", length = 200)
    private String email;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "mobile", length = 30)
    private String mobile;

    @Column(name = "is_primary", nullable = false)
    private boolean isPrimary;

    protected Contact() {}

    public static Contact of(Account account, String lastName, String firstName, String title,
                             String department, String email, String phone, String mobile,
                             boolean isPrimary) {
        Contact c = new Contact();
        c.account = account;
        c.lastName = lastName;
        c.firstName = firstName;
        c.title = title;
        c.department = department;
        c.email = email;
        c.phone = phone;
        c.mobile = mobile;
        c.isPrimary = isPrimary;
        return c;
    }

    public void update(String lastName, String firstName, String title, String department,
                       String email, String phone, String mobile, boolean isPrimary) {
        this.lastName = lastName;
        this.firstName = firstName;
        this.title = title;
        this.department = department;
        this.email = email;
        this.phone = phone;
        this.mobile = mobile;
        this.isPrimary = isPrimary;
    }

    public Long getId() { return id; }
    public Account getAccount() { return account; }
    public String getLastName() { return lastName; }
    public String getFirstName() { return firstName; }
    public String getTitle() { return title; }
    public String getDepartment() { return department; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getMobile() { return mobile; }
    public boolean isPrimary() { return isPrimary; }
}
