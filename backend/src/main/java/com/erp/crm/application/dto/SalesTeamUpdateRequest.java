package com.erp.crm.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SalesTeamUpdateRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull Long version
) {}
