package com.erp.crm.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record OpportunityUpdateRequest(
        @NotBlank @Size(max = 200) String name,
        @NotNull Long stageId,
        @PositiveOrZero BigDecimal amount,
        @Size(min = 3, max = 3) String currency,
        LocalDate closeDate,
        @Min(0) @Max(100) int probability,
        @NotBlank @Size(max = 100) String ownerId,
        @Size(max = 50) String source,
        String description
) {}
