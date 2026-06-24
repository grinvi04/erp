package com.erp.hr.application.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record EmployeeTerminateRequest(
    @NotNull LocalDate terminationDate
) {}
