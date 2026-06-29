package com.erp.finance.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class TaxTypeTest {

  @Test
  void 과세는_공급가액의_10퍼센트() {
    // AC-3
    assertThat(TaxType.TAXABLE.computeVat(new BigDecimal("100000"))).isEqualByComparingTo("10000");
  }

  @Test
  void 과세_세액은_원미만_절사() {
    // AC-4: 12345 × 0.1 = 1234.5 → 절사 1234
    assertThat(TaxType.TAXABLE.computeVat(new BigDecimal("12345"))).isEqualByComparingTo("1234");
  }

  @Test
  void 영세율과_면세는_세액0() {
    // AC-5
    assertThat(TaxType.ZERO_RATED.computeVat(new BigDecimal("100000"))).isEqualByComparingTo("0");
    assertThat(TaxType.EXEMPT.computeVat(new BigDecimal("100000"))).isEqualByComparingTo("0");
  }
}
