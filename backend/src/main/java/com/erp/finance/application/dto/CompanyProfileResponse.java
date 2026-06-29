package com.erp.finance.application.dto;

import com.erp.finance.domain.model.CompanyProfile;

/** 테넌트 회사정보(전자세금계산서 공급자) — 상호·사업자번호·대표자·주소·업태·종목. 미설정이면 전 항목 null(빈 응답). */
public record CompanyProfileResponse(
    String companyName,
    String businessNo,
    String representative,
    String address,
    String businessType,
    String businessItem) {

  public static CompanyProfileResponse of(CompanyProfile entity) {
    return new CompanyProfileResponse(
        entity.getCompanyName(),
        entity.getBusinessNo(),
        entity.getRepresentative(),
        entity.getAddress(),
        entity.getBusinessType(),
        entity.getBusinessItem());
  }

  public static CompanyProfileResponse empty() {
    return new CompanyProfileResponse(null, null, null, null, null, null);
  }
}
