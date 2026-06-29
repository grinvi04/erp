package com.erp.finance.application.dto;

import com.erp.finance.domain.model.TaxType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ArInvoiceCreateRequest(
    @NotBlank @Size(max = 30) String invoiceNo,
    @NotNull Long customerId,
    @NotNull LocalDate invoiceDate,
    @NotNull LocalDate dueDate,
    // 공급가액(부가세 제외). 총액 = 공급가액 + 자동계산 세액. 라인 제공 시 합계가 공급가액과 일치해야 한다.
    @NotNull @DecimalMin("0.01") BigDecimal supplyAmount,
    @NotNull TaxType taxType,
    @Size(max = 3) String currency,
    String note,
    @Valid List<ArInvoiceLineRequest> lines) {}
