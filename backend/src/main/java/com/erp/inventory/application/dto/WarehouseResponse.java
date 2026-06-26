package com.erp.inventory.application.dto;

import com.erp.inventory.domain.model.Warehouse;

public record WarehouseResponse(
        Long id,
        String code,
        String name,
        String address,
        boolean active,
        Long version
) {
    public static WarehouseResponse from(Warehouse w) {
        return new WarehouseResponse(w.getId(), w.getCode(), w.getName(), w.getAddress(), w.isActive(), w.getVersion());
    }
}
