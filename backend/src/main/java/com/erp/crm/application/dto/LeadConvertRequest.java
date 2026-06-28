package com.erp.crm.application.dto;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 리드 전환 요청.
 *
 * <p>accountId가 null이면 리드의 회사·전화로 신규 고객사를 생성하고, 값이 있으면 기존 고객사를 사용한다. 담당자(Contact)는 항상 리드 정보로 생성된다.
 * createOpportunity가 true면 영업기회도 함께 생성하며 이때 stageId는 필수다.
 */
public record LeadConvertRequest(
    Long accountId,
    boolean createOpportunity,
    @Size(max = 200) String opportunityName,
    Long stageId,
    @PositiveOrZero BigDecimal opportunityAmount,
    @Size(min = 3, max = 3) String opportunityCurrency,
    LocalDate opportunityCloseDate) {}
