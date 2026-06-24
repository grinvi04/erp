package com.erp.hr.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record JobGradeUpdateRequest(
    @NotBlank @Size(max = 100) String name,
    @Min(0) int gradeOrder,
    @DecimalMin("0") BigDecimal minSalary,
    @DecimalMin("0") BigDecimal maxSalary
) {}
