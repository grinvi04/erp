package com.erp.finance.application.dto;

import java.util.List;

/**
 * 월별 매입 인보이스 추이 — 통화별 시리즈 + 기준통화 환산 합계 시리즈.
 *
 * @param baseCurrency 테넌트 기준통화 코드(미설정 시 KRW)
 * @param byCurrency 통화별 월별 시리즈(기존 표시 유지)
 * @param baseMonthlyTotals 모든 통화를 기준통화로 합산한 월별 시리즈(base_amount 산정분만). 산정된 행이 하나도 없으면 빈 리스트.
 */
public record MonthlyInvoiceAnalyticsResponse(
    String baseCurrency,
    List<MonthlyInvoiceByCurrencyResponse> byCurrency,
    List<MonthlyInvoiceResponse> baseMonthlyTotals) {}
