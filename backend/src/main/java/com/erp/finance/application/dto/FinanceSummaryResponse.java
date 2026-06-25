package com.erp.finance.application.dto;

import java.math.BigDecimal;

public record FinanceSummaryResponse(
        long unpaidInvoices,
        BigDecimal unpaidAmount,
        long draftJournalEntries
) {
}
