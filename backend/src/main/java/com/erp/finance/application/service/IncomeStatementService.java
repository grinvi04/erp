package com.erp.finance.application.service;

import com.erp.common.security.Permission;
import com.erp.common.security.PermissionChecker;
import com.erp.finance.application.dto.IncomeStatementResponse;
import com.erp.finance.application.dto.IncomeStatementResponse.IncomeStatementLine;
import com.erp.finance.domain.model.Account;
import com.erp.finance.domain.model.AccountType;
import com.erp.finance.domain.model.FiscalYear;
import com.erp.finance.domain.repository.AccountBalanceRow;
import com.erp.finance.domain.repository.AccountRepository;
import com.erp.finance.domain.repository.FiscalYearRepository;
import com.erp.finance.domain.repository.JournalEntryRepository;
import com.erp.finance.domain.repository.JournalLineRepository;
import static com.erp.finance.application.service.FinancialStatementSupport.accountsById;
import static com.erp.finance.application.service.FinancialStatementSupport.display;
import static com.erp.finance.application.service.FinancialStatementSupport.resolveFiscalYear;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 손익계산서 — 회계연도 기간의 REVENUE(대변−차변)·EXPENSE(차변−대변) 발생액과 당기순이익(수익−비용)을 산출한다.
 * 부호는 계정 normalBalance 기준으로 처리해 수익·비용 모두 양수로 표시된다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IncomeStatementService {

    private final JournalLineRepository journalLineRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final AccountRepository accountRepository;
    private final FiscalYearRepository fiscalYearRepository;
    private final BaseCurrencyService baseCurrencyService;
    private final PermissionChecker permissionChecker;

    public IncomeStatementResponse getIncomeStatement(Integer year) {
        permissionChecker.require(Permission.FINANCE_READ);
        FiscalYear fy = resolveFiscalYear(fiscalYearRepository, year);
        Map<Long, Account> accounts = accountsById(accountRepository);

        List<IncomeStatementLine> revenues = new ArrayList<>();
        List<IncomeStatementLine> expenses = new ArrayList<>();
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;
        for (AccountBalanceRow r : journalLineRepository.aggregateBetween(fy.getStartDate(), fy.getEndDate())) {
            Account account = accounts.get(r.getAccountId());
            if (account == null || account.isSummary()) {
                continue;
            }
            // 원시(full precision)로 누적 — 표시용 반올림은 행·합계 출력 시점에만 적용한다.
            BigDecimal amount = account.getNormalBalance().balance(r.getDebitSum(), r.getCreditSum());
            if (account.getAccountType() == AccountType.REVENUE) {
                revenues.add(new IncomeStatementLine(account.getCode(), account.getName(), display(amount)));
                totalRevenue = totalRevenue.add(amount);
            } else if (account.getAccountType() == AccountType.EXPENSE) {
                expenses.add(new IncomeStatementLine(account.getCode(), account.getName(), display(amount)));
                totalExpense = totalExpense.add(amount);
            }
        }
        revenues.sort(Comparator.comparing(IncomeStatementLine::accountCode));
        expenses.sort(Comparator.comparing(IncomeStatementLine::accountCode));

        long excluded = journalEntryRepository.countPostedWithoutRateBetween(fy.getStartDate(), fy.getEndDate());
        return new IncomeStatementResponse(baseCurrencyService.currentBaseCurrencyCode(),
                revenues, display(totalRevenue), expenses, display(totalExpense),
                display(totalRevenue.subtract(totalExpense)), excluded);
    }
}
