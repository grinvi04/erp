package com.erp.finance.application.dto;

import com.erp.finance.domain.model.ExchangeRate;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ExchangeRateResponse(
    Long id, String fromCurrency, String toCurrency, LocalDate effectiveDate, BigDecimal rate) {
  public static ExchangeRateResponse from(ExchangeRate rate) {
    return new ExchangeRateResponse(
        rate.getId(),
        rate.getFromCurrency(),
        rate.getToCurrency(),
        rate.getEffectiveDate(),
        rate.getRate());
  }
}
