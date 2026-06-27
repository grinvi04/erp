package com.erp.crm.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TeamMemberRequest(
        @NotBlank @Size(max = 100) String userId
) {}
