package com.erp.crm.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ContactCreateRequest(
    @NotNull Long accountId,
    @NotBlank @Size(max = 50) String lastName,
    @NotBlank @Size(max = 50) String firstName,
    @Size(max = 100) String title,
    @Size(max = 100) String department,
    @Email @Size(max = 200) String email,
    @Size(max = 30) String phone,
    @Size(max = 30) String mobile,
    boolean isPrimary) {}
