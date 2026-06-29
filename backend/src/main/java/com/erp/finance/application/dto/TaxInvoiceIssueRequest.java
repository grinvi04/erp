package com.erp.finance.application.dto;

import com.erp.finance.domain.model.ChargeType;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * 세금계산서 발행 요청 — AR 인보이스 ID는 경로 변수. 작성일자 미지정 시 AR 인보이스 일자, 청구/영수 미지정 시 청구(CHARGE), 품목명 미지정 시 기본값을
 * 사용한다. 모두 선택.
 */
public record TaxInvoiceIssueRequest(
    LocalDate writeDate, ChargeType chargeType, @Size(max = 200) String itemName, String note) {}
