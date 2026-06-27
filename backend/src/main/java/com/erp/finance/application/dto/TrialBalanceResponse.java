package com.erp.finance.application.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 시산표 — 거래계정별 차변합·대변합·잔액 + 총차변·총대변(균형 시 ==).
 *
 * @param baseCurrency        기준통화 코드
 * @param rows                계정 행(계정코드 순)
 * @param totalDebit          총차변(기준통화)
 * @param totalCredit         총대변(기준통화)
 * @param excludedEntryCount  환율 미산정으로 집계에서 제외된 POSTED 분개 수
 */
public record TrialBalanceResponse(
        String baseCurrency,
        List<TrialBalanceRow> rows,
        BigDecimal totalDebit,
        BigDecimal totalCredit,
        long excludedEntryCount
) {
    public record TrialBalanceRow(
            String accountCode,
            String accountName,
            BigDecimal debit,
            BigDecimal credit,
            BigDecimal balance
    ) {
    }
}
