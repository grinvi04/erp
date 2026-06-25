package com.erp.finance.domain.repository;

import java.math.BigDecimal;

public interface MonthlyInvoiceRow {
    Integer getMonth();
    long getCount();
    BigDecimal getTotalAmount();
}
