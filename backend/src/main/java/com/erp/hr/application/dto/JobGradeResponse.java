package com.erp.hr.application.dto;

import com.erp.hr.domain.model.JobGrade;

import java.math.BigDecimal;

public record JobGradeResponse(
    Long id,
    String code,
    String name,
    int gradeOrder,
    BigDecimal minSalary,
    BigDecimal maxSalary
) {
    public static JobGradeResponse from(JobGrade grade) {
        return new JobGradeResponse(
            grade.getId(),
            grade.getCode(),
            grade.getName(),
            grade.getGradeOrder(),
            grade.getMinSalary(),
            grade.getMaxSalary()
        );
    }
}
