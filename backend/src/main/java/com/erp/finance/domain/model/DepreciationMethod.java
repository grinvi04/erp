package com.erp.finance.domain.model;

/** 감가상각 방법 — 정액법(취득가-잔존가치 균등)·정률법(장부가액 체감). */
public enum DepreciationMethod {
  STRAIGHT_LINE,
  DECLINING_BALANCE
}
