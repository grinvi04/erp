package com.erp.finance.application.service;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.security.Permission;
import com.erp.common.security.PermissionChecker;
import com.erp.finance.application.dto.BalanceSheetResponse;
import com.erp.finance.application.dto.BalanceSheetResponse.BalanceSheetLine;
import com.erp.finance.domain.model.Account;
import com.erp.finance.domain.model.FiscalYear;
import com.erp.finance.domain.repository.AccountBalanceRow;
import com.erp.finance.domain.repository.AccountRepository;
import com.erp.finance.domain.repository.FiscalYearRepository;
import com.erp.finance.domain.repository.JournalEntryRepository;
import com.erp.finance.domain.repository.JournalLineRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 재무상태표 — 연말(endDate) 기준 누적으로 자산·부채·자본 잔액을 산출하고, 당기순이익(누적 수익−비용)을
 * 이익잉여금으로 자본에 가산해 균형(자산 == 부채+자본+당기순이익)을 확인한다.
 *
 * <p>당기순이익은 자산·부채·자본과 동일한 누적 범위(기준일 이전 전체)의 수익·비용으로 산출한다 —
 * 복식부기 항등식(Σ차변=Σ대변)상 이 누적 순이익을 가산해야 균형식이 항상 성립한다(전기 이월분 포함).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BalanceSheetService {

    private static final BigDecimal BALANCE_TOLERANCE = new BigDecimal("0.01");

    private final JournalLineRepository journalLineRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final AccountRepository accountRepository;
    private final FiscalYearRepository fiscalYearRepository;
    private final BaseCurrencyService baseCurrencyService;
    private final PermissionChecker permissionChecker;

    public BalanceSheetResponse getBalanceSheet(Integer year) {
        permissionChecker.require(Permission.FINANCE_READ);
        FiscalYear fy = resolveFiscalYear(year);
        LocalDate asOf = fy.getEndDate();
        Map<Long, Account> accounts = accountRepository.findAll().stream()
                .collect(Collectors.toMap(Account::getId, Function.identity()));

        List<BalanceSheetLine> assets = new ArrayList<>();
        List<BalanceSheetLine> liabilities = new ArrayList<>();
        List<BalanceSheetLine> equity = new ArrayList<>();
        BigDecimal totalAssets = BigDecimal.ZERO;
        BigDecimal totalLiabilities = BigDecimal.ZERO;
        BigDecimal totalEquity = BigDecimal.ZERO;
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;
        for (AccountBalanceRow r : journalLineRepository.aggregateUpTo(asOf)) {
            Account account = accounts.get(r.getAccountId());
            if (account == null) {
                continue;
            }
            BigDecimal amount = account.getNormalBalance().balance(r.getDebitSum(), r.getCreditSum());
            switch (account.getAccountType()) {
                case ASSET -> {
                    assets.add(new BalanceSheetLine(account.getCode(), account.getName(), amount));
                    totalAssets = totalAssets.add(amount);
                }
                case LIABILITY -> {
                    liabilities.add(new BalanceSheetLine(account.getCode(), account.getName(), amount));
                    totalLiabilities = totalLiabilities.add(amount);
                }
                case EQUITY -> {
                    equity.add(new BalanceSheetLine(account.getCode(), account.getName(), amount));
                    totalEquity = totalEquity.add(amount);
                }
                case REVENUE -> totalRevenue = totalRevenue.add(amount);
                case EXPENSE -> totalExpense = totalExpense.add(amount);
                default -> { }
            }
        }
        assets.sort(Comparator.comparing(BalanceSheetLine::accountCode));
        liabilities.sort(Comparator.comparing(BalanceSheetLine::accountCode));
        equity.sort(Comparator.comparing(BalanceSheetLine::accountCode));

        BigDecimal netIncome = totalRevenue.subtract(totalExpense);
        BigDecimal rightSide = totalLiabilities.add(totalEquity).add(netIncome);
        boolean balanced = totalAssets.subtract(rightSide).abs().compareTo(BALANCE_TOLERANCE) <= 0;

        long excluded = journalEntryRepository.countPostedWithoutRateUpTo(asOf);
        return new BalanceSheetResponse(baseCurrencyService.currentBaseCurrencyCode(),
                assets, totalAssets, liabilities, totalLiabilities, equity, totalEquity,
                netIncome, balanced, excluded);
    }

    private FiscalYear resolveFiscalYear(Integer year) {
        int target = year != null ? year : Year.now().getValue();
        return fiscalYearRepository.findByYear(target)
                .orElseThrow(() -> new ErpException(ErrorCode.FISCAL_YEAR_NOT_FOUND));
    }
}
