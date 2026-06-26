package com.erp.crm.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SalesTeamCreateRequest(
        @NotBlank @Size(max = 30) String code,
        @NotBlank @Size(max = 100) String name
) {}
