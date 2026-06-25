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

/**
 * AR 전표 라인 — 대변 계정(매출·부가세예수금 등)과 금액. 승인 시 GL 분개의 대변이 된다
 * (차변은 고객 외상매출금 통제계정). 실무: 매출/세액을 라인별 계정으로 코딩한다.
 */
@Entity
@Table(name = "ar_invoice_line", schema = "finance")
public class ArInvoiceLine extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ar_invoice_line_seq")
    @SequenceGenerator(name = "ar_invoice_line_seq",
        sequenceName = "finance.ar_invoice_line_id_seq", allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ar_invoice_id", nullable = false)
    private ArInvoice arInvoice;

    @Column(name = "line_no", nullable = false)
    private int lineNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "description", length = 500)
    private String description;

    protected ArInvoiceLine() {}

    static ArInvoiceLine of(ArInvoice arInvoice, int lineNo, Account account,
                            BigDecimal amount, String description) {
        ArInvoiceLine line = new ArInvoiceLine();
        line.arInvoice = arInvoice;
        line.lineNo = lineNo;
        line.account = account;
        line.amount = amount;
        line.description = description;
        return line;
    }

    public Long getId() { return id; }
    public int getLineNo() { return lineNo; }
    public Account getAccount() { return account; }
    public BigDecimal getAmount() { return amount; }
    public String getDescription() { return description; }
}
