package com.erp.finance.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 부가세 과세구분 — 과세(TAXABLE 10%)·영세율(ZERO_RATED 0%)·면세(EXEMPT 0%). 인보이스 단위로 하나만 적용한다. 세액은 공급가액 기준
 * 자동계산하며, 과세는 원 미만을 절사한다(한국 관행).
 */
public enum TaxType {
  TAXABLE,
  ZERO_RATED,
  EXEMPT;

  private static final BigDecimal VAT_RATE = new BigDecimal("0.10");
  private static final int MONEY_SCALE = 2;

  /** 공급가액에 대한 부가세액. 과세는 공급가액×10%의 원 미만 절사, 영세율·면세는 0. */
  public BigDecimal computeVat(BigDecimal supplyAmount) {
    if (this != TAXABLE || supplyAmount == null) {
      return BigDecimal.ZERO.setScale(MONEY_SCALE);
    }
    return supplyAmount.multiply(VAT_RATE).setScale(0, RoundingMode.DOWN).setScale(MONEY_SCALE);
  }
}
