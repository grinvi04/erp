package com.erp.inventory.application.dto;

import com.erp.inventory.domain.model.LocationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LocationCreateRequest(
        @NotNull Long warehouseId,
        @NotBlank @Size(max = 30) String code,
        @NotBlank @Size(max = 100) String name,
        Long parentId,
        @NotNull LocationType locationType
) {}
