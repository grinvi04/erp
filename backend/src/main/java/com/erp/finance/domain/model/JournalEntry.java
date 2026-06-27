package com.erp.finance.domain.model;

import com.erp.common.audit.BaseEntity;
import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "journal_entry", schema = "finance")
public class JournalEntry extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "journal_entry_seq")
  @SequenceGenerator(
      name = "journal_entry_seq",
      sequenceName = "finance.journal_entry_id_seq",
      allocationSize = 50)
  private Long id;

  @Column(name = "entry_no", nullable = false, length = 30)
  private String entryNo;

  @Column(name = "entry_date", nullable = false)
  private LocalDate entryDate;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "fiscal_period_id", nullable = false)
  private FiscalPeriod fiscalPeriod;

  @Column(name = "description", nullable = false, length = 500)
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(name = "entry_type", nullable = false, length = 30)
  private JournalEntryType entryType;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private JournalEntryStatus status;

  @Column(name = "total_debit", nullable = false, precision = 20, scale = 2)
  private BigDecimal totalDebit;

  @Column(name = "total_credit", nullable = false, precision = 20, scale = 2)
  private BigDecimal totalCredit;

  @Column(name = "currency", nullable = false, length = 3)
  private String currency;

  // 거래 시점 FX 스냅샷 — 생성 시 환율로 차변합계를 환산해 고정(환율 변경에 불변). 부재 시 null(미산정).
  @Column(name = "base_amount", precision = 20, scale = 2)
  private BigDecimal baseAmount;

  @Column(name = "exchange_rate", precision = 18, scale = 8)
  private BigDecimal exchangeRate;

  @Column(name = "reference_type", length = 100)
  private String referenceType;

  @Column(name = "reference_id")
  private Long referenceId;

  @Column(name = "posted_at")
  private LocalDateTime postedAt;

  @Column(name = "posted_by", length = 100)
  private String postedBy;

  @Column(name = "approval_request_id")
  private Long approvalRequestId;

  @OneToMany(
      mappedBy = "journalEntry",
      cascade = CascadeType.ALL,
      fetch = FetchType.LAZY,
      orphanRemoval = true)
  @OrderBy("lineNo ASC")
  private List<JournalLine> lines = new ArrayList<>();

  protected JournalEntry() {}

  public static JournalEntry create(
      String entryNo,
      LocalDate entryDate,
      FiscalPeriod fiscalPeriod,
      String description,
      JournalEntryType entryType,
      String currency) {
    JournalEntry je = new JournalEntry();
    je.entryNo = entryNo;
    je.entryDate = entryDate;
    je.fiscalPeriod = fiscalPeriod;
    je.description = description;
    je.entryType = entryType;
    je.status = JournalEntryStatus.DRAFT;
    je.totalDebit = BigDecimal.ZERO;
    je.totalCredit = BigDecimal.ZERO;
    je.currency = currency != null ? currency : "KRW";
    return je;
  }

  public void addLine(JournalLine line) {
    lines.add(line);
    totalDebit = totalDebit.add(line.getDebitAmount());
    totalCredit = totalCredit.add(line.getCreditAmount());
  }

  public boolean isBalanced() {
    return totalDebit.compareTo(totalCredit) == 0 && totalDebit.compareTo(BigDecimal.ZERO) > 0;
  }

  /**
   * 결재 상신: DRAFT → PENDING_APPROVAL. 차대변 균형·회계기간 open을 상신 시점에 검증한다 (전기 전 게이트). 전기(post)는 결재 승인 후
   * PENDING_APPROVAL에서만 가능 — 직접 전기 차단.
   */
  public void submitForApproval() {
    if (status != JournalEntryStatus.DRAFT) {
      throw new ErpException(ErrorCode.JOURNAL_ENTRY_NOT_DRAFT);
    }
    if (!isBalanced()) {
      throw new ErpException(ErrorCode.JOURNAL_ENTRY_NOT_BALANCED);
    }
    if (!fiscalPeriod.isOpen()) {
      throw new ErpException(ErrorCode.FISCAL_PERIOD_CLOSED);
    }
    this.status = JournalEntryStatus.PENDING_APPROVAL;
  }

  /**
   * 결재 반려·철회 시 되돌리기: PENDING_APPROVAL → DRAFT. 되돌린 전표는 수정 후 재상신할 수 있다. 상신되지 않은(PENDING_APPROVAL 아님)
   * 전표는 되돌릴 수 없다.
   */
  public void returnToDraft() {
    if (status != JournalEntryStatus.PENDING_APPROVAL) {
      throw new ErpException(ErrorCode.JOURNAL_ENTRY_NOT_PENDING_APPROVAL);
    }
    this.status = JournalEntryStatus.DRAFT;
  }

  public void post(String postedBy) {
    // 직무분리: 작성자가 DRAFT를 직접 전기할 수 없다 — 결재 상신(PENDING_APPROVAL) 후에만 전기.
    if (status != JournalEntryStatus.PENDING_APPROVAL) {
      throw new ErpException(ErrorCode.JOURNAL_ENTRY_NOT_PENDING_APPROVAL);
    }
    if (!isBalanced()) {
      throw new ErpException(ErrorCode.JOURNAL_ENTRY_NOT_BALANCED);
    }
    if (!fiscalPeriod.isOpen()) {
      throw new ErpException(ErrorCode.FISCAL_PERIOD_CLOSED);
    }
    this.status = JournalEntryStatus.POSTED;
    this.postedAt = LocalDateTime.now();
    this.postedBy = postedBy;
  }

  /** 거래 시점 환산 스냅샷 적용(생성 시 1회). 환율 부재면 호출하지 않아 null(미산정)로 남는다. */
  public void applyBaseSnapshot(BigDecimal baseAmount, BigDecimal exchangeRate) {
    this.baseAmount = baseAmount;
    this.exchangeRate = exchangeRate;
  }

  public void linkApprovalRequest(Long approvalRequestId) {
    this.approvalRequestId = approvalRequestId;
  }

  public void markReversed() {
    this.status = JournalEntryStatus.REVERSED;
  }

  public void linkReference(String referenceType, Long referenceId) {
    this.referenceType = referenceType;
    this.referenceId = referenceId;
  }

  public Long getId() {
    return id;
  }

  public String getEntryNo() {
    return entryNo;
  }

  public LocalDate getEntryDate() {
    return entryDate;
  }

  public FiscalPeriod getFiscalPeriod() {
    return fiscalPeriod;
  }

  public String getDescription() {
    return description;
  }

  public JournalEntryType getEntryType() {
    return entryType;
  }

  public JournalEntryStatus getStatus() {
    return status;
  }

  public BigDecimal getTotalDebit() {
    return totalDebit;
  }

  public BigDecimal getTotalCredit() {
    return totalCredit;
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

  public String getReferenceType() {
    return referenceType;
  }

  public Long getReferenceId() {
    return referenceId;
  }

  public LocalDateTime getPostedAt() {
    return postedAt;
  }

  public String getPostedBy() {
    return postedBy;
  }

  public Long getApprovalRequestId() {
    return approvalRequestId;
  }

  public List<JournalLine> getLines() {
    return lines;
  }
}
