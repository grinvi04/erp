package com.erp.finance.application.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 손익계산서 — 수익(대변−차변)·비용(차변−대변) 계정별 발생액 + 유형별 합계 + 당기순이익(수익−비용).
 *
 * @param baseCurrency 기준통화 코드
 * @param revenues 수익 계정 행(계정코드 순)
 * @param totalRevenue 수익 합
 * @param expenses 비용 계정 행(계정코드 순)
 * @param totalExpense 비용 합
 * @param netIncome 당기순이익 = 수익 − 비용
 * @param excludedEntryCount 환율 미산정으로 제외된 POSTED 분개 수
 */
public record IncomeStatementResponse(
    String baseCurrency,
    List<IncomeStatementLine> revenues,
    BigDecimal totalRevenue,
    List<IncomeStatementLine> expenses,
    BigDecimal totalExpense,
    BigDecimal netIncome,
    long excludedEntryCount) {
  public record IncomeStatementLine(String accountCode, String accountName, BigDecimal amount) {}
}
