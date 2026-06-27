package com.erp.finance.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ApInvoiceCreateRequest(
    @NotBlank @Size(max = 30) String invoiceNo,
    @NotNull Long vendorId,
    @NotNull LocalDate invoiceDate,
    @NotNull LocalDate dueDate,
    @NotNull @DecimalMin("0.01") BigDecimal totalAmount,
    @Size(max = 3) String currency,
    String note,
    // 차변 라인(선택) — 제공 시 합계가 totalAmount와 일치해야 하며, 승인 시 GL 자동 분개의 차변이 된다.
    @Valid List<ApInvoiceLineRequest> lines) {}
