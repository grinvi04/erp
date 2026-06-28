package com.erp.finance.application.dto;

import com.erp.common.response.CurrencyAmount;
import java.math.BigDecimal;
import java.util.List;

/**
 * @param baseCurrency 테넌트 기준통화 코드(미설정 시 KRW)
 * @param unpaidBaseTotal 미지급 금액의 기준통화 환산 합계(base_amount 산정분만, 미산정 행 제외). 산정된 미지급 행이 없으면 null(통화별 분리
 *     합계는 그대로 유지).
 * @param unpaidBaseTotalPartial 환율 미산정으로 환산 합계에서 제외된 미지급 행이 하나라도 있으면 true. 합계가 "일부 미환산"임을 화면이 정직하게
 *     표기하도록 한다(과소 표시를 조용히 숨기지 않음).
 */
public record FinanceSummaryResponse(
    long unpaidInvoices,
    List<CurrencyAmount> unpaidAmounts,
    long draftJournalEntries,
    String baseCurrency,
    BigDecimal unpaidBaseTotal,
    boolean unpaidBaseTotalPartial) {}
