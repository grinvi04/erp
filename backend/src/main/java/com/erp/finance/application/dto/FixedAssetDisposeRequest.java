package com.erp.finance.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/** 고정자산 처분(매각·폐기) 요청 — 처분일·처분대가(폐기는 0)·대가 수령 계정(현금·미수금, 대가 0이면 불필요). */
public record FixedAssetDisposeRequest(
    @NotNull LocalDate disposalDate,
    @NotNull @DecimalMin("0") BigDecimal proceeds,
    Long proceedsAccountId) {}
