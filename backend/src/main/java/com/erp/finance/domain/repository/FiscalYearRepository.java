package com.erp.finance.domain.repository;

import com.erp.finance.domain.model.FiscalYear;
import com.erp.finance.domain.model.FiscalYearStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FiscalYearRepository extends JpaRepository<FiscalYear, Long> {
    boolean existsByYear(int year);
    List<FiscalYear> findAllByOrderByYearDesc();
    Optional<FiscalYear> findByYear(int year);
    List<FiscalYear> findByStatus(FiscalYearStatus status);
}
