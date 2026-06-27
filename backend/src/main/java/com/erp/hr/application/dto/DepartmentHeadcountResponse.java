package com.erp.hr.application.dto;

public record DepartmentHeadcountResponse(
        Long departmentId,
        String departmentName,
        long count
) {
}
