package com.erp.finance.application.dto;

/** 감가상각·처분 계정 설정 변경 — 각 nullable(미지정 시 해제 → 상각/처분 분개 차단). */
public record DepreciationAccountUpdateRequest(
    Long depreciationExpenseAccountId,
    Long accumulatedDepreciationAccountId,
    Long disposalGainAccountId,
    Long disposalLossAccountId) {}
