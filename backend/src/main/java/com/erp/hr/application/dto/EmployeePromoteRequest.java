package com.erp.hr.application.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record EmployeePromoteRequest(
    @NotNull Long positionId,
    Long jobGradeId,
    BigDecimal baseSalary
) {}
