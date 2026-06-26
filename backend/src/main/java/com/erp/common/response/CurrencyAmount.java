package com.erp.common.response;

import java.math.BigDecimal;

public record CurrencyAmount(String currency, BigDecimal amount) {
}
