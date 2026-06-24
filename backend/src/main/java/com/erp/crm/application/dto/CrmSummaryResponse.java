package com.erp.crm.application.dto;

import java.math.BigDecimal;

public record CrmSummaryResponse(
        long openOpportunities,
        BigDecimal openOpportunityAmount,
        long newLeads,
        long openActivities
) {
}
