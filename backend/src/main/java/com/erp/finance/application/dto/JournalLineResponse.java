package com.erp.finance.application.dto;

import com.erp.finance.domain.model.JournalLine;
import java.math.BigDecimal;

public record JournalLineResponse(
    Long id,
    int lineNo,
    Long accountId,
    String accountCode,
    String accountName,
    BigDecimal debitAmount,
    BigDecimal creditAmount,
    String description,
    Long departmentId) {
  public static JournalLineResponse from(JournalLine line) {
    return new JournalLineResponse(
        line.getId(),
        line.getLineNo(),
        line.getAccount().getId(),
        line.getAccount().getCode(),
        line.getAccount().getName(),
        line.getDebitAmount(),
        line.getCreditAmount(),
        line.getDescription(),
        line.getDepartmentId());
  }
}
