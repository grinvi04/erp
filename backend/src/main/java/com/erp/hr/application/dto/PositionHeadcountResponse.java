package com.erp.hr.application.dto;

public record PositionHeadcountResponse(
        Long positionId,
        String positionName,
        long count
) {
}
