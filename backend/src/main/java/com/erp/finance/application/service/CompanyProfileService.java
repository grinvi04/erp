package com.erp.finance.application.service;

import com.erp.common.security.Permission;
import com.erp.common.security.PermissionChecker;
import com.erp.finance.application.dto.CompanyProfileResponse;
import com.erp.finance.application.dto.CompanyProfileUpdateRequest;
import com.erp.finance.domain.model.CompanyProfile;
import com.erp.finance.domain.repository.CompanyProfileRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 테넌트 회사정보 설정 — 전자세금계산서 공급자 신원. 조회(FINANCE_READ, 미설정 시 빈 응답)·변경(FINANCE_SETTING_WRITE, 테넌트당 1행
 * upsert). BaseCurrencyService 단일행 설정 패턴과 동형.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompanyProfileService {

  private final CompanyProfileRepository repository;
  private final PermissionChecker permissionChecker;

  /** 회사정보 조회(FINANCE_READ). 미설정이면 빈 응답(전 항목 null). */
  public CompanyProfileResponse getCompanyProfile() {
    permissionChecker.require(Permission.FINANCE_READ);
    return repository
        .findFirstByOrderByIdAsc()
        .map(CompanyProfileResponse::of)
        .orElseGet(CompanyProfileResponse::empty);
  }

  /** 회사정보 변경(FINANCE_SETTING_WRITE). 테넌트당 1행 upsert — 행이 있으면 갱신, 없으면 생성. */
  @Transactional
  public CompanyProfileResponse updateCompanyProfile(CompanyProfileUpdateRequest request) {
    permissionChecker.require(Permission.FINANCE_SETTING_WRITE);
    CompanyProfile entity =
        repository
            .findFirstByOrderByIdAsc()
            .map(
                existing -> {
                  existing.update(
                      request.companyName(),
                      request.businessNo(),
                      request.representative(),
                      request.address(),
                      request.businessType(),
                      request.businessItem());
                  return existing;
                })
            .orElseGet(
                () ->
                    repository.save(
                        CompanyProfile.of(
                            request.companyName(),
                            request.businessNo(),
                            request.representative(),
                            request.address(),
                            request.businessType(),
                            request.businessItem())));
    return CompanyProfileResponse.of(entity);
  }

  /** 발행 시 공급자 스냅샷·필수검증에 쓰일 현재 회사정보(내부, 권한 검사 없음). 미설정이면 empty. */
  public Optional<CompanyProfile> currentCompanyProfile() {
    return repository.findFirstByOrderByIdAsc();
  }
}
