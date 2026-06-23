package com.erp.inventory.application.dto;

import com.erp.inventory.domain.model.CostMethod;
import com.erp.inventory.domain.model.Item;
import java.math.BigDecimal;

public record ItemResponse(
        Long id,
        String sku,
        String name,
        String description,
        Long categoryId,
        String categoryName,
        Long uomId,
        String uomCode,
        String uomName,
        CostMethod costMethod,
        BigDecimal standardCost,
        BigDecimal reorderPoint,
        BigDecimal reorderQty,
        BigDecimal minStock,
        BigDecimal maxStock,
        boolean lotTracked,
        boolean serialTracked,
        boolean active
) {
    public static ItemResponse from(Item item) {
        Long categoryId = item.getCategory() != null ? item.getCategory().getId() : null;
        String categoryName = item.getCategory() != null ? item.getCategory().getName() : null;
        return new ItemResponse(
                item.getId(), item.getSku(), item.getName(), item.getDescription(),
                categoryId, categoryName,
                item.getUom().getId(), item.getUom().getCode(), item.getUom().getName(),
                item.getCostMethod(), item.getStandardCost(),
                item.getReorderPoint(), item.getReorderQty(),
                item.getMinStock(), item.getMaxStock(),
                item.isLotTracked(), item.isSerialTracked(), item.isActive());
    }
}
