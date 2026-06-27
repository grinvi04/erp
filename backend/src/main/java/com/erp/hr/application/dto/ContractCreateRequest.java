package com.erp.hr.application.dto;

import com.erp.hr.domain.model.ContractType;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ContractCreateRequest(
    @NotNull ContractType contractType,
    @NotNull LocalDate startDate,
    LocalDate endDate,
    BigDecimal baseSalary,
    @NotNull Long positionId,
    Long jobGradeId,
    String note) {}
