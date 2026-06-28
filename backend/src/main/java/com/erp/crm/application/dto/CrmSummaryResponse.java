package com.erp.crm.application.dto;

import com.erp.common.response.CurrencyAmount;
import java.math.BigDecimal;
import java.util.List;

/**
 * @param baseCurrency 테넌트 기준통화 코드(미설정 시 KRW)
 * @param openOpportunityBaseTotal 진행중 파이프라인 금액의 기준통화 환산 합계(base_amount 산정분만, 미산정 행 제외). 산정된 진행중 기회가
 *     없으면 null.
 * @param openOpportunityBaseTotalPartial 환율 미산정으로 환산 합계에서 제외된 진행중 기회가 하나라도 있으면 true. 합계가 "일부 미환산"임을
 *     화면이 정직하게 표기하도록 한다.
 */
public record CrmSummaryResponse(
    long openOpportunities,
    List<CurrencyAmount> openOpportunityAmounts,
    long newLeads,
    long openActivities,
    String baseCurrency,
    BigDecimal openOpportunityBaseTotal,
    boolean openOpportunityBaseTotalPartial) {}
