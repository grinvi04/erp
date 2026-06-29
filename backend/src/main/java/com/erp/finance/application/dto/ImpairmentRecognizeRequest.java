package com.erp.finance.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** 손상차손 인식 요청 — 대상 회계기간·회수가능액(0 이상). */
public record ImpairmentRecognizeRequest(
    @NotNull Long fiscalPeriodId,
    @NotNull @DecimalMin("0.00") @Digits(integer = 18, fraction = 2)
        BigDecimal recoverableAmount) {}
