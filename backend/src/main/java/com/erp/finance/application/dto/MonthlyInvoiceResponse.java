package com.erp.finance.application.dto;

import java.math.BigDecimal;

public record MonthlyInvoiceResponse(int month, long count, BigDecimal totalAmount) {}
