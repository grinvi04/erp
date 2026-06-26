package com.erp.inventory.application.dto;

import java.math.BigDecimal;

public record WarehouseStockResponse(
        Long warehouseId,
        String warehouseName,
        BigDecimal totalQty,
        BigDecimal totalValue
) {
}
