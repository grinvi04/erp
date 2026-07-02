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
  void declining_fullyDepreciatesToResidualByEndOfLife() {
    // AC-2·3: 정률 1,200만·60월·잔존0·율0.5 → 초기엔 정률(월0=500,000), 후반 정액 전환, 60개월 후 장부가액 0.
    FixedAsset asset =
        FixedAsset.register(
            "FA-DFULL",
            "자산",
            DATE,
            BigDecimal.valueOf(12_000_000),
            BigDecimal.ZERO,
            60,
            DepreciationMethod.DECLINING_BALANCE,
            new BigDecimal("0.5"),
            null);
    // 월0: 정률 500,000(=12M×0.5/12) > 정액 200,000(=12M/60) → 정률액.
    assertThat(asset.monthlyDepreciation()).isEqualByComparingTo("500000");
    for (int i = 0; i < 60; i++) {
      BigDecimal m = asset.monthlyDepreciation();
      assertThat(m.signum()).as("매월 양수 상각(월 %d)", i).isPositive();
      asset.applyDepreciation(m);
    }
    assertThat(asset.bookValue()).isEqualByComparingTo("0");
    assertThat(asset.monthlyDepreciation()).isEqualByComparingTo("0");
  }

  @Test
  void noImpairmentCarryingAmount_declining_reflectsStraightLineSwitch() {
    // 환입 한도: 정률 시뮬레이션이 monthlyDepreciation과 동일 max(정률,정액)을 써야 함.
    // 12M·12월·율0.5는 월0부터 정액(1M)>정률(500k)이라 6개월 후 장부=6,000,000(순수정률이면 과대).
    FixedAsset asset =
        FixedAsset.register(
            "FA-DCEIL",
            "자산",
            DATE,
            BigDecimal.valueOf(12_000_000),
            BigDecimal.ZERO,
            12,
            DepreciationMethod.DECLINING_BALANCE,
            new BigDecimal("0.5"),
            null);
    assertThat(asset.noImpairmentCarryingAmount(6)).isEqualByComparingTo("6000000");
  }

  @Test
  void declining_afterImpairment_stillFullyDepreciates() {
    // AC-9: 손상 인식된 정률 자산도 내용연수 말 장부가액=잔존(0)에 도달.
    FixedAsset asset =
        FixedAsset.register(
            "FA-DIMP",
            "자산",
            DATE,
            BigDecimal.valueOf(12_000_000),
            BigDecimal.ZERO,
            12,
            DepreciationMethod.DECLINING_BALANCE,
            new BigDecimal("0.5"),
            null);
    for (int i = 0; i < 12; i++) {
      if (i == 3) {
        asset.applyImpairment(new BigDecimal("1000000"), 9); // 정률: 손상누계만 반영(override 없음)
      }
      asset.applyDepreciation(asset.monthlyDepreciation());
    }
    assertThat(asset.bookValue()).isEqualByComparingTo("0");
  }

  @Test
  void declining_withResidual_reachesResidualByEndOfLife() {
    // AC-4: 정률·잔존 200만·12월 → 12개월 상각 후 장부가액=잔존가치.
    FixedAsset a =
        FixedAsset.register(
            "FA-DRES",
            "자산",
            DATE,
            BigDecimal.valueOf(12_000_000),
            BigDecimal.valueOf(2_000_000),
            12,
            DepreciationMethod.DECLINING_BALANCE,
            new BigDecimal("0.5"),
            null);
    for (int i = 0; i < 12; i++) {
      a.applyDepreciation(a.monthlyDepreciation());
    }
    assertThat(a.bookValue()).isEqualByComparingTo("2000000");
  }

  @Test
  void applyDepreciation_incrementsDepreciatedMonths() {
    // AC-6: applyDepreciation 1회당 depreciatedMonths 1 증가.
    FixedAsset a = straightLine(12_000_000, 0, 60);
    a.applyDepreciation(new BigDecimal("200000"));
    a.applyDepreciation(new BigDecimal("200000"));
    assertThat(a.getDepreciatedMonths()).isEqualTo(2);
  }

  @Test
  void noImpairmentCarryingAmount_straightLine() {
    // AC-2: 1,200만·60월·정액, 12개월 경과 → 가상 누계 240만 → 한도 960만.
    assertThat(straightLine(12_000_000, 0, 60).noImpairmentCarryingAmount(12))
        .isEqualByComparingTo("9600000");
  }

  @Test
  void noImpairmentCarryingAmount_decliningBalance() {
    // AC-2: 1,000만·0.45 정률, 2개월 시뮬레이션 → 375,000·360,937.50 차감 → 9,264,062.50.
    assertThat(declining(10_000_000, 0, "0.45").noImpairmentCarryingAmount(2))
        .isEqualByComparingTo("9264062.50");
  }

  @Test
  void applyReversal_straightLine_raisesBookValueAndRespreads() {
    // AC-5: 12개월 상각·480만 손상(장부 480만) 후 240만 환입 → 장부 720만, 잔여 48월 재배분 = 720만/48 = 15만.
    FixedAsset a = straightLine(12_000_000, 0, 60);
    a.applyDepreciation(new BigDecimal("2400000"));
    a.applyImpairment(new BigDecimal("4800000"), 48);
    a.applyReversal(new BigDecimal("2400000"), 48);
    assertThat(a.getAccumulatedImpairment()).isEqualByComparingTo("2400000");
    assertThat(a.bookValue()).isEqualByComparingTo("7200000");
    assertThat(a.monthlyDepreciation()).isEqualByComparingTo("150000");
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
