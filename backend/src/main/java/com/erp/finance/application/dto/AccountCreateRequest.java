package com.erp.finance.application.dto;

import com.erp.finance.domain.model.AccountType;
import com.erp.finance.domain.model.NormalBalance;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AccountCreateRequest(
    @NotBlank @Size(max = 20) String code,
    @NotBlank @Size(max = 200) String name,
    @NotNull AccountType accountType,
    @NotNull NormalBalance normalBalance,
    Long parentId,
    boolean isSummary) {}
