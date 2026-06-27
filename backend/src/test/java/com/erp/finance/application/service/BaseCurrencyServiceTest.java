package com.erp.finance.application.service;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.security.Permission;
import com.erp.common.security.PermissionChecker;
import com.erp.finance.application.dto.BaseCurrencyResponse;
import com.erp.finance.application.dto.BaseCurrencyUpdateRequest;
import com.erp.finance.domain.model.TenantBaseCurrency;
import com.erp.finance.domain.repository.TenantBaseCurrencyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class BaseCurrencyServiceTest {

    @Mock private TenantBaseCurrencyRepository repository;
    @Mock private PermissionChecker permissionChecker;

    @InjectMocks private BaseCurrencyService baseCurrencyService;

    @Test
    void getBaseCurrency_noSetting_returnsKrwDefault() {
        // AC-1: 미설정 시 KRW 기본 반환.
        given(repository.findFirstByOrderByIdAsc()).willReturn(Optional.empty());

        BaseCurrencyResponse result = baseCurrencyService.getBaseCurrency();

        assertThat(result.baseCurrency()).isEqualTo("KRW");
    }

    @Test
    void getBaseCurrency_existing_returnsStoredCurrency() {
        given(repository.findFirstByOrderByIdAsc())
            .willReturn(Optional.of(TenantBaseCurrency.of("USD")));

        BaseCurrencyResponse result = baseCurrencyService.getBaseCurrency();

        assertThat(result.baseCurrency()).isEqualTo("USD");
    }

    @Test
    void updateBaseCurrency_firstTime_createsRow() {
        // AC-1: 변경 — 미설정이면 새로 저장.
        given(repository.findFirstByOrderByIdAsc()).willReturn(Optional.empty());
        given(repository.save(any())).willReturn(TenantBaseCurrency.of("USD"));

        BaseCurrencyResponse result =
            baseCurrencyService.updateBaseCurrency(new BaseCurrencyUpdateRequest("USD"));

        assertThat(result.baseCurrency()).isEqualTo("USD");
    }

    @Test
    void updateBaseCurrency_withoutPermission_throwsForbidden() {
        // AC-7: FINANCE_SETTING_WRITE 없으면 변경 불가.
        doThrow(new ErpException(ErrorCode.FORBIDDEN))
            .when(permissionChecker).require(Permission.FINANCE_SETTING_WRITE);

        ErpException ex = assertThrows(ErpException.class, () ->
            baseCurrencyService.updateBaseCurrency(new BaseCurrencyUpdateRequest("USD")));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
    }
}
