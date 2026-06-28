package com.erp.finance.domain.model;

import com.erp.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.hibernate.annotations.SQLRestriction;

/**
 * 환율 — from_currency 1단위를 to_currency로 환산하는 비율(발효일 기준). 조회는 effectiveDate ≤ 조회일 중 최신을 사용한다. 수동
 * 등록·외부 provider 수집 양쪽으로 채워진다.
 */
@Entity
@Table(name = "exchange_rate", schema = "finance")
@SQLRestriction("deleted_at IS NULL")
public class ExchangeRate extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "exchange_rate_seq")
  @SequenceGenerator(
      name = "exchange_rate_seq",
      sequenceName = "finance.exchange_rate_id_seq",
      allocationSize = 50)
  private Long id;

  @Column(name = "from_currency", nullable = false, length = 3)
  private String fromCurrency;

  @Column(name = "to_currency", nullable = false, length = 3)
  private String toCurrency;

  @Column(name = "effective_date", nullable = false)
  private LocalDate effectiveDate;

  @Column(name = "rate", nullable = false, precision = 18, scale = 8)
  private BigDecimal rate;

  protected ExchangeRate() {}

  public static ExchangeRate of(
      String fromCurrency, String toCurrency, LocalDate effectiveDate, BigDecimal rate) {
    ExchangeRate entity = new ExchangeRate();
    entity.fromCurrency = fromCurrency;
    entity.toCurrency = toCurrency;
    entity.effectiveDate = effectiveDate;
    entity.rate = rate;
    return entity;
  }

  public Long getId() {
    return id;
  }

  public String getFromCurrency() {
    return fromCurrency;
  }

  public String getToCurrency() {
    return toCurrency;
  }

  public LocalDate getEffectiveDate() {
    return effectiveDate;
  }

  public BigDecimal getRate() {
    return rate;
  }
}
