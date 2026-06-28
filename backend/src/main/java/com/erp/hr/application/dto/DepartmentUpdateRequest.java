package com.erp.hr.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DepartmentUpdateRequest(
    @NotBlank @Size(max = 100) String name,
    int sortOrder,
    Long parentId,
    boolean active,
    @NotNull Long version) {}
