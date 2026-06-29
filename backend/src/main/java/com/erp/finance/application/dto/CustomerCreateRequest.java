package com.erp.finance.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerCreateRequest(
    @NotBlank @Size(max = 30) String code,
    @NotBlank @Size(max = 200) String name,
    @Size(max = 30) String businessNo,
    @Size(max = 100) String contactName,
    @Email @Size(max = 200) String contactEmail,
    @Size(max = 30) String contactPhone,
    @Min(0) int paymentTerms,
    // 외상매출금 통제계정(차변) — 설정 시 AR 전표 승인이 이 계정으로 자동 분개된다.
    Long receivablesAccountId,
    // 세금계산서 공급받는자 인적사항(#3) — 대표자·주소·업태·종목. 모두 선택.
    @Size(max = 100) String representativeName,
    @Size(max = 500) String address,
    @Size(max = 200) String businessType,
    @Size(max = 200) String businessItem) {}
