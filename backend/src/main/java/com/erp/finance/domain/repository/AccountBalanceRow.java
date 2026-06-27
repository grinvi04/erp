package com.erp.finance.domain.repository;

import java.math.BigDecimal;

/**
 * 계정별 차·대변 환산합 projection — 재무제표 집계용.
 * 금액은 각 분개의 exchangeRate로 환산한 기준통화 합계(소수 2자리)다.
 */
public interface AccountBalanceRow {
    Long getAccountId();
    BigDecimal getDebitSum();
    BigDecimal getCreditSum();
}
