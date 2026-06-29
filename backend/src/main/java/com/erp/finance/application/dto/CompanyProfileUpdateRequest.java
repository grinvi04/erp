package com.erp.finance.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 회사정보(전자세금계산서 공급자) 설정 변경 — 상호·사업자번호는 필수, 대표자·주소·업태·종목은 선택. 전체 교체(upsert). */
public record CompanyProfileUpdateRequest(
    @NotBlank @Size(max = 200) String companyName,
    @NotBlank @Size(max = 30) String businessNo,
    @Size(max = 100) String representative,
    @Size(max = 500) String address,
    @Size(max = 200) String businessType,
    @Size(max = 200) String businessItem) {}
