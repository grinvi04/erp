package com.erp.finance.application.dto;

import com.erp.finance.domain.model.DepreciationEntry;
import java.math.BigDecimal;

/** 감가상각 이력 1건 — 자산 상세의 상각 내역 표시용. */
public record DepreciationEntryResponse(
    Long id, Long fiscalPeriodId, BigDecimal amount, Long journalEntryId) {

  public static DepreciationEntryResponse from(DepreciationEntry e) {
    return new DepreciationEntryResponse(
        e.getId(), e.getFiscalPeriodId(), e.getAmount(), e.getJournalEntryId());
  }
}
