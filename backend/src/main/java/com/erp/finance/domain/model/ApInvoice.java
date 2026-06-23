package com.erp.finance.domain.model;

import com.erp.common.audit.BaseEntity;
import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
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
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "ap_invoice", schema = "finance")
public class ApInvoice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "invoice_seq")
    @SequenceGenerator(name = "invoice_seq", sequenceName = "finance.invoice_id_seq", allocationSize = 50)
    private Long id;

    @Column(name = "invoice_no", nullable = false, length = 30)
    private String invoiceNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ApInvoiceStatus status;

    @Column(name = "journal_entry_id")
    private Long journalEntryId;

    @Column(name = "approval_request_id")
    private Long approvalRequestId;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    protected ApInvoice() {}

    public static ApInvoice create(String invoiceNo, Vendor vendor, LocalDate invoiceDate,
                                    LocalDate dueDate, BigDecimal totalAmount, String currency, String note) {
        if (dueDate.isBefore(invoiceDate)) {
            throw new ErpException(ErrorCode.INVOICE_DUE_DATE_INVALID);
        }
        ApInvoice inv = new ApInvoice();
        inv.invoiceNo = invoiceNo;
        inv.vendor = vendor;
        inv.invoiceDate = invoiceDate;
        inv.dueDate = dueDate;
        inv.totalAmount = totalAmount;
        inv.paidAmount = BigDecimal.ZERO;
        inv.currency = currency != null ? currency : "KRW";
        inv.status = ApInvoiceStatus.DRAFT;
        inv.note = note;
        return inv;
    }

    public void submit() {
        if (status != ApInvoiceStatus.DRAFT) {
            throw new ErpException(ErrorCode.INVOICE_ALREADY_PROCESSED);
        }
        this.status = ApInvoiceStatus.PENDING_APPROVAL;
    }

    public void approve() {
        if (status != ApInvoiceStatus.PENDING_APPROVAL) {
            throw new ErpException(ErrorCode.INVOICE_ALREADY_PROCESSED);
        }
        this.status = ApInvoiceStatus.APPROVED;
    }

    public void pay(BigDecimal amount) {
        if (status != ApInvoiceStatus.APPROVED) {
            throw new ErpException(ErrorCode.INVOICE_ALREADY_PROCESSED);
        }
        if (amount.compareTo(getOutstandingAmount()) > 0) {
            throw new ErpException(ErrorCode.INVOICE_OVERPAYMENT);
        }
        this.paidAmount = this.paidAmount.add(amount);
        if (this.paidAmount.compareTo(this.totalAmount) >= 0) {
            this.status = ApInvoiceStatus.PAID;
        }
    }

    public void cancel() {
        if (status == ApInvoiceStatus.PAID || status == ApInvoiceStatus.CANCELLED) {
            throw new ErpException(ErrorCode.INVOICE_ALREADY_PROCESSED);
        }
        this.status = ApInvoiceStatus.CANCELLED;
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
    public Vendor getVendor() { return vendor; }
    public LocalDate getInvoiceDate() { return invoiceDate; }
    public LocalDate getDueDate() { return dueDate; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public String getCurrency() { return currency; }
    public ApInvoiceStatus getStatus() { return status; }
    public Long getJournalEntryId() { return journalEntryId; }
    public Long getApprovalRequestId() { return approvalRequestId; }
    public String getNote() { return note; }
}
