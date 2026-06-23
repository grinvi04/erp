package com.erp.finance.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ApInvoiceCreateRequest(
    @NotBlank @Size(max = 30) String invoiceNo,
    @NotNull Long vendorId,
    @NotNull LocalDate invoiceDate,
    @NotNull LocalDate dueDate,
    @NotNull @DecimalMin("0.01") BigDecimal totalAmount,
    @Size(max = 3) String currency,
    String note
) {}
