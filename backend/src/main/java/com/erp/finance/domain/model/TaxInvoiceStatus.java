package com.erp.finance.domain.model;

/** 전자세금계산서 상태 — 발행(ISSUED)·취소(CANCELLED). 발행 즉시 ISSUED(별도 DRAFT 없음). */
public enum TaxInvoiceStatus {
  ISSUED,
  CANCELLED
}
