package com.erp.crm.application.dto;

import com.erp.common.response.CurrencyAmount;
import java.math.BigDecimal;
import java.util.List;

/**
 * @param amounts 단계별 통화 분리 합계(기존 표시 유지)
 * @param baseTotal 단계별 기준통화 환산 합계(base_amount 산정분만). 산정된 기회가 없으면 null.
 */
public record PipelineDistributionResponse(
    Long stageId,
    String stageName,
    int stageOrder,
    long count,
    List<CurrencyAmount> amounts,
    BigDecimal baseTotal) {}
