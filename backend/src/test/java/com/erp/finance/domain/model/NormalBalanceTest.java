package com.erp.finance.domain.model;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * normalBalance 부호 처리 단위 검증 — 재무제표 잔액 산식의 핵심.
 * 차변정상(DEBIT)=차변−대변, 대변정상(CREDIT)=대변−차변.
 */
class NormalBalanceTest {

    @Test
    void debitNormal_balanceIsDebitMinusCredit() {
        // 자산·비용 계정: 차변 > 대변이면 양수
        assertThat(NormalBalance.DEBIT.balance(new BigDecimal("150000"), new BigDecimal("50000")))
                .isEqualByComparingTo("100000");
    }

    @Test
    void creditNormal_balanceIsCreditMinusDebit() {
        // 부채·자본·수익 계정: 대변 > 차변이면 양수
        assertThat(NormalBalance.CREDIT.balance(new BigDecimal("40000"), new BigDecimal("90000")))
                .isEqualByComparingTo("50000");
    }

    @Test
    void zeroActivity_balanceIsZero() {
        assertThat(NormalBalance.DEBIT.balance(BigDecimal.ZERO, BigDecimal.ZERO))
                .isEqualByComparingTo("0");
    }
}
