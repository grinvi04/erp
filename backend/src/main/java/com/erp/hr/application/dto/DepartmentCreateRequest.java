package com.erp.hr.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DepartmentCreateRequest(
    @NotBlank @Size(max = 30) String code,
    @NotBlank @Size(max = 100) String name,
    Long parentId,
    int sortOrder) {}
