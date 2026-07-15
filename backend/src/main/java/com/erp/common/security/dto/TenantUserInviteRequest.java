package com.erp.common.security.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record TenantUserInviteRequest(
    @NotBlank @Email @Size(max = 320) String email,
    @Size(max = 100) String firstName,
    @Size(max = 100) String lastName,
    @NotBlank @Size(max = 100) String requestKey,
    @NotNull @Size(max = 20) Set<@NotNull @Positive Long> roleIds) {}
