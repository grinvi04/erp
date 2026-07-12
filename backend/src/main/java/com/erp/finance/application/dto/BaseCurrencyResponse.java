package com.erp.finance.application.dto;

public record BaseCurrencyResponse(String baseCurrency, Long version) {
  public static BaseCurrencyResponse of(String baseCurrency, Long version) {
    return new BaseCurrencyResponse(baseCurrency, version);
  }

  public static BaseCurrencyResponse of(String baseCurrency) {
    return of(baseCurrency, null);
  }
}
