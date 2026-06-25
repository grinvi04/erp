package com.erp.crm.application.dto;

import java.math.BigDecimal;

public record PipelineDistributionResponse(
        Long stageId,
        String stageName,
        int stageOrder,
        long count,
        BigDecimal totalAmount
) {
}
