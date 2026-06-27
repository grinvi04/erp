package com.erp.hr.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record EmployeeUpdateRequest(
    @Size(max = 50) String lastName,
    @Size(max = 50) String firstName,
    @Size(max = 30) String phone,
    @Email @Size(max = 200) String personalEmail,
    @Email @Size(max = 200) String workEmail,
    BigDecimal baseSalary,
    Long managerId,
    @Size(max = 100) String userId,
    @NotNull Long version) {}
