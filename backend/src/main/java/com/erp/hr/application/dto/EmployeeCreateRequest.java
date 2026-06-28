package com.erp.hr.application.dto;

import com.erp.hr.domain.model.EmploymentType;
import com.erp.hr.domain.model.PersonalInfo;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record EmployeeCreateRequest(
    @NotBlank @Size(max = 30) String employeeNo,
    @NotBlank @Size(max = 50) String lastName,
    @NotBlank @Size(max = 50) String firstName,
    LocalDate dateOfBirth,
    PersonalInfo.Gender gender,
    String nationalId,
    String phone,
    String personalEmail,
    @NotNull Long departmentId,
    @NotNull Long positionId,
    Long jobGradeId,
    @NotNull LocalDate hireDate,
    @NotNull EmploymentType employmentType,
    @NotBlank @Email String workEmail,
    BigDecimal baseSalary,
    Long managerId,
    @Size(max = 100) String userId) {}
