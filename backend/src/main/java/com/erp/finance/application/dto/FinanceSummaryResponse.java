package com.erp.finance.application.dto;

import com.erp.common.response.CurrencyAmount;
import java.util.List;

public record FinanceSummaryResponse(
        long unpaidInvoices,
        List<CurrencyAmount> unpaidAmounts,
        long draftJournalEntries
) {
}
