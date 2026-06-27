package com.erp.inventory.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WarehouseCreateRequest(
    @NotBlank @Size(max = 20) String code,
    @NotBlank @Size(max = 100) String name,
    @Size(max = 500) String address) {}
