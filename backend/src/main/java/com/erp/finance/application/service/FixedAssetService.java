package com.erp.finance.application.service;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.response.PageResponse;
import com.erp.common.security.Permission;
import com.erp.common.security.PermissionChecker;
import com.erp.finance.application.dto.FixedAssetCreateRequest;
import com.erp.finance.application.dto.FixedAssetResponse;
import com.erp.finance.domain.model.Account;
import com.erp.finance.domain.model.FixedAsset;
import com.erp.finance.domain.repository.AccountRepository;
import com.erp.finance.domain.repository.FixedAssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 고정자산 대장 — 등록·조회. 상각/처분은 별도 서비스(DepreciationPostingService·dispose). */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FixedAssetService {

  private final FixedAssetRepository fixedAssetRepository;
  private final AccountRepository accountRepository;
  private final PermissionChecker permissionChecker;

  public PageResponse<FixedAssetResponse> findAll(Pageable pageable) {
    permissionChecker.require(Permission.FINANCE_READ);
    return PageResponse.from(
        fixedAssetRepository.findByOrderByIdDesc(pageable).map(FixedAssetResponse::from));
  }

  public FixedAssetResponse findById(Long id) {
    permissionChecker.require(Permission.FINANCE_READ);
    return FixedAssetResponse.from(getOrThrow(id));
  }

  @Transactional
  public FixedAssetResponse create(FixedAssetCreateRequest request) {
    permissionChecker.require(Permission.FINANCE_WRITE);
    if (fixedAssetRepository.existsByCode(request.code())) {
      throw new ErpException(ErrorCode.DUPLICATE_CODE, "이미 존재하는 자산 코드: " + request.code());
    }
    Account assetAccount =
        accountRepository
            .findById(request.assetAccountId())
            .orElseThrow(() -> new ErpException(ErrorCode.ACCOUNT_NOT_FOUND));
    FixedAsset asset =
        FixedAsset.register(
            request.code(),
            request.name(),
            request.acquisitionDate(),
            request.acquisitionCost(),
            request.residualValue(),
            request.usefulLifeMonths(),
            request.method(),
            request.decliningAnnualRate(),
            assetAccount);
    return FixedAssetResponse.from(fixedAssetRepository.save(asset));
  }

  FixedAsset getOrThrow(Long id) {
    return fixedAssetRepository
        .findById(id)
        .orElseThrow(() -> new ErpException(ErrorCode.RESOURCE_NOT_FOUND, "고정자산을 찾을 수 없습니다"));
  }
}
