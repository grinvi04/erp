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

/**
 * 손상차손 이력 — 자산별·회계기간별 손상 인식 1건. (자산,기간) UNIQUE로 같은 기간 중복 인식을 DB에서 막는다(멱등). 인식 전 장부가액·회수가능액·손상차손액을
 * 함께 보관하고 생성된 GL 분개를 역참조한다.
 */
@Entity
@Table(name = "impairment_entry", schema = "finance")
@SQLRestriction("deleted_at IS NULL")
public class ImpairmentEntry extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "impairment_entry_seq")
  @SequenceGenerator(
      name = "impairment_entry_seq",
      sequenceName = "finance.impairment_entry_id_seq",
      allocationSize = 50)
  private Long id;

  @Column(name = "fixed_asset_id", nullable = false)
  private Long fixedAssetId;

  @Column(name = "fiscal_period_id", nullable = false)
  private Long fiscalPeriodId;

  @Column(name = "recoverable_amount", nullable = false, precision = 20, scale = 2)
  private BigDecimal recoverableAmount;

  @Column(name = "book_value_before", nullable = false, precision = 20, scale = 2)
  private BigDecimal bookValueBefore;

  @Column(name = "impairment_loss", nullable = false, precision = 20, scale = 2)
  private BigDecimal impairmentLoss;

  @Column(name = "journal_entry_id")
  private Long journalEntryId;

  protected ImpairmentEntry() {}

  public static ImpairmentEntry of(
      Long fixedAssetId,
      Long fiscalPeriodId,
      BigDecimal recoverableAmount,
      BigDecimal bookValueBefore,
      BigDecimal impairmentLoss,
      Long journalEntryId) {
    ImpairmentEntry e = new ImpairmentEntry();
    e.fixedAssetId = fixedAssetId;
    e.fiscalPeriodId = fiscalPeriodId;
    e.recoverableAmount = recoverableAmount;
    e.bookValueBefore = bookValueBefore;
    e.impairmentLoss = impairmentLoss;
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

  public BigDecimal getRecoverableAmount() {
    return recoverableAmount;
  }

  public BigDecimal getBookValueBefore() {
    return bookValueBefore;
  }

  public BigDecimal getImpairmentLoss() {
    return impairmentLoss;
  }

  public Long getJournalEntryId() {
    return journalEntryId;
  }
}
