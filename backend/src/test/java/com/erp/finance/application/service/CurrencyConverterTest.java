package com.erp.finance.application.service;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CurrencyConverterTest {

    @Mock private ExchangeRateRepository exchangeRateRepository;
    @Mock private BaseCurrencyService baseCurrencyService;

    @InjectMocks private CurrencyConverter currencyConverter;

    @Test
    void convert_sameAsBaseCurrency_returnsAmountUnchanged() {
        // AC-6: 기준통화면 rate=1 — amount 그대로(환율 조회 없음).
        given(baseCurrencyService.currentBaseCurrencyCode()).willReturn("KRW");

        BigDecimal result = currencyConverter.convert(new BigDecimal("1000000"), "KRW", LocalDate.of(2024, 6, 1));

        assertThat(result).isEqualByComparingTo("1000000");
    }

    @Test
    void convert_foreignCurrency_multipliesByRate() {
        // AC-6: base = amount × rate(통화, date).
        given(baseCurrencyService.currentBaseCurrencyCode()).willReturn("KRW");
        given(exchangeRateRepository
            .findTopByFromCurrencyAndToCurrencyAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
                eq("USD"), eq("KRW"), any()))
            .willReturn(Optional.of(ExchangeRate.of("USD", "KRW", LocalDate.of(2024, 1, 1), new BigDecimal("1300.00000000"))));

        BigDecimal result = currencyConverter.convert(new BigDecimal("100"), "USD", LocalDate.of(2024, 6, 1));

        assertThat(result).isEqualByComparingTo("130000.00");
    }

    @Test
    void convert_usesLatestRateOnOrBeforeDate() {
        // AC-3: effectiveDate ≤ 조회일 중 최신 rate 사용 — repo가 그 1건을 반환하면 그 rate로 환산.
        given(baseCurrencyService.currentBaseCurrencyCode()).willReturn("KRW");
        given(exchangeRateRepository
            .findTopByFromCurrencyAndToCurrencyAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
                "USD", "KRW", LocalDate.of(2024, 6, 15)))
            .willReturn(Optional.of(ExchangeRate.of("USD", "KRW", LocalDate.of(2024, 6, 10), new BigDecimal("1350.00000000"))));

        BigDecimal result = currencyConverter.convert(new BigDecimal("10"), "USD", LocalDate.of(2024, 6, 15));

        assertThat(result).isEqualByComparingTo("13500.00");
    }

    @Test
    void convert_noRate_throwsCurrencyRateNotFound() {
        // AC-5: 환율 부재 → 명시적 실패(0·1로 조용히 처리 금지).
        given(baseCurrencyService.currentBaseCurrencyCode()).willReturn("KRW");
        given(exchangeRateRepository
            .findTopByFromCurrencyAndToCurrencyAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
                eq("JPY"), eq("KRW"), any()))
            .willReturn(Optional.empty());

        ErpException ex = assertThrows(ErpException.class, () ->
            currencyConverter.convert(new BigDecimal("5000"), "JPY", LocalDate.of(2024, 6, 1)));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CURRENCY_RATE_NOT_FOUND);
    }
}
