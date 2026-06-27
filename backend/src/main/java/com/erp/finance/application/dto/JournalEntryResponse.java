package com.erp.finance.application.dto;

import com.erp.finance.domain.model.JournalEntry;
import com.erp.finance.domain.model.JournalEntryStatus;
import com.erp.finance.domain.model.JournalEntryType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record JournalEntryResponse(
    Long id,
    String entryNo,
    LocalDate entryDate,
    Long fiscalPeriodId,
    int fiscalPeriodNumber,
    String description,
    JournalEntryType entryType,
    JournalEntryStatus status,
    BigDecimal totalDebit,
    BigDecimal totalCredit,
    String currency,
    String referenceType,
    Long referenceId,
    LocalDateTime postedAt,
    String postedBy) {
  public static JournalEntryResponse from(JournalEntry je) {
    return new JournalEntryResponse(
        je.getId(),
        je.getEntryNo(),
        je.getEntryDate(),
        je.getFiscalPeriod().getId(),
        je.getFiscalPeriod().getPeriodNumber(),
        je.getDescription(),
        je.getEntryType(),
        je.getStatus(),
        je.getTotalDebit(),
        je.getTotalCredit(),
        je.getCurrency(),
        je.getReferenceType(),
        je.getReferenceId(),
        je.getPostedAt(),
        je.getPostedBy());
  }
}
