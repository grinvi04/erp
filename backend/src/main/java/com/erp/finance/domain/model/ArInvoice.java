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
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ar_invoice", schema = "finance")
public class ArInvoice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ar_invoice_seq")
    @SequenceGenerator(name = "ar_invoice_seq", sequenceName = "finance.ar_invoice_id_seq", allocationSize = 50)
    private Long id;

    @Column(name = "invoice_no", nullable = false, length = 30)
    private String invoiceNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "total_amount", nullable = false, precision = 20, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "paid_amount", nullable = false, precision = 20, scale = 2)
    private BigDecimal paidAmount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    // 거래 시점 FX 스냅샷 — 생성 시 환율로 환산해 고정(환율 변경에 불변). 환율 부재 시 null(미산정).
    @Column(name = "base_amount", precision = 20, scale = 2)
    private BigDecimal baseAmount;

    @Column(name = "exchange_rate", precision = 18, scale = 8)
    private BigDecimal exchangeRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ArInvoiceStatus status;

    @Column(name = "journal_entry_id")
    private Long journalEntryId;

    @Column(name = "approval_request_id")
    private Long approvalRequestId;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @OneToMany(mappedBy = "arInvoice", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("lineNo ASC")
    private List<ArInvoiceLine> lines = new ArrayList<>();

    protected ArInvoice() {}

    public static ArInvoice create(String invoiceNo, Customer customer, LocalDate invoiceDate,
                                   LocalDate dueDate, BigDecimal totalAmount, String currency, String note) {
        if (dueDate.isBefore(invoiceDate)) {
            throw new ErpException(ErrorCode.INVOICE_DUE_DATE_INVALID);
        }
        ArInvoice inv = new ArInvoice();
        inv.invoiceNo = invoiceNo;
        inv.customer = customer;
        inv.invoiceDate = invoiceDate;
        inv.dueDate = dueDate;
        inv.totalAmount = totalAmount;
        inv.paidAmount = BigDecimal.ZERO;
        inv.currency = currency != null ? currency : "KRW";
        inv.status = ArInvoiceStatus.DRAFT;
        inv.note = note;
        return inv;
    }

    /** 대변 라인 추가(매출·부가세예수금). DRAFT 상태에서만. lineNo는 추가 순서로 자동 채번. */
    public ArInvoiceLine addLine(Account account, BigDecimal amount, String description) {
        if (status != ArInvoiceStatus.DRAFT) {
            throw new ErpException(ErrorCode.INVOICE_ALREADY_PROCESSED);
        }
        ArInvoiceLine line = ArInvoiceLine.of(this, lines.size() + 1, account, amount, description);
        lines.add(line);
        return line;
    }

    public void submit() {
        if (status != ArInvoiceStatus.DRAFT) {
            throw new ErpException(ErrorCode.INVOICE_ALREADY_PROCESSED);
        }
        this.status = ArInvoiceStatus.PENDING_APPROVAL;
    }

    public void approve() {
        if (status != ArInvoiceStatus.PENDING_APPROVAL) {
            throw new ErpException(ErrorCode.INVOICE_ALREADY_PROCESSED);
        }
        this.status = ArInvoiceStatus.APPROVED;
    }

    public void pay(BigDecimal amount) {
        if (status != ArInvoiceStatus.APPROVED) {
            throw new ErpException(ErrorCode.INVOICE_ALREADY_PROCESSED);
        }
        if (amount.compareTo(getOutstandingAmount()) > 0) {
            throw new ErpException(ErrorCode.INVOICE_OVERPAYMENT);
        }
        this.paidAmount = this.paidAmount.add(amount);
        if (this.paidAmount.compareTo(this.totalAmount) >= 0) {
            this.status = ArInvoiceStatus.PAID;
        }
    }

    public void cancel() {
        if (status == ArInvoiceStatus.PAID || status == ArInvoiceStatus.CANCELLED) {
            throw new ErpException(ErrorCode.INVOICE_ALREADY_PROCESSED);
        }
        this.status = ArInvoiceStatus.CANCELLED;
    }

    /** 거래 시점 환산 스냅샷 적용(생성 시 1회). 환율 부재면 호출하지 않아 null(미산정)로 남는다. */
    public void applyBaseSnapshot(BigDecimal baseAmount, BigDecimal exchangeRate) {
        this.baseAmount = baseAmount;
        this.exchangeRate = exchangeRate;
    }

    public void linkJournalEntry(Long journalEntryId) {
        this.journalEntryId = journalEntryId;
    }

    public void linkApprovalRequest(Long approvalRequestId) {
        this.approvalRequestId = approvalRequestId;
    }

    public BigDecimal getOutstandingAmount() {
        return totalAmount.subtract(paidAmount);
    }

    public Long getId() { return id; }
    public String getInvoiceNo() { return invoiceNo; }
    public Customer getCustomer() { return customer; }
    public LocalDate getInvoiceDate() { return invoiceDate; }
    public LocalDate getDueDate() { return dueDate; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public String getCurrency() { return currency; }
    public BigDecimal getBaseAmount() { return baseAmount; }
    public BigDecimal getExchangeRate() { return exchangeRate; }
    public ArInvoiceStatus getStatus() { return status; }
    public Long getJournalEntryId() { return journalEntryId; }
    public Long getApprovalRequestId() { return approvalRequestId; }
    public String getNote() { return note; }
    public List<ArInvoiceLine> getLines() { return lines; }
    public boolean hasLines() { return !lines.isEmpty(); }
}
