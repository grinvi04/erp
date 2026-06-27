package com.erp.finance.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record FiscalPeriodCreateRequest(
    @NotNull @Min(1) @Max(12) Integer periodNumber,
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate) {}
