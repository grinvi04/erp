package com.erp.finance.domain.model;

import com.erp.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

  // 손상차손액(IMPAIRMENT) 또는 환입액(REVERSAL) — 부호는 양수, 의미는 entryType으로 구분.
  @Column(name = "impairment_loss", nullable = false, precision = 20, scale = 2)
  private BigDecimal impairmentLoss;

  @Enumerated(EnumType.STRING)
  @Column(name = "entry_type", nullable = false, length = 20)
  private ImpairmentEntryType entryType;

  @Column(name = "journal_entry_id")
  private Long journalEntryId;

  protected ImpairmentEntry() {}

  /** 손상 인식 이력. */
  public static ImpairmentEntry of(
      Long fixedAssetId,
      Long fiscalPeriodId,
      BigDecimal recoverableAmount,
      BigDecimal bookValueBefore,
      BigDecimal impairmentLoss,
      Long journalEntryId) {
    return create(
        fixedAssetId,
        fiscalPeriodId,
        recoverableAmount,
        bookValueBefore,
        impairmentLoss,
        ImpairmentEntryType.IMPAIRMENT,
        journalEntryId);
  }

  /** 손상 환입 이력 — impairmentLoss에 환입액(양수), bookValueBefore는 환입 전 장부가액. */
  public static ImpairmentEntry reversal(
      Long fixedAssetId,
      Long fiscalPeriodId,
      BigDecimal recoverableAmount,
      BigDecimal bookValueBefore,
      BigDecimal reversalAmount,
      Long journalEntryId) {
    return create(
        fixedAssetId,
        fiscalPeriodId,
        recoverableAmount,
        bookValueBefore,
        reversalAmount,
        ImpairmentEntryType.REVERSAL,
        journalEntryId);
  }

  private static ImpairmentEntry create(
      Long fixedAssetId,
      Long fiscalPeriodId,
      BigDecimal recoverableAmount,
      BigDecimal bookValueBefore,
      BigDecimal impairmentLoss,
      ImpairmentEntryType entryType,
      Long journalEntryId) {
    ImpairmentEntry e = new ImpairmentEntry();
    e.fixedAssetId = fixedAssetId;
    e.fiscalPeriodId = fiscalPeriodId;
    e.recoverableAmount = recoverableAmount;
    e.bookValueBefore = bookValueBefore;
    e.impairmentLoss = impairmentLoss;
    e.entryType = entryType;
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

  public ImpairmentEntryType getEntryType() {
    return entryType;
  }

  public Long getJournalEntryId() {
    return journalEntryId;
  }
}
