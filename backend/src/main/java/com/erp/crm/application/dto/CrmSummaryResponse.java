package com.erp.crm.application.dto;

import com.erp.common.response.CurrencyAmount;
import java.util.List;

public record CrmSummaryResponse(
        long openOpportunities,
        List<CurrencyAmount> openOpportunityAmounts,
        long newLeads,
        long openActivities
) {
}
