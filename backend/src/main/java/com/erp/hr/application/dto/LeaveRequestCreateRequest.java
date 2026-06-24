package com.erp.hr.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LeaveRequestCreateRequest(
    @NotNull Long employeeId,
    @NotNull Long leavePolicyId,
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate,
    @NotNull @DecimalMin("0.5") BigDecimal requestedDays,
    String reason
) {}
