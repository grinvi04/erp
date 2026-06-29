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

  @Test
  void bookValue_subtractsAccumulatedImpairment() {
    // AC-2: 장부가액 = 취득원가 − 감가상각누계액 − 손상차손누계액.
    FixedAsset a = straightLine(10_000_000, 0, 60);
    a.applyDepreciation(new BigDecimal("1000000")); // 누계상각 100만
    a.applyImpairment(new BigDecimal("2000000"), 48); // 손상 200만
    assertThat(a.bookValue()).isEqualByComparingTo("7000000");
  }

  @Test
  void straightLine_afterImpairment_respreadsOverRemainingLife() {
    // AC-4: 12개월 상각(장부 960만) 후 회수가능액 480만으로 손상(손상 480만) → 잔여 48월 재배분 = 480만/48 = 10만.
    FixedAsset a = straightLine(12_000_000, 0, 60);
    a.applyDepreciation(new BigDecimal("2400000")); // 12개월분 누계
    a.applyImpairment(new BigDecimal("4800000"), 48);
    assertThat(a.bookValue()).isEqualByComparingTo("4800000");
    assertThat(a.monthlyDepreciation()).isEqualByComparingTo("100000");
  }

  @Test
  void declining_afterImpairment_usesImpairedBookValue() {
    // AC-3: 1개월 상각(장부 962.5만) 후 회수가능액 500만으로 손상 → 차기 = 500만 × 0.45/12 = 187,500. override 미설정.
    FixedAsset a = declining(10_000_000, 0, "0.45");
    a.applyDepreciation(new BigDecimal("375000"));
    a.applyImpairment(new BigDecimal("4625000"), 48);
    assertThat(a.bookValue()).isEqualByComparingTo("5000000");
    assertThat(a.monthlyDepreciation()).isEqualByComparingTo("187500");
    assertThat(a.getStraightLineMonthlyOverride()).isNull();
  }
}
