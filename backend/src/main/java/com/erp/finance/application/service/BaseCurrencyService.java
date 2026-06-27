package com.erp.finance.application.service;

import com.erp.common.security.Permission;
import com.erp.common.security.PermissionChecker;
import com.erp.finance.application.dto.BaseCurrencyResponse;
import com.erp.finance.application.dto.BaseCurrencyUpdateRequest;
import com.erp.finance.domain.model.TenantBaseCurrency;
import com.erp.finance.domain.repository.TenantBaseCurrencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 테넌트 기준통화 설정 — 조회(미설정 시 KRW 기본)·변경(FINANCE_SETTING_WRITE). FiscalYear 마스터 패턴.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BaseCurrencyService {

    private final TenantBaseCurrencyRepository repository;
    private final PermissionChecker permissionChecker;

    public BaseCurrencyResponse getBaseCurrency() {
        permissionChecker.require(Permission.FINANCE_READ);
        return BaseCurrencyResponse.of(currentBaseCurrencyCode());
    }

    /** 환산 등 내부 사용을 위한 현재 테넌트 기준통화 코드(미설정 시 KRW). 권한 검사 없음. */
    public String currentBaseCurrencyCode() {
        return repository.findFirstByOrderByIdAsc()
            .map(TenantBaseCurrency::getBaseCurrency)
            .orElse(TenantBaseCurrency.DEFAULT_BASE_CURRENCY);
    }

    @Transactional
    public BaseCurrencyResponse updateBaseCurrency(BaseCurrencyUpdateRequest request) {
        permissionChecker.require(Permission.FINANCE_SETTING_WRITE);
        TenantBaseCurrency entity = repository.findFirstByOrderByIdAsc()
            .map(existing -> { existing.changeBaseCurrency(request.baseCurrency()); return existing; })
            .orElseGet(() -> repository.save(TenantBaseCurrency.of(request.baseCurrency())));
        return BaseCurrencyResponse.of(entity.getBaseCurrency());
    }
}
