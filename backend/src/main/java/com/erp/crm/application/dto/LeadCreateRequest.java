package com.erp.crm.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LeadCreateRequest(
        @NotBlank @Size(max = 50) String lastName,
        @NotBlank @Size(max = 50) String firstName,
        @Size(max = 200) String company,
        @Size(max = 100) String title,
        @Email @Size(max = 200) String email,
        @Size(max = 30) String phone,
        @Size(max = 50) String source,
        String note
) {}
