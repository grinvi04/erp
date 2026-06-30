package com.erp.finance.application.dto;

import com.erp.finance.domain.model.ImpairmentEntry;
import java.math.BigDecimal;

/** 손상차손 이력 1건 — 자산 상세의 손상 내역 표시용. */
public record ImpairmentEntryResponse(
    Long id,
    Long fiscalPeriodId,
    String entryType,
    BigDecimal recoverableAmount,
    BigDecimal bookValueBefore,
    BigDecimal impairmentLoss,
    Long journalEntryId) {

  public static ImpairmentEntryResponse from(ImpairmentEntry e) {
    return new ImpairmentEntryResponse(
        e.getId(),
        e.getFiscalPeriodId(),
        e.getEntryType().name(),
        e.getRecoverableAmount(),
        e.getBookValueBefore(),
        e.getImpairmentLoss(),
        e.getJournalEntryId());
  }
}
