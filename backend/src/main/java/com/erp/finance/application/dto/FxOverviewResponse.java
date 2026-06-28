package com.erp.finance.application.dto;

import java.util.List;

/**
 * FX 설정 한눈 조회 — 기준통화·환율 목록·환차손익 계정을 한 번에 반환(FINANCE_READ). 데이터가 없어도 기본값으로 200: baseCurrency는 미설정 시
 * KRW, rates는 빈 목록, gainLossAccounts는 항목별 null.
 */
public record FxOverviewResponse(
    BaseCurrencyResponse baseCurrency,
    List<ExchangeRateResponse> rates,
    FxGainLossAccountResponse gainLossAccounts) {}
