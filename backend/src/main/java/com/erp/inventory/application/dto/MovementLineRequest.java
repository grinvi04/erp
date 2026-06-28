package com.erp.inventory.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record MovementLineRequest(
    @NotNull Long itemId,
    Long fromLocationId,
    Long toLocationId,
    @Size(max = 50) String lotNo,
    @Size(max = 100) String serialNo,
    @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal qty,
    @NotNull @DecimalMin("0") BigDecimal unitCost) {}
