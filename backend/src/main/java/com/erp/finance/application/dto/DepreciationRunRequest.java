package com.erp.finance.application.dto;

import jakarta.validation.constraints.NotNull;

/** 월별 감가상각 처리 요청 — 대상 회계기간. */
public record DepreciationRunRequest(@NotNull Long fiscalPeriodId) {}
