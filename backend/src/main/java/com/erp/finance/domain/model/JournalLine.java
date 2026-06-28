package com.erp.finance.domain.model;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
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
import org.hibernate.annotations.TenantId;

@Entity
@Table(name = "journal_line", schema = "finance")
public class JournalLine {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "journal_line_seq")
  @SequenceGenerator(
      name = "journal_line_seq",
      sequenceName = "finance.journal_line_id_seq",
      allocationSize = 200)
  private Long id;

  @TenantId
  @Column(name = "tenant_id", nullable = false, updatable = false)
  private Long tenantId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "journal_entry_id", nullable = false)
  private JournalEntry journalEntry;

  @Column(name = "line_no", nullable = false)
  private int lineNo;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "account_id", nullable = false)
  private Account account;

  @Column(name = "debit_amount", nullable = false, precision = 20, scale = 2)
  private BigDecimal debitAmount;

  @Column(name = "credit_amount", nullable = false, precision = 20, scale = 2)
  private BigDecimal creditAmount;

  @Column(name = "description", length = 500)
  private String description;

  @Column(name = "department_id")
  private Long departmentId;

  protected JournalLine() {}

  public static JournalLine of(
      JournalEntry entry,
      int lineNo,
      Account account,
      BigDecimal debitAmount,
      BigDecimal creditAmount,
      String description,
      Long departmentId) {
    BigDecimal debit = debitAmount != null ? debitAmount : BigDecimal.ZERO;
    BigDecimal credit = creditAmount != null ? creditAmount : BigDecimal.ZERO;
    if (debit.compareTo(BigDecimal.ZERO) > 0 && credit.compareTo(BigDecimal.ZERO) > 0) {
      throw new ErpException(ErrorCode.JOURNAL_LINE_AMOUNTS_INVALID);
    }
    JournalLine line = new JournalLine();
    line.journalEntry = entry;
    line.lineNo = lineNo;
    line.account = account;
    line.debitAmount = debit;
    line.creditAmount = credit;
    line.description = description;
    line.departmentId = departmentId;
    return line;
  }

  public Long getId() {
    return id;
  }

  public Long getTenantId() {
    return tenantId;
  }

  public JournalEntry getJournalEntry() {
    return journalEntry;
  }

  public int getLineNo() {
    return lineNo;
  }

  public Account getAccount() {
    return account;
  }

  public BigDecimal getDebitAmount() {
    return debitAmount;
  }

  public BigDecimal getCreditAmount() {
    return creditAmount;
  }

  public String getDescription() {
    return description;
  }

  public Long getDepartmentId() {
    return departmentId;
  }
}
