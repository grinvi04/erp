package com.erp.finance.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** AP 전표 차변 라인(비용/자산·부가세 계정). */
public record ApInvoiceLineRequest(
    @NotNull Long accountId,
    @NotNull @DecimalMin("0.01") BigDecimal amount,
    @Size(max = 500) String description) {}
