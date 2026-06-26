package com.erp.finance.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AccountUpdateRequest(
    @NotBlank @Size(max = 200) String name,
    boolean isSummary,
    @NotNull Long version
) {}
