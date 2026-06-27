package com.erp.finance.application.service;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.security.Permission;
import com.erp.common.security.PermissionChecker;
import com.erp.finance.application.dto.BaseCurrencyResponse;
import com.erp.finance.application.dto.BaseCurrencyUpdateRequest;
import com.erp.finance.domain.model.TenantBaseCurrency;
import com.erp.finance.domain.repository.ApInvoiceRepository;
import com.erp.finance.domain.repository.ArInvoiceRepository;
import com.erp.finance.domain.repository.JournalEntryRepository;
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
    @Mock private ApInvoiceRepository apInvoiceRepository;
    @Mock private ArInvoiceRepository arInvoiceRepository;
    @Mock private JournalEntryRepository journalEntryRepository;
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
    void updateBaseCurrency_snapshotExists_throwsNotAllowed() {
        // 가드: 다른 통화로 변경 시 base_amount 스냅샷이 있으면 거부(409). 기존 설정 불변(save 미호출).
        given(repository.findFirstByOrderByIdAsc())
            .willReturn(Optional.of(TenantBaseCurrency.of("KRW")));
        given(apInvoiceRepository.existsByBaseAmountIsNotNull()).willReturn(true);

        ErpException ex = assertThrows(ErpException.class, () ->
            baseCurrencyService.updateBaseCurrency(new BaseCurrencyUpdateRequest("USD")));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.BASE_CURRENCY_CHANGE_NOT_ALLOWED);
    }

    @Test
    void updateBaseCurrency_noSnapshot_changeAllowed() {
        // 가드 미발동: 스냅샷이 전혀 없으면(아직 FX 거래 없음) 다른 통화로 변경 허용.
        given(repository.findFirstByOrderByIdAsc())
            .willReturn(Optional.of(TenantBaseCurrency.of("USD")));

        BaseCurrencyResponse result =
            baseCurrencyService.updateBaseCurrency(new BaseCurrencyUpdateRequest("EUR"));

        assertThat(result.baseCurrency()).isEqualTo("EUR");
    }

    @Test
    void updateBaseCurrency_sameValue_noOpAllowedEvenWithSnapshot() {
        // 동일 값 PUT(no-op)은 스냅샷이 있어도 허용 — 통화가 실제로 바뀌지 않으므로 가드 미발동.
        given(repository.findFirstByOrderByIdAsc())
            .willReturn(Optional.of(TenantBaseCurrency.of("USD")));

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
