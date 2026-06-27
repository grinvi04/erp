package com.erp.finance.application.service;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.security.PermissionChecker;
import com.erp.finance.application.dto.ExchangeRateCreateRequest;
import com.erp.finance.application.dto.ExchangeRateResponse;
import com.erp.finance.application.port.out.ExchangeRateProvider;
import com.erp.finance.domain.model.ExchangeRate;
import com.erp.finance.domain.repository.ExchangeRateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ExchangeRateServiceTest {

    @Mock private ExchangeRateRepository repository;
    @Mock private ExchangeRateProvider exchangeRateProvider;
    @Mock private PermissionChecker permissionChecker;

    @InjectMocks private ExchangeRateService exchangeRateService;

    @Test
    void register_newRate_saves() {
        // AC-2: 수동 등록 정상 저장.
        ExchangeRateCreateRequest request = new ExchangeRateCreateRequest(
            "USD", "KRW", LocalDate.of(2024, 6, 1), new BigDecimal("1300.00000000"));
        given(repository.existsByFromCurrencyAndToCurrencyAndEffectiveDate("USD", "KRW", LocalDate.of(2024, 6, 1)))
            .willReturn(false);
        given(repository.save(any())).willReturn(
            ExchangeRate.of("USD", "KRW", LocalDate.of(2024, 6, 1), new BigDecimal("1300.00000000")));

        ExchangeRateResponse result = exchangeRateService.register(request);

        assertThat(result.fromCurrency()).isEqualTo("USD");
        assertThat(result.rate()).isEqualByComparingTo("1300.00000000");
    }

    @Test
    void register_duplicate_throwsExchangeRateDuplicate() {
        // AC-2: 테넌트·통화쌍·일자 UNIQUE — 중복 거부.
        given(repository.existsByFromCurrencyAndToCurrencyAndEffectiveDate("USD", "KRW", LocalDate.of(2024, 6, 1)))
            .willReturn(true);

        ErpException ex = assertThrows(ErpException.class, () ->
            exchangeRateService.register(new ExchangeRateCreateRequest(
                "USD", "KRW", LocalDate.of(2024, 6, 1), new BigDecimal("1300"))));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.EXCHANGE_RATE_DUPLICATE);
        verify(repository, never()).save(any());
    }

    @Test
    void collectFromProvider_providerReturnsRate_savesExchangeRate() {
        // AC-4: provider(포트)로 수집 → ExchangeRate 저장. provider는 mock(실제 HTTP 미호출).
        given(repository.existsByFromCurrencyAndToCurrencyAndEffectiveDate("USD", "KRW", LocalDate.of(2024, 6, 1)))
            .willReturn(false);
        given(exchangeRateProvider.fetchRate("USD", "KRW", LocalDate.of(2024, 6, 1)))
            .willReturn(Optional.of(new BigDecimal("1305.50")));
        given(repository.save(any())).willReturn(
            ExchangeRate.of("USD", "KRW", LocalDate.of(2024, 6, 1), new BigDecimal("1305.50")));

        Optional<ExchangeRateResponse> result =
            exchangeRateService.collectFromProvider("USD", "KRW", LocalDate.of(2024, 6, 1));

        assertThat(result).isPresent();
        assertThat(result.get().rate()).isEqualByComparingTo("1305.50");
        verify(repository).save(any());
    }

    @Test
    void collectFromProvider_providerEmpty_savesNothing() {
        // AC-4 graceful: provider가 못 주면(네트워크·미지원) 저장 없이 빈 Optional.
        given(repository.existsByFromCurrencyAndToCurrencyAndEffectiveDate("JPY", "KRW", LocalDate.of(2024, 6, 1)))
            .willReturn(false);
        given(exchangeRateProvider.fetchRate("JPY", "KRW", LocalDate.of(2024, 6, 1)))
            .willReturn(Optional.empty());

        Optional<ExchangeRateResponse> result =
            exchangeRateService.collectFromProvider("JPY", "KRW", LocalDate.of(2024, 6, 1));

        assertThat(result).isEmpty();
        verify(repository, never()).save(any());
    }
}
