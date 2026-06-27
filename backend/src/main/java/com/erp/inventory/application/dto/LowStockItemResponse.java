package com.erp.inventory.application.dto;

import java.math.BigDecimal;

public record LowStockItemResponse(
        String sku,
        String name,
        String categoryName,
        BigDecimal currentQty,
        BigDecimal reorderPoint
) {
}
