package com.erp.hr.application.dto;

import com.erp.hr.domain.model.Department;

public record DepartmentResponse(
    Long id,
    String code,
    String name,
    Long parentId,
    int depth,
    int sortOrder,
    Long headEmployeeId,
    boolean active,
    Long version
) {
    public static DepartmentResponse from(Department dept) {
        return new DepartmentResponse(
            dept.getId(),
            dept.getCode(),
            dept.getName(),
            dept.getParent() != null ? dept.getParent().getId() : null,
            dept.getDepth(),
            dept.getSortOrder(),
            dept.getHeadEmployeeId(),
            dept.isActive(),
            dept.getVersion()
        );
    }
}
