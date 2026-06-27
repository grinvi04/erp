package com.erp.finance.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record BudgetCreateRequest(
    @NotNull Long fiscalYearId,
    @NotNull Long accountId,
    Long departmentId,
    @NotNull @DecimalMin("0.00") BigDecimal budgetAmount) {}
