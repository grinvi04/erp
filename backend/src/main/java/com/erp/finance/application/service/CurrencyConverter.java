package com.erp.finance.application.service;

import com.erp.common.currency.CurrencyConversionPort;
import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.finance.domain.model.ExchangeRate;
import com.erp.finance.domain.repository.ExchangeRateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Optional;

/**
 * 원통화 금액을 테넌트 기준통화로 환산한다(거래 시점 고정 환산에 사용 — PR2).
 * <ul>
 *   <li>통화 == 기준통화: amount 그대로(rate=1).</li>
 *   <li>그 외: effectiveDate ≤ date 중 최신 환율로 amount × rate.</li>
 *   <li>환율 부재: {@code convert}는 {@link ErrorCode#CURRENCY_RATE_NOT_FOUND}로 명시적 실패,
 *       {@code tryConvert}는 빈 Optional(거래 스냅샷의 미산정 허용 정책 — 0·1로 조용히 처리 금지).</li>
 * </ul>
 *
 * <p>{@link CurrencyConversionPort} 구현 — 타 모듈(CRM 등)은 common 포트로 주입받는다(모듈 경계).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CurrencyConverter implements CurrencyConversionPort {

    private static final int BASE_AMOUNT_SCALE = 2;

    private final ExchangeRateRepository exchangeRateRepository;
    private final BaseCurrencyService baseCurrencyService;

    /** amount(currency)를 date 시점 기준통화로 환산한 금액. 환율 부재 시 예외. */
    public BigDecimal convert(BigDecimal amount, String currency, LocalDate date) {
        String baseCurrency = baseCurrencyService.currentBaseCurrencyCode();
        if (baseCurrency.equals(currency)) {
            return amount.setScale(BASE_AMOUNT_SCALE, RoundingMode.HALF_UP);
        }
        BigDecimal rate = exchangeRateRepository
            .findTopByFromCurrencyAndToCurrencyAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
                currency, baseCurrency, date)
            .map(ExchangeRate::getRate)
            .orElseThrow(() -> new ErpException(ErrorCode.CURRENCY_RATE_NOT_FOUND));
        return amount.multiply(rate).setScale(BASE_AMOUNT_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 거래 시점 스냅샷용 환산 — 환율 부재 시 예외 대신 빈 Optional(거래는 정상 생성, base 미산정).
     * amount가 null이면(예: 금액 미정 Opportunity) 환산 대상이 아니므로 빈 Optional.
     */
    @Override
    public Optional<Conversion> tryConvert(BigDecimal amount, String currency, LocalDate date) {
        if (amount == null) {
            return Optional.empty();
        }
        String baseCurrency = baseCurrencyService.currentBaseCurrencyCode();
        if (baseCurrency.equals(currency)) {
            return Optional.of(new Conversion(
                amount.setScale(BASE_AMOUNT_SCALE, RoundingMode.HALF_UP), BigDecimal.ONE));
        }
        return exchangeRateRepository
            .findTopByFromCurrencyAndToCurrencyAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
                currency, baseCurrency, date)
            .map(rate -> new Conversion(
                amount.multiply(rate.getRate()).setScale(BASE_AMOUNT_SCALE, RoundingMode.HALF_UP),
                rate.getRate()));
    }
}
