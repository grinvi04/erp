package com.erp.crm.domain.model;

import com.erp.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "lead", schema = "crm")
@SQLRestriction("deleted_at IS NULL")
public class Lead extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "crm_lead_seq")
  @SequenceGenerator(name = "crm_lead_seq", sequenceName = "crm.lead_id_seq", allocationSize = 50)
  private Long id;

  @Column(name = "last_name", nullable = false, length = 50)
  private String lastName;

  @Column(name = "first_name", nullable = false, length = 50)
  private String firstName;

  @Column(name = "company", length = 200)
  private String company;

  @Column(name = "title", length = 100)
  private String title;

  @Column(name = "email", length = 200)
  private String email;

  @Column(name = "phone", length = 30)
  private String phone;

  @Column(name = "source", length = 50)
  private String source;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 30)
  private LeadStatus status;

  @Column(name = "owner_id", nullable = false, length = 100)
  private String ownerId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "converted_account_id")
  private Account convertedAccount;

  @Column(name = "converted_opportunity_id")
  private Long convertedOpportunityId;

  @Column(name = "converted_at")
  private LocalDateTime convertedAt;

  @Column(name = "note", columnDefinition = "TEXT")
  private String note;

  protected Lead() {}

  public static Lead of(
      String lastName,
      String firstName,
      String company,
      String title,
      String email,
      String phone,
      String source,
      String ownerId,
      String note) {
    Lead l = new Lead();
    l.lastName = lastName;
    l.firstName = firstName;
    l.company = company;
    l.title = title;
    l.email = email;
    l.phone = phone;
    l.source = source;
    l.status = LeadStatus.NEW;
    l.ownerId = ownerId;
    l.note = note;
    return l;
  }

  public void update(
      String lastName,
      String firstName,
      String company,
      String title,
      String email,
      String phone,
      String source,
      String ownerId,
      String note) {
    this.lastName = lastName;
    this.firstName = firstName;
    this.company = company;
    this.title = title;
    this.email = email;
    this.phone = phone;
    this.source = source;
    this.ownerId = ownerId;
    this.note = note;
  }

  public void qualify() {
    this.status = LeadStatus.QUALIFIED;
  }

  public void contact() {
    this.status = LeadStatus.CONTACTED;
  }

  public void disqualify() {
    this.status = LeadStatus.DISQUALIFIED;
  }

  public void convert(Account account, Long opportunityId) {
    this.status = LeadStatus.CONVERTED;
    this.convertedAccount = account;
    this.convertedOpportunityId = opportunityId;
    this.convertedAt = LocalDateTime.now();
  }

  public boolean isConverted() {
    return this.status == LeadStatus.CONVERTED;
  }

  public Long getId() {
    return id;
  }

  public String getLastName() {
    return lastName;
  }

  public String getFirstName() {
    return firstName;
  }

  public String getCompany() {
    return company;
  }

  public String getTitle() {
    return title;
  }

  public String getEmail() {
    return email;
  }

  public String getPhone() {
    return phone;
  }

  public String getSource() {
    return source;
  }

  public LeadStatus getStatus() {
    return status;
  }

  public String getOwnerId() {
    return ownerId;
  }

  public Account getConvertedAccount() {
    return convertedAccount;
  }

  public Long getConvertedOpportunityId() {
    return convertedOpportunityId;
  }

  public LocalDateTime getConvertedAt() {
    return convertedAt;
  }

  public String getNote() {
    return note;
  }
}
