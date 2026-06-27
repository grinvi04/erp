package com.erp.finance.application.dto;

/**
 * 환차손익 계정 설정 변경 — 환차이익·환차손 계정 ID. 둘 다 nullable(미지정 시 해제 → 환차 분개 폴백).
 */
public record FxGainLossAccountUpdateRequest(
    Long fxGainAccountId,
    Long fxLossAccountId
) {}
