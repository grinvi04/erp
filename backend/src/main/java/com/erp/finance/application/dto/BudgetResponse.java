package com.erp.finance.application.dto;

import com.erp.finance.domain.model.Budget;

import java.math.BigDecimal;

public record BudgetResponse(
    Long id,
    Long fiscalYearId,
    int fiscalYear,
    Long accountId,
    String accountCode,
    String accountName,
    Long departmentId,
    BigDecimal budgetAmount,
    BigDecimal actualAmount,
    BigDecimal remainingBudget,
    boolean isOverBudget,
    Long version
) {
    public static BudgetResponse from(Budget b) {
        return new BudgetResponse(
            b.getId(),
            b.getFiscalYear().getId(), b.getFiscalYear().getYear(),
            b.getAccount().getId(), b.getAccount().getCode(), b.getAccount().getName(),
            b.getDepartmentId(),
            b.getBudgetAmount(), b.getActualAmount(), b.getRemainingBudget(), b.isOverBudget(),
            b.getVersion());
    }
}
