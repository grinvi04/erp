package com.erp.finance.application.dto;

import com.erp.finance.domain.model.ImpairmentEntry;
import java.math.BigDecimal;

/** 손상차손 환입 결과 — 환입 전 장부가액·회수가능액·환입액·환입 후 장부가액·생성 분개. */
public record ImpairmentReversalResponse(
    Long fixedAssetId,
    Long fiscalPeriodId,
    BigDecimal bookValueBefore,
    BigDecimal recoverableAmount,
    BigDecimal reversalAmount,
    BigDecimal bookValueAfter,
    Long journalEntryId) {

  public static ImpairmentReversalResponse of(ImpairmentEntry e, BigDecimal bookValueAfter) {
    return new ImpairmentReversalResponse(
        e.getFixedAssetId(),
        e.getFiscalPeriodId(),
        e.getBookValueBefore(),
        e.getRecoverableAmount(),
        e.getImpairmentLoss(),
        bookValueAfter,
        e.getJournalEntryId());
  }
}
