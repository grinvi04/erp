package com.erp.hr.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PositionUpdateRequest(
    @NotBlank @Size(max = 100) String name,
    @Min(0) int levelOrder,
    @NotNull Long version
) {}
