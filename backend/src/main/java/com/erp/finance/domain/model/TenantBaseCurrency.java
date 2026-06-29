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
import org.hibernate.annotations.SQLRestriction;

/**
 * 테넌트별 기준통화 — 혼합통화 거래를 합산·환산하는 기준. 테넌트당 1행(tenant_id UNIQUE). 미설정 테넌트는 서비스가 KRW를 기본으로 반환한다(행 없음 =
 * KRW).
 */
@Entity
@Table(name = "tenant_base_currency", schema = "finance")
@SQLRestriction("deleted_at IS NULL")
public class TenantBaseCurrency extends BaseEntity {

  public static final String DEFAULT_BASE_CURRENCY = "KRW";

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tenant_base_currency_seq")
  @SequenceGenerator(
      name = "tenant_base_currency_seq",
      sequenceName = "finance.tenant_base_currency_id_seq",
      allocationSize = 10)
  private Long id;

  @Column(name = "base_currency", nullable = false, length = 3)
  private String baseCurrency;

  // 실현 환차손익 계정 — 외화 결제 시 결제환율≠인보이스환율 차액을 분개할 계정(테넌트 설정).
  // 둘 다 설정돼야 환차 분개를 적용하며, 미설정이면 환차 라인을 생략한다(폴백).
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "fx_gain_account_id")
  private Account fxGainAccount;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "fx_loss_account_id")
  private Account fxLossAccount;

  // 부가세 통제계정 — 부가세대급금(매입, AP)·부가세예수금(매출, AR). 각각 독립적으로 쓰며,
  // 미설정이면 해당 인보이스 부가세 라인을 생략한다(폴백).
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "vat_receivable_account_id")
  private Account vatReceivableAccount;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "vat_payable_account_id")
  private Account vatPayableAccount;

  // 고정자산 감가상각·처분 계정 — 감가상각비(비용)·감가상각누계액(자산 차감)·처분이익(수익)·처분손실(비용).
  // 미설정이면 상각/처분 분개를 차단한다(빈 값 분개 금지).
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "depreciation_expense_account_id")
  private Account depreciationExpenseAccount;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "accumulated_depreciation_account_id")
  private Account accumulatedDepreciationAccount;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "disposal_gain_account_id")
  private Account disposalGainAccount;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "disposal_loss_account_id")
  private Account disposalLossAccount;

  // 손상차손 계정 — 손상차손비(비용)·손상차손누계액(자산 차감). 둘 다 설정돼야 손상 분개를 적용한다(미설정 시 차단).
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "impairment_loss_account_id")
  private Account impairmentLossAccount;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "accumulated_impairment_account_id")
  private Account accumulatedImpairmentAccount;

  protected TenantBaseCurrency() {}

  public static TenantBaseCurrency of(String baseCurrency) {
    TenantBaseCurrency entity = new TenantBaseCurrency();
    entity.baseCurrency = baseCurrency;
    return entity;
  }

  public void changeBaseCurrency(String baseCurrency) {
    this.baseCurrency = baseCurrency;
  }

  /** 환차손익 계정 설정(둘 다 함께 지정·해제). null이면 미설정 → 환차 분개 폴백. */
  public void assignFxAccounts(Account fxGainAccount, Account fxLossAccount) {
    this.fxGainAccount = fxGainAccount;
    this.fxLossAccount = fxLossAccount;
  }

  /** 부가세 통제계정 설정 — 부가세대급금(매입)·부가세예수금(매출). 각각 nullable(미설정 시 부가세 분개 폴백). */
  public void assignVatAccounts(Account vatReceivableAccount, Account vatPayableAccount) {
    this.vatReceivableAccount = vatReceivableAccount;
    this.vatPayableAccount = vatPayableAccount;
  }

  public Long getId() {
    return id;
  }

  public String getBaseCurrency() {
    return baseCurrency;
  }

  public Account getFxGainAccount() {
    return fxGainAccount;
  }

  public Account getFxLossAccount() {
    return fxLossAccount;
  }

  public Account getVatReceivableAccount() {
    return vatReceivableAccount;
  }

  public Account getVatPayableAccount() {
    return vatPayableAccount;
  }

  /** 감가상각·처분 계정 설정(함께 지정·해제). 각 nullable — 미설정 시 상각/처분 분개 차단. */
  public void assignDepreciationAccounts(
      Account depreciationExpenseAccount,
      Account accumulatedDepreciationAccount,
      Account disposalGainAccount,
      Account disposalLossAccount) {
    this.depreciationExpenseAccount = depreciationExpenseAccount;
    this.accumulatedDepreciationAccount = accumulatedDepreciationAccount;
    this.disposalGainAccount = disposalGainAccount;
    this.disposalLossAccount = disposalLossAccount;
  }

  public Account getDepreciationExpenseAccount() {
    return depreciationExpenseAccount;
  }

  public Account getAccumulatedDepreciationAccount() {
    return accumulatedDepreciationAccount;
  }

  public Account getDisposalGainAccount() {
    return disposalGainAccount;
  }

  public Account getDisposalLossAccount() {
    return disposalLossAccount;
  }

  /** 손상차손 계정 설정(함께 지정·해제). 각 nullable — 미설정 시 손상 분개 차단. */
  public void assignImpairmentAccounts(
      Account impairmentLossAccount, Account accumulatedImpairmentAccount) {
    this.impairmentLossAccount = impairmentLossAccount;
    this.accumulatedImpairmentAccount = accumulatedImpairmentAccount;
  }

  public Account getImpairmentLossAccount() {
    return impairmentLossAccount;
  }

  public Account getAccumulatedImpairmentAccount() {
    return accumulatedImpairmentAccount;
  }
}
