package com.erp.finance.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** 실현 환차손익 계산·부호 단위 검증 — 통제계정 청산(인보이스환율)·현금(결제환율)·차액 부호. */
class FxGainLossTest {

  @Test
  void paymentRateHigherThanInvoiceRate_cashExceedsControl() {
    // 100 USD, 인보이스 1300 / 결제 1350 → 현금(135000) > 통제(130000), 차액 5000.
    FxGainLoss fx =
        FxGainLoss.of(new BigDecimal("100"), new BigDecimal("1300"), new BigDecimal("1350"));

    assertThat(fx.controlAmount()).isEqualByComparingTo("130000");
    assertThat(fx.cashAmount()).isEqualByComparingTo("135000");
    assertThat(fx.amount()).isEqualByComparingTo("5000");
    assertThat(fx.hasDifference()).isTrue();
    assertThat(fx.cashExceedsControl()).isTrue();
  }

  @Test
  void paymentRateLowerThanInvoiceRate_controlExceedsCash() {
    // 100 USD, 인보이스 1300 / 결제 1250 → 현금(125000) < 통제(130000), 차액 5000.
    FxGainLoss fx =
        FxGainLoss.of(new BigDecimal("100"), new BigDecimal("1300"), new BigDecimal("1250"));

    assertThat(fx.amount()).isEqualByComparingTo("5000");
    assertThat(fx.hasDifference()).isTrue();
    assertThat(fx.cashExceedsControl()).isFalse();
  }

  @Test
  void sameRate_noDifference() {
    FxGainLoss fx =
        FxGainLoss.of(new BigDecimal("100"), new BigDecimal("1300"), new BigDecimal("1300"));

    assertThat(fx.amount()).isEqualByComparingTo("0");
    assertThat(fx.hasDifference()).isFalse();
  }

  @Test
  void partialPayment_proportionalToPaidAmount() {
    // 부분지급 40 USD만 — 환차도 paidAmount 기준(전액 아님): 40×(1350−1300)=2000.
    FxGainLoss fx =
        FxGainLoss.of(new BigDecimal("40"), new BigDecimal("1300"), new BigDecimal("1350"));

    assertThat(fx.controlAmount()).isEqualByComparingTo("52000");
    assertThat(fx.cashAmount()).isEqualByComparingTo("54000");
    assertThat(fx.amount()).isEqualByComparingTo("2000");
  }

  @Test
  void amounts_roundedToScale2_andDifferenceBalances() {
    // 반올림된 두 금액의 차로 환차를 잡아 통제+환차 = 현금(균형) 보장.
    FxGainLoss fx =
        FxGainLoss.of(
            new BigDecimal("33.33"), new BigDecimal("1300.12345"), new BigDecimal("1351.98765"));

    assertThat(fx.controlAmount().scale()).isEqualTo(2);
    assertThat(fx.cashAmount().scale()).isEqualTo(2);
    // 현금 = 통제 + 환차(현금>통제) — 차대변 균형의 핵심 불변식.
    assertThat(fx.controlAmount().add(fx.amount())).isEqualByComparingTo(fx.cashAmount());
  }
}
