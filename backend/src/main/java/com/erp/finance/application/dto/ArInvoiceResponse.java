package com.erp.finance.application.dto;

import com.erp.finance.domain.model.ArInvoice;
import com.erp.finance.domain.model.ArInvoiceStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ArInvoiceResponse(
    Long id,
    String invoiceNo,
    Long customerId,
    String customerName,
    LocalDate invoiceDate,
    LocalDate dueDate,
    BigDecimal totalAmount,
    BigDecimal paidAmount,
    BigDecimal outstandingAmount,
    String currency,
    ArInvoiceStatus status,
    Long journalEntryId,
    Long approvalRequestId,
    String note) {
  public static ArInvoiceResponse from(ArInvoice inv) {
    return new ArInvoiceResponse(
        inv.getId(),
        inv.getInvoiceNo(),
        inv.getCustomer().getId(),
        inv.getCustomer().getName(),
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
