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
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "opportunity", schema = "crm")
public class Opportunity extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "crm_opportunity_seq")
  @SequenceGenerator(
      name = "crm_opportunity_seq",
      sequenceName = "crm.opportunity_id_seq",
      allocationSize = 50)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "account_id", nullable = false)
  private Account account;

  @Column(name = "name", nullable = false, length = 200)
  private String name;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "stage_id", nullable = false)
  private PipelineStage stage;

  @Column(name = "amount", precision = 20, scale = 2)
  private BigDecimal amount;

  @Column(name = "currency", nullable = false, length = 3)
  private String currency;

  // 거래 시점 FX 스냅샷 — 생성 시 환율로 amount를 환산해 고정(환율 변경에 불변). 부재·금액미정 시 null.
  @Column(name = "base_amount", precision = 20, scale = 2)
  private BigDecimal baseAmount;

  @Column(name = "exchange_rate", precision = 18, scale = 8)
  private BigDecimal exchangeRate;

  @Column(name = "close_date")
  private LocalDate closeDate;

  @Column(name = "probability", nullable = false)
  private int probability;

  @Column(name = "owner_id", nullable = false, length = 100)
  private String ownerId;

  @Column(name = "source", length = 50)
  private String source;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  protected Opportunity() {}

  public static Opportunity of(
      Account account,
      String name,
      PipelineStage stage,
      BigDecimal amount,
      String currency,
      LocalDate closeDate,
      int probability,
      String ownerId,
      String source,
      String description) {
    Opportunity o = new Opportunity();
    o.account = account;
    o.name = name;
    o.stage = stage;
    o.amount = amount;
    o.currency = currency != null ? currency : "KRW";
    o.closeDate = closeDate;
    o.probability = probability;
    o.ownerId = ownerId;
    o.source = source;
    o.description = description;
    return o;
  }

  public void update(
      String name,
      PipelineStage stage,
      BigDecimal amount,
      String currency,
      LocalDate closeDate,
      int probability,
      String ownerId,
      String source,
      String description) {
    this.name = name;
    this.stage = stage;
    this.amount = amount;
    this.currency = currency != null ? currency : "KRW";
    this.closeDate = closeDate;
    this.probability = probability;
    this.ownerId = ownerId;
    this.source = source;
    this.description = description;
  }

  /** 거래 시점 환산 스냅샷 적용(생성 시 1회). 환율 부재·금액 미정이면 호출되지 않아 null로 남는다. */
  public void applyBaseSnapshot(BigDecimal baseAmount, BigDecimal exchangeRate) {
    this.baseAmount = baseAmount;
    this.exchangeRate = exchangeRate;
  }

  public Long getId() {
    return id;
  }

  public Account getAccount() {
    return account;
  }

  public String getName() {
    return name;
  }

  public PipelineStage getStage() {
    return stage;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public String getCurrency() {
    return currency;
  }

  public BigDecimal getBaseAmount() {
    return baseAmount;
  }

  public BigDecimal getExchangeRate() {
    return exchangeRate;
  }

  public LocalDate getCloseDate() {
    return closeDate;
  }

  public int getProbability() {
    return probability;
  }

  public String getOwnerId() {
    return ownerId;
  }

  public String getSource() {
    return source;
  }

  public String getDescription() {
    return description;
  }
}
