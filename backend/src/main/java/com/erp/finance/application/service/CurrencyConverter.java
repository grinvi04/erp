package com.erp.finance.application.service;

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

/**
 * 원통화 금액을 테넌트 기준통화로 환산한다(거래 시점 고정 환산에 사용 — PR2).
 * <ul>
 *   <li>통화 == 기준통화: amount 그대로(rate=1).</li>
 *   <li>그 외: effectiveDate ≤ date 중 최신 환율로 amount × rate.</li>
 *   <li>환율 부재: {@link ErrorCode#CURRENCY_RATE_NOT_FOUND} — 0·1로 조용히 처리하지 않는다.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CurrencyConverter {

    private static final int BASE_AMOUNT_SCALE = 2;

    private final ExchangeRateRepository exchangeRateRepository;
    private final BaseCurrencyService baseCurrencyService;

    /** amount(currency)를 date 시점 기준통화로 환산한 금액. */
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
}
