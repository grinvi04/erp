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
import org.hibernate.annotations.SQLRestriction;

/** 감가상각 이력 — 자산별·회계기간별 상각 1건. (자산,기간) UNIQUE로 같은 기간 중복 상각을 DB에서 막는다(멱등). 생성된 GL 분개를 역참조한다. */
@Entity
@Table(name = "depreciation_entry", schema = "finance")
@SQLRestriction("deleted_at IS NULL")
public class DepreciationEntry extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "depreciation_entry_seq")
  @SequenceGenerator(
      name = "depreciation_entry_seq",
      sequenceName = "finance.depreciation_entry_id_seq",
      allocationSize = 50)
  private Long id;

  @Column(name = "fixed_asset_id", nullable = false)
  private Long fixedAssetId;

  @Column(name = "fiscal_period_id", nullable = false)
  private Long fiscalPeriodId;

  @Column(name = "amount", nullable = false, precision = 20, scale = 2)
  private BigDecimal amount;

  @Column(name = "journal_entry_id")
  private Long journalEntryId;

  protected DepreciationEntry() {}

  public static DepreciationEntry of(
      Long fixedAssetId, Long fiscalPeriodId, BigDecimal amount, Long journalEntryId) {
    DepreciationEntry e = new DepreciationEntry();
    e.fixedAssetId = fixedAssetId;
    e.fiscalPeriodId = fiscalPeriodId;
    e.amount = amount;
    e.journalEntryId = journalEntryId;
    return e;
  }

  public Long getId() {
    return id;
  }

  public Long getFixedAssetId() {
    return fixedAssetId;
  }

  public Long getFiscalPeriodId() {
    return fiscalPeriodId;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public Long getJournalEntryId() {
    return journalEntryId;
  }
}
