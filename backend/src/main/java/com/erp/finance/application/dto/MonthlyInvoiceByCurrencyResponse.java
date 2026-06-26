package com.erp.finance.application.dto;

import java.util.List;

public record MonthlyInvoiceByCurrencyResponse(
        String currency,
        List<MonthlyInvoiceResponse> months
) {
}
