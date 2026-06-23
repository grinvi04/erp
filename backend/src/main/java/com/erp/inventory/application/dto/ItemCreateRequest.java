package com.erp.inventory.application.dto;

import com.erp.inventory.domain.model.CostMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ItemCreateRequest(
        @NotBlank @Size(max = 50) String sku,
        @NotBlank @Size(max = 200) String name,
        String description,
        Long categoryId,
        @NotNull Long uomId,
        @NotNull CostMethod costMethod,
        @NotNull @DecimalMin("0") BigDecimal standardCost,
        @NotNull @DecimalMin("0") BigDecimal reorderPoint,
        @NotNull @DecimalMin("0") BigDecimal reorderQty,
        @NotNull @DecimalMin("0") BigDecimal minStock,
        @NotNull @DecimalMin("0") BigDecimal maxStock,
        boolean lotTracked,
        boolean serialTracked
) {}
