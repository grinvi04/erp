package com.erp.inventory.application.dto;

import com.erp.inventory.domain.model.UnitOfMeasure;

public record UomResponse(
        Long id,
        String code,
        String name,
        Long version
) {
    public static UomResponse from(UnitOfMeasure uom) {
        return new UomResponse(uom.getId(), uom.getCode(), uom.getName(), uom.getVersion());
    }
}
