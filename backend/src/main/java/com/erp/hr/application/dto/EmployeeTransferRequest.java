package com.erp.hr.application.dto;

import jakarta.validation.constraints.NotNull;

public record EmployeeTransferRequest(@NotNull Long departmentId, @NotNull Long positionId) {}
