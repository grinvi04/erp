package com.erp.crm.application.dto;

import com.erp.crm.domain.model.Opportunity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record OpportunityResponse(
        Long id,
        Long accountId,
        String accountName,
        String name,
        Long stageId,
        String stageName,
        BigDecimal amount,
        String currency,
        LocalDate closeDate,
        int probability,
        String ownerId,
        String source,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static OpportunityResponse from(Opportunity o) {
        return new OpportunityResponse(o.getId(), o.getAccount().getId(), o.getAccount().getName(),
                o.getName(), o.getStage().getId(), o.getStage().getName(),
                o.getAmount(), o.getCurrency(), o.getCloseDate(), o.getProbability(),
                o.getOwnerId(), o.getSource(), o.getDescription(),
                o.getCreatedAt(), o.getUpdatedAt());
    }
}
