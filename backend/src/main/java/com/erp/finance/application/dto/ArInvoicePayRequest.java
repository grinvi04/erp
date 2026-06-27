package com.erp.finance.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ArInvoicePayRequest(
    @NotNull @DecimalMin("0.01") BigDecimal amount,
    // 수금 계정(현금·예금) + 수금일 — 제공 시 수금 분개를 자동 생성한다(미제공 시 잔액만 갱신).
    Long cashAccountId,
    LocalDate paymentDate) {}
