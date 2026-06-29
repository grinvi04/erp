package com.erp.finance.domain.repository;

import com.erp.finance.domain.model.FiscalPeriod;
import com.erp.finance.domain.model.FiscalPeriodStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FiscalPeriodRepository extends JpaRepository<FiscalPeriod, Long> {
  List<FiscalPeriod> findByFiscalYearIdOrderByPeriodNumberAsc(Long fiscalYearId);

  boolean existsByFiscalYearIdAndPeriodNumber(Long fiscalYearId, int periodNumber);

  Optional<FiscalPeriod> findByFiscalYearIdAndPeriodNumber(Long fiscalYearId, int periodNumber);

  Optional<FiscalPeriod> findFirstByFiscalYearIdAndStatusOrderByPeriodNumberAsc(
      Long fiscalYearId, FiscalPeriodStatus status);

  List<FiscalPeriod> findByFiscalYearIdAndStatus(Long fiscalYearId, FiscalPeriodStatus status);

  Optional<FiscalPeriod> findByStartDateLessThanEqualAndEndDateGreaterThanEqual(
      LocalDate date, LocalDate date2);

  // 처분 직전까지 catch-up 상각 대상 — [취득일 이후 종료 ~ 처분월 시작일 이전 종료) 기간만(처분월·취득 전 제외), 오름차순.
  List<FiscalPeriod> findByEndDateGreaterThanEqualAndEndDateLessThanOrderByStartDateAsc(
      LocalDate acquisitionDate, LocalDate disposalPeriodStart);

  // 손상 인식 기간까지 catch-up 상각 대상 — [취득일 이후 종료 ~ 인식기간 종료일 이하] 기간(인식기간 포함·취득 전 제외), 오름차순.
  List<FiscalPeriod> findByEndDateGreaterThanEqualAndEndDateLessThanEqualOrderByStartDateAsc(
      LocalDate acquisitionDate, LocalDate throughPeriodEnd);
}
