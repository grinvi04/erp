package com.erp.crm.application.dto;

import com.erp.common.response.CurrencyAmount;
import java.util.List;

public record PipelineDistributionResponse(
        Long stageId,
        String stageName,
        int stageOrder,
        long count,
        List<CurrencyAmount> amounts
) {
}
