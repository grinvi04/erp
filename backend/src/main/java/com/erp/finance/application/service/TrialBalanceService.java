package com.erp.finance.application.service;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.security.Permission;
import com.erp.common.security.PermissionChecker;
import com.erp.finance.application.dto.TrialBalanceResponse;
import com.erp.finance.application.dto.TrialBalanceResponse.TrialBalanceRow;
import com.erp.finance.domain.model.Account;
import com.erp.finance.domain.model.FiscalYear;
import com.erp.finance.domain.repository.AccountBalanceRow;
import com.erp.finance.domain.repository.AccountRepository;
import com.erp.finance.domain.repository.FiscalYearRepository;
import com.erp.finance.domain.repository.JournalEntryRepository;
import com.erp.finance.domain.repository.JournalLineRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
 * 시산표 — 회계연도 기간(startDate~endDate)의 거래계정별 차변합·대변합·잔액을 기준통화로 산출한다.
 * POSTED·환율 산정 분개만 집계하며, 모든 전기 분개가 차대변 균형이므로 총차변==총대변이 성립한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrialBalanceService {

    private final JournalLineRepository journalLineRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final AccountRepository accountRepository;
    private final FiscalYearRepository fiscalYearRepository;
    private final BaseCurrencyService baseCurrencyService;
    private final PermissionChecker permissionChecker;

    public TrialBalanceResponse getTrialBalance(Integer year) {
        permissionChecker.require(Permission.FINANCE_READ);
        FiscalYear fy = resolveFiscalYear(year);
        Map<Long, Account> accounts = accountRepository.findAll().stream()
                .collect(Collectors.toMap(Account::getId, Function.identity()));

        List<TrialBalanceRow> rows = new ArrayList<>();
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        for (AccountBalanceRow r : journalLineRepository.aggregateBetween(fy.getStartDate(), fy.getEndDate())) {
            Account account = accounts.get(r.getAccountId());
            if (account == null || account.isSummary()) {
                continue;
            }
            // 원시(full precision)로 누적 — 표시용 반올림은 행·합계 출력 시점에만 적용한다.
            BigDecimal debit = r.getDebitSum();
            BigDecimal credit = r.getCreditSum();
            totalDebit = totalDebit.add(debit);
            totalCredit = totalCredit.add(credit);
            rows.add(new TrialBalanceRow(account.getCode(), account.getName(),
                    display(debit), display(credit),
                    display(account.getNormalBalance().balance(debit, credit))));
        }
        rows.sort(Comparator.comparing(TrialBalanceRow::accountCode));

        long excluded = journalEntryRepository.countPostedWithoutRateBetween(fy.getStartDate(), fy.getEndDate());
        // 원시 총차변==총대변이 정확히 성립하므로 표시 반올림 후에도 동일하다.
        return new TrialBalanceResponse(baseCurrencyService.currentBaseCurrencyCode(),
                rows, display(totalDebit), display(totalCredit), excluded);
    }

    private FiscalYear resolveFiscalYear(Integer year) {
        int target = year != null ? year : Year.now().getValue();
        return fiscalYearRepository.findByYear(target)
                .orElseThrow(() -> new ErpException(ErrorCode.FISCAL_YEAR_NOT_FOUND));
    }

    /** 표시용 반올림 — 기준통화 2자리(HALF_UP). 균형·총합 비교는 원시값으로 끝낸 뒤에만 적용한다. */
    private static BigDecimal display(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}
