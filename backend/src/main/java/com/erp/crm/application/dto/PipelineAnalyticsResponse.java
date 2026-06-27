package com.erp.crm.application.dto;

import java.util.List;

/**
 * 영업 파이프라인 분포 — 단계별 통화 분리 + 기준통화 합계.
 *
 * @param baseCurrency 테넌트 기준통화 코드(미설정 시 KRW)
 * @param stages       단계별 분포(각 단계에 통화별 분리 합계와 기준통화 합계 포함)
 */
public record PipelineAnalyticsResponse(
        String baseCurrency,
        List<PipelineDistributionResponse> stages
) {
}
