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
}
