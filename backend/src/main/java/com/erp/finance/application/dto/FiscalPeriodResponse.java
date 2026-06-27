package com.erp.finance.application.dto;

import com.erp.finance.domain.model.FiscalPeriod;
import com.erp.finance.domain.model.FiscalPeriodStatus;
import java.time.LocalDate;

public record FiscalPeriodResponse(
    Long id,
    Long fiscalYearId,
    int periodNumber,
    LocalDate startDate,
    LocalDate endDate,
    FiscalPeriodStatus status) {
  public static FiscalPeriodResponse from(FiscalPeriod fp) {
    return new FiscalPeriodResponse(
        fp.getId(),
        fp.getFiscalYear().getId(),
        fp.getPeriodNumber(),
        fp.getStartDate(),
        fp.getEndDate(),
        fp.getStatus());
  }
}
