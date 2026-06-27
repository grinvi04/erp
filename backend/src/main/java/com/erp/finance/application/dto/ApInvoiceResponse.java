package com.erp.finance.application.dto;

import com.erp.finance.domain.model.ApInvoice;
import com.erp.finance.domain.model.ApInvoiceStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ApInvoiceResponse(
    Long id,
    String invoiceNo,
    Long vendorId,
    String vendorName,
    LocalDate invoiceDate,
    LocalDate dueDate,
    BigDecimal totalAmount,
    BigDecimal paidAmount,
    BigDecimal outstandingAmount,
    String currency,
    ApInvoiceStatus status,
    Long journalEntryId,
    Long approvalRequestId,
    String note) {
  public static ApInvoiceResponse from(ApInvoice inv) {
    return new ApInvoiceResponse(
        inv.getId(),
        inv.getInvoiceNo(),
        inv.getVendor().getId(),
        inv.getVendor().getName(),
        inv.getInvoiceDate(),
        inv.getDueDate(),
        inv.getTotalAmount(),
        inv.getPaidAmount(),
        inv.getOutstandingAmount(),
        inv.getCurrency(),
        inv.getStatus(),
        inv.getJournalEntryId(),
        inv.getApprovalRequestId(),
        inv.getNote());
  }
}
