package com.erp.inventory.application.dto;

import java.math.BigDecimal;

public record MonthlyMovementResponse(
        int month,
        long count,
        BigDecimal totalQty
) {
}
