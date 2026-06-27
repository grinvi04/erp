package com.erp.crm.application.dto;

import com.erp.common.response.CurrencyAmount;
import java.math.BigDecimal;
import java.util.List;

/**
 * @param baseCurrency 테넌트 기준통화 코드(미설정 시 KRW)
 * @param openOpportunityBaseTotal 진행중 파이프라인 금액의 기준통화 환산 합계(base_amount 산정분만, 미산정 행 제외). 산정된 진행중 기회가
 *     없으면 null.
 */
public record CrmSummaryResponse(
    long openOpportunities,
    List<CurrencyAmount> openOpportunityAmounts,
    long newLeads,
    long openActivities,
    String baseCurrency,
    BigDecimal openOpportunityBaseTotal) {}
