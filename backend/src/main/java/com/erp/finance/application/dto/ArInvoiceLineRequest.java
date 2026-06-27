package com.erp.finance.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** AR 전표 대변 라인(매출·부가세예수금 계정). */
public record ArInvoiceLineRequest(
    @NotNull Long accountId,
    @NotNull @DecimalMin("0.01") BigDecimal amount,
    @Size(max = 500) String description) {}
