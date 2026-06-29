package com.erp.finance.domain.model;

/** 세금계산서 청구/영수 구분 — 대금을 청구(CHARGE)하는지 이미 영수(RECEIPT)했는지. 국세청 표준의 영수/청구 구분. */
public enum ChargeType {
  CHARGE,
  RECEIPT
}
