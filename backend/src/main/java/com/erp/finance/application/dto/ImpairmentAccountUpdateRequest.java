package com.erp.finance.application.dto;

/** 손상차손 계정 설정 변경 — 각 nullable(미지정 시 해제 → 손상 분개 차단). */
public record ImpairmentAccountUpdateRequest(
    Long impairmentLossAccountId, Long accumulatedImpairmentAccountId) {}
