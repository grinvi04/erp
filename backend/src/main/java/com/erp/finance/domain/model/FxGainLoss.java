package com.erp.finance.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 실현 환차손익 계산 — 외화 결제 시 인보이스환율(스냅샷)과 결제환율의 기준통화 차액.
 *
 * <ul>
 *   <li>{@code controlAmount} = paidAmount × invoiceRate — 통제계정(외상매입/매출금) 청산액(인보이스환율).</li>
 *   <li>{@code cashAmount}    = paidAmount × paymentRate — 현금 환산액(결제환율).</li>
 * </ul>
 *
 * <p>차액은 두 <b>반올림된</b> 금액의 차(=환차)로 잡아 분개 차대변이 정확히 균형하도록 한다
 * (양쪽을 각각 ROUND(2) 후 빼므로 잔돈이 남지 않는다). 부호 해석(손익·차대변)은 AP/AR 호출부에서
 * {@link #cashExceedsControl()}로 결정한다. 순수 계산 — 부수효과·의존 없음(단위 테스트 가능).
 */
public record FxGainLoss(BigDecimal controlAmount, BigDecimal cashAmount) {

    private static final int SCALE = 2;

    /** paidAmount를 인보이스환율·결제환율로 각각 환산해(ROUND 2) 환차 계산 객체를 만든다. */
    public static FxGainLoss of(BigDecimal paidAmount, BigDecimal invoiceRate, BigDecimal paymentRate) {
        return new FxGainLoss(
            paidAmount.multiply(invoiceRate).setScale(SCALE, RoundingMode.HALF_UP),
            paidAmount.multiply(paymentRate).setScale(SCALE, RoundingMode.HALF_UP));
    }

    /** 환차 금액(분개 라인 금액) = |현금 − 통제|. */
    public BigDecimal amount() {
        return cashAmount.subtract(controlAmount).abs();
    }

    /** 환차가 존재하는지(결제환율≠인보이스환율로 차액 발생). */
    public boolean hasDifference() {
        return cashAmount.compareTo(controlAmount) != 0;
    }

    /**
     * 현금 환산액이 통제계정 청산액보다 큰지 — 결제환율 &gt; 인보이스환율.
     * AP면 더 지급(환차손), AR이면 더 수금(환차이익). 반대면 각각 환차이익·환차손.
     */
    public boolean cashExceedsControl() {
        return cashAmount.compareTo(controlAmount) > 0;
    }
}
