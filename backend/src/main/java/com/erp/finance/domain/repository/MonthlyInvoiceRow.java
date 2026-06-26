package com.erp.finance.domain.repository;

import java.math.BigDecimal;

public interface MonthlyInvoiceRow {
    Integer getMonth();
    String getCurrency();
    long getCount();
    BigDecimal getTotalAmount();
}
