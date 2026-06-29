package com.erp.finance.domain.repository;

import java.math.BigDecimal;

/**
 * 거래처(매출처/매입처) 사업자번호 단위 합계 projection — 부가세 신고 합계표. 사업자번호가 없으면 businessNo는 null(서비스가 별도 그룹으로 표기).
 */
public interface PartyAmountRow {
  String getBusinessNo();

  String getName();

  long getCount();

  BigDecimal getSupplyTotal();

  BigDecimal getVatTotal();
}
