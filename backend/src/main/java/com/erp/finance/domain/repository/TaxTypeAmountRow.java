package com.erp.finance.domain.repository;

import com.erp.finance.domain.model.TaxType;
import java.math.BigDecimal;

/** 과세구분별 공급가액·세액 합계 projection — 부가세 신고 요약(매출 과세/영세율/면세 분리). */
public interface TaxTypeAmountRow {
  TaxType getTaxType();

  BigDecimal getSupplyTotal();

  BigDecimal getVatTotal();
}
