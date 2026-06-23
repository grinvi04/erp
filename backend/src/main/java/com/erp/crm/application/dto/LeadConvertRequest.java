package com.erp.crm.application.dto;

import jakarta.validation.constraints.NotNull;

public record LeadConvertRequest(
        @NotNull Long accountId,
        Long opportunityId
) {}
