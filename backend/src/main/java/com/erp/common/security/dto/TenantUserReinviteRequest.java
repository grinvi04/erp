package com.erp.common.security.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record TenantUserReinviteRequest(
    @NotNull @Size(max = 20) Set<@NotNull @Positive Long> roleIds) {}
