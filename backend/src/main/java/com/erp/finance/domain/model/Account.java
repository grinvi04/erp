package com.erp.finance.domain.model;

import com.erp.common.audit.BaseEntity;
import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
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

@Entity
@Table(name = "account", schema = "finance")
public class Account extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "account_seq")
  @SequenceGenerator(
      name = "account_seq",
      sequenceName = "finance.account_id_seq",
      allocationSize = 50)
  private Long id;

  @Column(name = "code", nullable = false, length = 20)
  private String code;

  @Column(name = "name", nullable = false, length = 200)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(name = "account_type", nullable = false, length = 30)
  private AccountType accountType;

  @Enumerated(EnumType.STRING)
  @Column(name = "normal_balance", nullable = false, length = 10)
  private NormalBalance normalBalance;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_id")
  private Account parent;

  @Column(name = "is_summary", nullable = false)
  private boolean isSummary;

  @Column(name = "is_active", nullable = false)
  private boolean isActive;

  protected Account() {}

  public static Account of(
      String code,
      String name,
      AccountType accountType,
      NormalBalance normalBalance,
      Account parent,
      boolean isSummary) {
    Account a = new Account();
    a.code = code;
    a.name = name;
    a.accountType = accountType;
    a.normalBalance = normalBalance;
    a.parent = parent;
    a.isSummary = isSummary;
    a.isActive = true;
    return a;
  }

  public void update(String name, boolean isSummary) {
    this.name = name;
    this.isSummary = isSummary;
  }

  public void deactivate() {
    this.isActive = false;
  }

  public void assertPostable() {
    if (isSummary) {
      throw new ErpException(ErrorCode.ACCOUNT_IS_SUMMARY);
    }
    if (!isActive) {
      throw new ErpException(ErrorCode.ACCOUNT_NOT_FOUND);
    }
  }

  public Long getId() {
    return id;
  }

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public AccountType getAccountType() {
    return accountType;
  }

  public NormalBalance getNormalBalance() {
    return normalBalance;
  }

  public Account getParent() {
    return parent;
  }

  public boolean isSummary() {
    return isSummary;
  }

  public boolean isActive() {
    return isActive;
  }
}
