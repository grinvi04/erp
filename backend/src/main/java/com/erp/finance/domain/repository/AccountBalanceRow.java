package com.erp.finance.domain.repository;

import java.math.BigDecimal;

/**
 * 계정별 차·대변 환산합 projection — 재무제표 집계용.
 * 금액은 각 분개의 exchangeRate로 환산한 기준통화 원시(full precision) 합계다 — 표시용 반올림은 서비스가 적용한다.
 */
public interface AccountBalanceRow {
    Long getAccountId();
    BigDecimal getDebitSum();
    BigDecimal getCreditSum();
}
