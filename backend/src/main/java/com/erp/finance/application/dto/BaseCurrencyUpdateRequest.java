package com.erp.finance.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record BaseCurrencyUpdateRequest(
    @NotBlank @Pattern(regexp = "^[A-Z]{3}$", message = "통화 코드는 대문자 3자리(ISO 4217)여야 합니다")
        String baseCurrency) {}
