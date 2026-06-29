package com.erp.finance.application.dto;

import java.math.BigDecimal;

/** 월별 감가상각 처리 결과 — 처리 건수·건너뜀(이미 처리·상각 완료) 건수·총 상각액. */
public record DepreciationRunResponse(
    Long fiscalPeriodId, int processedCount, int skippedCount, BigDecimal totalAmount) {}
