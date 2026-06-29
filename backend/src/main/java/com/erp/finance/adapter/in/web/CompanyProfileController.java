package com.erp.finance.adapter.in.web;

import com.erp.common.response.ApiResponse;
import com.erp.finance.application.dto.CompanyProfileResponse;
import com.erp.finance.application.dto.CompanyProfileUpdateRequest;
import com.erp.finance.application.service.CompanyProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 테넌트 회사정보 — 전자세금계산서 공급자 신원. 조회(FINANCE_READ)·변경(FINANCE_SETTING_WRITE, 서비스에서 검사). */
@RestController
@RequestMapping("/api/finance/company-profile")
@RequiredArgsConstructor
public class CompanyProfileController {

  private final CompanyProfileService companyProfileService;

  @GetMapping
  public ResponseEntity<ApiResponse<CompanyProfileResponse>> get() {
    return ResponseEntity.ok(ApiResponse.ok(companyProfileService.getCompanyProfile()));
  }

  @PutMapping
  public ResponseEntity<ApiResponse<CompanyProfileResponse>> update(
      @Valid @RequestBody CompanyProfileUpdateRequest request) {
    return ResponseEntity.ok(ApiResponse.ok(companyProfileService.updateCompanyProfile(request)));
  }
}
