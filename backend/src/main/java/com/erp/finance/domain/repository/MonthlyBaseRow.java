package com.erp.finance.domain.repository;

import java.math.BigDecimal;

/** 월별 기준통화 환산액 합계 행 — {@link ApInvoiceRepository#monthlyBaseTotals(int)}. */
public interface MonthlyBaseRow {
    Integer getMonth();
    BigDecimal getBaseTotal();
}
