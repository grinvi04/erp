package com.erp.finance.application.dto;

public record BaseCurrencyResponse(
    String baseCurrency
) {
    public static BaseCurrencyResponse of(String baseCurrency) {
        return new BaseCurrencyResponse(baseCurrency);
    }
}
