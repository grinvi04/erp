package com.erp.finance.domain.repository;

import com.erp.finance.domain.model.FiscalPeriod;
import com.erp.finance.domain.model.FiscalPeriodStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FiscalPeriodRepository extends JpaRepository<FiscalPeriod, Long> {
    List<FiscalPeriod> findByFiscalYearIdOrderByPeriodNumberAsc(Long fiscalYearId);
    boolean existsByFiscalYearIdAndPeriodNumber(Long fiscalYearId, int periodNumber);
    Optional<FiscalPeriod> findByFiscalYearIdAndPeriodNumber(Long fiscalYearId, int periodNumber);
    Optional<FiscalPeriod> findFirstByFiscalYearIdAndStatusOrderByPeriodNumberAsc(Long fiscalYearId, FiscalPeriodStatus status);
    List<FiscalPeriod> findByFiscalYearIdAndStatus(Long fiscalYearId, FiscalPeriodStatus status);
    Optional<FiscalPeriod> findByStartDateLessThanEqualAndEndDateGreaterThanEqual(LocalDate date, LocalDate date2);
}
