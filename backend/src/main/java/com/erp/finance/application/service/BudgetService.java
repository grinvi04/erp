package com.erp.finance.application.service;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.finance.application.dto.BudgetCreateRequest;
import com.erp.finance.application.dto.BudgetResponse;
import com.erp.finance.application.dto.BudgetUpdateRequest;
import com.erp.finance.domain.model.Account;
import com.erp.finance.domain.model.Budget;
import com.erp.finance.domain.model.FiscalYear;
import com.erp.finance.domain.repository.AccountRepository;
import com.erp.finance.domain.repository.BudgetRepository;
import com.erp.finance.domain.repository.FiscalYearRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final FiscalYearRepository fiscalYearRepository;
    private final AccountRepository accountRepository;

    public List<BudgetResponse> findByFiscalYear(Long fiscalYearId) {
        return budgetRepository.findByFiscalYearId(fiscalYearId).stream()
            .map(BudgetResponse::from)
            .toList();
    }

    public BudgetResponse findById(Long id) {
        return BudgetResponse.from(getOrThrow(id));
    }

    @Transactional
    public BudgetResponse create(BudgetCreateRequest request) {
        FiscalYear fy = fiscalYearRepository.findById(request.fiscalYearId())
            .orElseThrow(() -> new ErpException(ErrorCode.FISCAL_YEAR_NOT_FOUND));
        Account account = accountRepository.findById(request.accountId())
            .orElseThrow(() -> new ErpException(ErrorCode.ACCOUNT_NOT_FOUND));
        if (account.isSummary()) {
            throw new ErpException(ErrorCode.ACCOUNT_IS_SUMMARY);
        }

        if (budgetRepository.existsByFiscalYearIdAndAccountIdAndDepartmentId(
                request.fiscalYearId(), request.accountId(), request.departmentId())) {
            throw new ErpException(ErrorCode.BUDGET_DUPLICATE);
        }

        Budget budget = Budget.of(fy, account, request.departmentId(), request.budgetAmount());
        return BudgetResponse.from(budgetRepository.save(budget));
    }

    @Transactional
    public BudgetResponse update(Long id, BudgetUpdateRequest request) {
        Budget budget = getOrThrow(id);
        budget.updateBudgetAmount(request.budgetAmount());
        return BudgetResponse.from(budget);
    }

    @Transactional
    public void delete(Long id) {
        Budget budget = getOrThrow(id);
        budgetRepository.delete(budget);
    }

    private Budget getOrThrow(Long id) {
        return budgetRepository.findById(id)
            .orElseThrow(() -> new ErpException(ErrorCode.BUDGET_NOT_FOUND));
    }
}
