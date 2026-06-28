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
import java.math.BigDecimal;
import org.hibernate.annotations.SQLRestriction;

/**
 * AP 전표 라인 — 차변 계정(비용/자산·부가세대급금 등)과 금액. 승인 시 GL 분개의 차변이 된다 (대변은 공급업체 외상매입금 통제계정). 실무: 비용/세액을 라인별
 * 계정으로 코딩한다.
 */
@Entity
@Table(name = "ap_invoice_line", schema = "finance")
@SQLRestriction("deleted_at IS NULL")
public class ApInvoiceLine extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ap_invoice_line_seq")
  @SequenceGenerator(
      name = "ap_invoice_line_seq",
      sequenceName = "finance.ap_invoice_line_id_seq",
      allocationSize = 50)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ap_invoice_id", nullable = false)
  private ApInvoice apInvoice;

  @Column(name = "line_no", nullable = false)
  private int lineNo;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "account_id", nullable = false)
  private Account account;

  @Column(name = "amount", nullable = false)
  private BigDecimal amount;

  @Column(name = "description", length = 500)
  private String description;

  protected ApInvoiceLine() {}

  static ApInvoiceLine of(
      ApInvoice apInvoice, int lineNo, Account account, BigDecimal amount, String description) {
    ApInvoiceLine line = new ApInvoiceLine();
    line.apInvoice = apInvoice;
    line.lineNo = lineNo;
    line.account = account;
    line.amount = amount;
    line.description = description;
    return line;
  }

  public Long getId() {
    return id;
  }

  public int getLineNo() {
    return lineNo;
  }

  public Account getAccount() {
    return account;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public String getDescription() {
    return description;
  }
}
