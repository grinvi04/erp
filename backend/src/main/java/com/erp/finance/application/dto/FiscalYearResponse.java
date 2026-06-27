package com.erp.finance.application.dto;

import com.erp.finance.domain.model.FiscalYear;
import com.erp.finance.domain.model.FiscalYearStatus;
import java.time.LocalDate;

public record FiscalYearResponse(
    Long id, int year, LocalDate startDate, LocalDate endDate, FiscalYearStatus status) {
  public static FiscalYearResponse from(FiscalYear fy) {
    return new FiscalYearResponse(
        fy.getId(), fy.getYear(), fy.getStartDate(), fy.getEndDate(), fy.getStatus());
  }
}
