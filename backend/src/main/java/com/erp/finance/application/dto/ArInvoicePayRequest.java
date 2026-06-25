package com.erp.finance.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ArInvoicePayRequest(
    @NotNull @DecimalMin("0.01") BigDecimal amount
) {}
