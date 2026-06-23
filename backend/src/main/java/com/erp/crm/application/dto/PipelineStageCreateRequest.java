package com.erp.crm.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PipelineStageCreateRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull @Min(1) Integer stageOrder,
        @Min(0) @Max(100) int probability,
        boolean isClosedWon,
        boolean isClosedLost
) {}
