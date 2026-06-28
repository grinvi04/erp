package com.erp.finance.domain.model;

import java.math.BigDecimal;

public enum NormalBalance {
  DEBIT,
  CREDIT;

  /**
   * 정상잔액 방향에 따른 부호 처리한 잔액 — 차변정상(DEBIT)=차변−대변, 대변정상(CREDIT)=대변−차변. 재무제표(시산표·손익계산서·재무상태표)에서 계정 유형별
   * 잔액 산출에 공통 사용한다.
   */
  public BigDecimal balance(BigDecimal debit, BigDecimal credit) {
    return this == DEBIT ? debit.subtract(credit) : credit.subtract(debit);
  }
}
