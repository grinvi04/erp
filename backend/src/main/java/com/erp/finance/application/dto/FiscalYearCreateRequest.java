package com.erp.finance.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record FiscalYearCreateRequest(
    @NotNull @Min(2000) @Max(2100) Integer year,
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate) {}
