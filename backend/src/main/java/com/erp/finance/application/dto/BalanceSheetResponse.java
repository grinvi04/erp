package com.erp.finance.application.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 재무상태표 — 자산·부채·자본 계정의 연말 기준 누적 잔액 + 유형별 합계 + 당기순이익(자본 가산분) + 균형 여부.
 * 균형식: 자산합 == 부채합 + 자본합 + 당기순이익 (±0.01).
 *
 * @param baseCurrency        기준통화 코드
 * @param assets              자산 계정 행(계정코드 순)
 * @param totalAssets         자산 합
 * @param liabilities         부채 계정 행
 * @param totalLiabilities    부채 합
 * @param equity              자본 계정 행
 * @param totalEquity         자본 합(당기순이익 가산 전)
 * @param netIncome           당기순이익(누적 수익−비용) — 이익잉여금으로 자본에 가산
 * @param balanced            균형(자산 == 부채+자본+당기순이익) 여부
 * @param excludedEntryCount  환율 미산정으로 제외된 POSTED 분개 수
 */
public record BalanceSheetResponse(
        String baseCurrency,
        List<BalanceSheetLine> assets,
        BigDecimal totalAssets,
        List<BalanceSheetLine> liabilities,
        BigDecimal totalLiabilities,
        List<BalanceSheetLine> equity,
        BigDecimal totalEquity,
        BigDecimal netIncome,
        boolean balanced,
        long excludedEntryCount
) {
    public record BalanceSheetLine(
            String accountCode,
            String accountName,
            BigDecimal amount
    ) {
    }
}
