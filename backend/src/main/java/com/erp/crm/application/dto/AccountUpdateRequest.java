package com.erp.crm.application.dto;

import com.erp.crm.domain.model.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record AccountUpdateRequest(
    @NotBlank @Size(max = 200) String name,
    @Size(max = 30) String businessNo,
    @Size(max = 100) String industry,
    @Size(max = 300) String website,
    @Size(max = 30) String phone,
    @Size(max = 500) String address,
    @PositiveOrZero Integer employeeCount,
    @PositiveOrZero BigDecimal annualRevenue,
    @NotNull AccountType accountType,
    @NotBlank @Size(max = 100) String ownerId,
    @NotNull Long version) {}
