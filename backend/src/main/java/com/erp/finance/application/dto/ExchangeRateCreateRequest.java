package com.erp.finance.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExchangeRateCreateRequest(
    @Pattern(regexp = "^[A-Z]{3}$", message = "통화 코드는 대문자 3자리(ISO 4217)여야 합니다") String fromCurrency,
    @Pattern(regexp = "^[A-Z]{3}$", message = "통화 코드는 대문자 3자리(ISO 4217)여야 합니다") String toCurrency,
    @NotNull LocalDate effectiveDate,
    @NotNull @DecimalMin(value = "0.0", inclusive = false, message = "환율은 0보다 커야 합니다") BigDecimal rate
) {}
