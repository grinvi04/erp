package com.erp.finance.domain.repository;

import com.erp.finance.domain.model.Budget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {
    List<Budget> findByFiscalYearId(Long fiscalYearId);
    List<Budget> findByFiscalYearIdAndDepartmentId(Long fiscalYearId, Long departmentId);
    Optional<Budget> findByFiscalYearIdAndAccountIdAndDepartmentId(Long fiscalYearId, Long accountId, Long departmentId);
    boolean existsByFiscalYearIdAndAccountIdAndDepartmentId(Long fiscalYearId, Long accountId, Long departmentId);
}
