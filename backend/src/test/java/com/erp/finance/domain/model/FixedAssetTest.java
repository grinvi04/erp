package com.erp.finance.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class FixedAssetTest {

  private static final LocalDate DATE = LocalDate.of(2025, 1, 1);

  private static FixedAsset straightLine(long cost, long residual, int months) {
    return FixedAsset.register(
        "FA1",
        "자산",
        DATE,
        BigDecimal.valueOf(cost),
        BigDecimal.valueOf(residual),
        months,
        DepreciationMethod.STRAIGHT_LINE,
        null,
        null);
  }

  private static FixedAsset declining(long cost, long residual, String rate) {
    return FixedAsset.register(
        "FA2",
        "자산",
        DATE,
        BigDecimal.valueOf(cost),
        BigDecimal.valueOf(residual),
        60,
        DepreciationMethod.DECLINING_BALANCE,
        new BigDecimal(rate),
        null);
  }

  @Test
  void straightLine_noResidual() {
    // AC-2: 1,200만 / 60월 = 20만.
    assertThat(straightLine(12_000_000, 0, 60).monthlyDepreciation())
        .isEqualByComparingTo("200000");
  }

  @Test
  void straightLine_withResidual() {
    // (1,200만 − 120만) / 60 = 18만.
    assertThat(straightLine(12_000_000, 1_200_000, 60).monthlyDepreciation())
        .isEqualByComparingTo("180000");
  }

  @Test
  void straightLine_lastPeriod_capsAtRemaining() {
    // AC-6: 누계 1,190만 도달 시 잔여 10만만 상각(20만 아님).
    FixedAsset a = straightLine(12_000_000, 0, 60);
    a.applyDepreciation(new BigDecimal("11900000"));
    assertThat(a.monthlyDepreciation()).isEqualByComparingTo("100000");
  }

  @Test
  void straightLine_atResidual_returnsZero() {
    // AC-6: 잔존가치 도달 → 추가 상각 0.
    FixedAsset a = straightLine(12_000_000, 2_000_000, 60);
    a.applyDepreciation(new BigDecimal("10000000")); // 장부가액 = 잔존가치
    assertThat(a.monthlyDepreciation()).isEqualByComparingTo("0");
  }

  @Test
  void declining_firstMonth() {
    // AC-3: 1,000만 × 0.45/12 = 37.5만.
    assertThat(declining(10_000_000, 0, "0.45").monthlyDepreciation())
        .isEqualByComparingTo("375000");
  }

  @Test
  void declining_decreasesEachPeriod() {
    // 체감: 1개월 상각 후 장부가액 9,625,000 × 0.45/12 = 360,937.50.
    FixedAsset a = declining(10_000_000, 0, "0.45");
    a.applyDepreciation(new BigDecimal("375000"));
    assertThat(a.monthlyDepreciation()).isEqualByComparingTo("360937.50");
  }

  @Test
  void disposed_returnsZero() {
    // AC-7: 처분 자산은 상각 0.
    FixedAsset a = straightLine(12_000_000, 0, 60);
    a.dispose();
    assertThat(a.monthlyDepreciation()).isEqualByComparingTo("0");
    assertThat(a.isActive()).isFalse();
  }

  @Test
  void bookValue_isCostMinusAccumulated() {
    FixedAsset a = straightLine(12_000_000, 0, 60);
    a.applyDepreciation(new BigDecimal("200000"));
    assertThat(a.bookValue()).isEqualByComparingTo("11800000");
  }
}
