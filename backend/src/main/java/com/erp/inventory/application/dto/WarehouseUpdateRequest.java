package com.erp.inventory.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record WarehouseUpdateRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 500) String address,
        @NotNull Long version
) {}
