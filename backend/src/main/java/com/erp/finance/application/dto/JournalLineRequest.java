package com.erp.finance.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record JournalLineRequest(
    @NotNull Long accountId,
    @NotNull @DecimalMin("0.00") BigDecimal debitAmount,
    @NotNull @DecimalMin("0.00") BigDecimal creditAmount,
    String description,
    Long departmentId
) {}
