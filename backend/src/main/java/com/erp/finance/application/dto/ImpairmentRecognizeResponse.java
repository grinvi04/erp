package com.erp.finance.application.dto;

import com.erp.finance.domain.model.ImpairmentEntry;
import java.math.BigDecimal;

/** 손상차손 인식 결과 — 인식 전 장부가액·회수가능액·손상차손액·인식 후 장부가액·생성 분개. */
public record ImpairmentRecognizeResponse(
    Long fixedAssetId,
    Long fiscalPeriodId,
    BigDecimal bookValueBefore,
    BigDecimal recoverableAmount,
    BigDecimal impairmentLoss,
    BigDecimal bookValueAfter,
    Long journalEntryId) {

  public static ImpairmentRecognizeResponse of(ImpairmentEntry e, BigDecimal bookValueAfter) {
    return new ImpairmentRecognizeResponse(
        e.getFixedAssetId(),
        e.getFiscalPeriodId(),
        e.getBookValueBefore(),
        e.getRecoverableAmount(),
        e.getImpairmentLoss(),
        bookValueAfter,
        e.getJournalEntryId());
  }
}
