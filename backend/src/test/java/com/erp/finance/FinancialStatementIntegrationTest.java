package com.erp.finance;

import com.erp.common.AbstractIntegrationTest;
import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.finance.application.dto.BalanceSheetResponse;
import com.erp.finance.application.dto.IncomeStatementResponse;
import com.erp.finance.application.dto.TrialBalanceResponse;
import com.erp.finance.application.service.BalanceSheetService;
import com.erp.finance.application.service.IncomeStatementService;
import com.erp.finance.application.service.TrialBalanceService;
import com.erp.finance.domain.model.Account;
import com.erp.finance.domain.model.AccountType;
import com.erp.finance.domain.model.FiscalPeriod;
import com.erp.finance.domain.model.FiscalYear;
import com.erp.finance.domain.model.JournalEntry;
import com.erp.finance.domain.model.JournalEntryType;
import com.erp.finance.domain.model.JournalLine;
import com.erp.finance.domain.model.NormalBalance;
import com.erp.finance.domain.repository.AccountRepository;
import com.erp.finance.domain.repository.FiscalPeriodRepository;
import com.erp.finance.domain.repository.FiscalYearRepository;
import com.erp.finance.domain.repository.JournalEntryRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 재무제표(시산표·손익계산서·재무상태표) 통합 검증.
 * 계정 유형별 POSTED 분개(차대변 균형)를 시드하고 균형·환산·필터·권한·빈데이터를 확인한다.
 */
@Transactional
class FinancialStatementIntegrationTest extends AbstractIntegrationTest {

    @Autowired private TrialBalanceService trialBalanceService;
    @Autowired private IncomeStatementService incomeStatementService;
    @Autowired private BalanceSheetService balanceSheetService;
    @Autowired private AccountRepository accountRepository;
    @Autowired private FiscalYearRepository fiscalYearRepository;
    @Autowired private FiscalPeriodRepository fiscalPeriodRepository;
    @Autowired private JournalEntryRepository journalEntryRepository;

    private FiscalPeriod period2025;
    private Account cash;
    private Account payable;
    private Account capital;
    private Account sales;
    private Account supplies;

    @BeforeEach
    void setUp() {
        authenticate("reader", "finance:read");
        FiscalYear fy = fiscalYearRepository.save(FiscalYear.of(2025,
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31)));
        period2025 = fiscalPeriodRepository.save(FiscalPeriod.of(fy, 1,
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31)));

        cash = accountRepository.save(Account.of("10100", "현금", AccountType.ASSET, NormalBalance.DEBIT, null, false));
        payable = accountRepository.save(Account.of("25100", "외상매입금", AccountType.LIABILITY, NormalBalance.CREDIT, null, false));
        capital = accountRepository.save(Account.of("30100", "자본금", AccountType.EQUITY, NormalBalance.CREDIT, null, false));
        sales = accountRepository.save(Account.of("40100", "매출", AccountType.REVENUE, NormalBalance.CREDIT, null, false));
        supplies = accountRepository.save(Account.of("51100", "소모품비", AccountType.EXPENSE, NormalBalance.DEBIT, null, false));
    }

    /** 표준 KRW(rate=1) 시드: 자산·부채·자본·수익·비용을 모두 포함하는 균형 분개 3건. */
    private void seedStandard() {
        seedPosted(period2025, LocalDate.of(2025, 3, 1), "JE-1", "KRW", BigDecimal.ONE,
                new Posting(cash, new BigDecimal("100000"), null),
                new Posting(sales, null, new BigDecimal("100000")));
        seedPosted(period2025, LocalDate.of(2025, 3, 2), "JE-2", "KRW", BigDecimal.ONE,
                new Posting(supplies, new BigDecimal("40000"), null),
                new Posting(payable, null, new BigDecimal("40000")));
        seedPosted(period2025, LocalDate.of(2025, 3, 3), "JE-3", "KRW", BigDecimal.ONE,
                new Posting(cash, new BigDecimal("50000"), null),
                new Posting(capital, null, new BigDecimal("50000")));
    }

    // AC-1: 시산표 총차변 == 총대변
    @Test
    void trialBalance_totalsAreBalanced() {
        seedStandard();
        TrialBalanceResponse tb = trialBalanceService.getTrialBalance(2025);

        assertThat(tb.totalDebit()).isEqualByComparingTo("190000"); // 현금 150,000 + 소모품비 40,000
        assertThat(tb.totalCredit()).isEqualByComparingTo("190000"); // 매출 100,000 + 외매 40,000 + 자본 50,000
        assertThat(tb.totalDebit()).isEqualByComparingTo(tb.totalCredit());
        assertThat(tb.rows()).hasSize(5);
        assertThat(tb.excludedEntryCount()).isZero();
    }

    // AC-2: DRAFT/PENDING_APPROVAL/REVERSED 분개는 모든 집계에서 제외
    @Test
    void trialBalance_excludesNonPostedEntries() {
        seedStandard();
        // 환율은 산정돼 있으나 상태가 비-POSTED → 집계 제외돼야 한다
        seedWithStatus(EntryState.DRAFT, "JE-DRAFT", new BigDecimal("11111"));
        seedWithStatus(EntryState.PENDING, "JE-PENDING", new BigDecimal("22222"));
        seedWithStatus(EntryState.REVERSED, "JE-REVERSED", new BigDecimal("33333"));

        TrialBalanceResponse tb = trialBalanceService.getTrialBalance(2025);
        assertThat(tb.totalDebit()).isEqualByComparingTo("190000");
        assertThat(tb.totalCredit()).isEqualByComparingTo("190000");
    }

    // AC-3: 분개 통화≠기준통화 — 금액 × exchangeRate로 환산 집계 (USD rate 1300)
    @Test
    void incomeStatement_convertsForeignCurrencyByExchangeRate() {
        seedPosted(period2025, LocalDate.of(2025, 5, 1), "JE-USD", "USD", new BigDecimal("1300"),
                new Posting(cash, new BigDecimal("100"), null),
                new Posting(sales, null, new BigDecimal("100")));

        IncomeStatementResponse is = incomeStatementService.getIncomeStatement(2025);
        assertThat(is.totalRevenue()).isEqualByComparingTo("130000"); // 100 USD × 1300
        assertThat(is.revenues()).singleElement()
                .satisfies(line -> assertThat(line.amount()).isEqualByComparingTo("130000"));

        TrialBalanceResponse tb = trialBalanceService.getTrialBalance(2025);
        assertThat(tb.totalDebit()).isEqualByComparingTo("130000");
    }

    // AC-4: exchangeRate=null(미산정) 분개 제외 + excludedEntryCount 보고
    @Test
    void trialBalance_excludesUnpricedEntriesAndReportsCount() {
        seedStandard();
        // 환율 미산정(rate=null) — 집계 제외, 건수만 보고
        seedPosted(period2025, LocalDate.of(2025, 6, 1), "JE-NULLRATE", "USD", null,
                new Posting(cash, new BigDecimal("99999"), null),
                new Posting(sales, null, new BigDecimal("99999")));

        TrialBalanceResponse tb = trialBalanceService.getTrialBalance(2025);
        assertThat(tb.totalDebit()).isEqualByComparingTo("190000"); // 99999 미포함
        assertThat(tb.excludedEntryCount()).isEqualTo(1);

        // 손익계산서도 동일하게 제외
        assertThat(incomeStatementService.getIncomeStatement(2025).totalRevenue())
                .isEqualByComparingTo("100000");
    }

    // AC-5: 손익계산서 당기순이익 = 수익 − 비용
    @Test
    void incomeStatement_netIncomeIsRevenueMinusExpense() {
        seedStandard();
        IncomeStatementResponse is = incomeStatementService.getIncomeStatement(2025);
        assertThat(is.totalRevenue()).isEqualByComparingTo("100000");
        assertThat(is.totalExpense()).isEqualByComparingTo("40000");
        assertThat(is.netIncome()).isEqualByComparingTo("60000");
    }

    // AC-6: 재무상태표 자산합 == 부채합 + 자본합 + 당기순이익
    @Test
    void balanceSheet_assetsEqualLiabilitiesPlusEquityPlusNetIncome() {
        seedStandard();
        BalanceSheetResponse bs = balanceSheetService.getBalanceSheet(2025);
        assertThat(bs.totalAssets()).isEqualByComparingTo("150000"); // 현금 100,000 + 50,000
        assertThat(bs.totalLiabilities()).isEqualByComparingTo("40000");
        assertThat(bs.totalEquity()).isEqualByComparingTo("50000");
        assertThat(bs.netIncome()).isEqualByComparingTo("60000");
        assertThat(bs.balanced()).isTrue();
        assertThat(bs.totalAssets()).isEqualByComparingTo(
                bs.totalLiabilities().add(bs.totalEquity()).add(bs.netIncome()));
    }

    // AC-7: 시산표·IS는 해당 연도만, BS는 연말 이전 누적(전기 분개 포함)
    @Test
    void yearFilter_periodExcludesPriorYearButCumulativeIncludesIt() {
        seedStandard();
        // 전기(2024) 분개: 현금 70,000 / 자본 70,000
        FiscalYear fy2024 = fiscalYearRepository.save(FiscalYear.of(2024,
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31)));
        FiscalPeriod period2024 = fiscalPeriodRepository.save(FiscalPeriod.of(fy2024, 1,
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31)));
        seedPosted(period2024, LocalDate.of(2024, 6, 1), "JE-2024", "KRW", BigDecimal.ONE,
                new Posting(cash, new BigDecimal("70000"), null),
                new Posting(capital, null, new BigDecimal("70000")));

        // 시산표 2025 — 전기(2024) 분개 비포함
        TrialBalanceResponse tb = trialBalanceService.getTrialBalance(2025);
        assertThat(tb.totalDebit()).isEqualByComparingTo("190000");

        // 재무상태표 2025 — 누적이므로 2024 분개 포함, 균형 유지
        BalanceSheetResponse bs = balanceSheetService.getBalanceSheet(2025);
        assertThat(bs.totalAssets()).isEqualByComparingTo("220000"); // 150,000 + 70,000
        assertThat(bs.totalEquity()).isEqualByComparingTo("120000"); // 50,000 + 70,000
        assertThat(bs.netIncome()).isEqualByComparingTo("60000");
        assertThat(bs.balanced()).isTrue();
    }

    // AC-8: FINANCE_READ 권한 없으면 FORBIDDEN(403)
    @Test
    void reports_requireFinanceReadPermission() {
        authenticate("noperm", "hr:employee:read");
        assertThatThrownBy(() -> trialBalanceService.getTrialBalance(2025))
                .isInstanceOf(ErpException.class)
                .extracting(e -> ((ErpException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    // AC-9: 전기 분개 0건 — 빈 리포트(합계 0, 균형 유지), 오류 아님
    @Test
    void emptyData_returnsZeroTotalsAndStaysBalanced() {
        TrialBalanceResponse tb = trialBalanceService.getTrialBalance(2025);
        assertThat(tb.rows()).isEmpty();
        assertThat(tb.totalDebit()).isEqualByComparingTo("0");
        assertThat(tb.totalCredit()).isEqualByComparingTo("0");

        IncomeStatementResponse is = incomeStatementService.getIncomeStatement(2025);
        assertThat(is.netIncome()).isEqualByComparingTo("0");

        BalanceSheetResponse bs = balanceSheetService.getBalanceSheet(2025);
        assertThat(bs.totalAssets()).isEqualByComparingTo("0");
        assertThat(bs.balanced()).isTrue();
    }

    // AC-10(회귀): 소수 환율·다계정 분산 — 계정별 반올림 누적오차가 총차변≠총대변·BS 불균형을 만들지 않아야 한다.
    // 차변은 한 계정(현금)에 집계되고 대변은 4개 계정에 분산돼 계정 분할이 다르다 →
    // 쿼리가 계정별 ROUND 후 재합산하던 (수정 전) 코드에서는 Σ ROUND ≠ ROUND Σ로 0.02 오차가 나 실패한다.
    @Test
    void decimalExchangeRate_perAccountRoundingDoesNotBreakBalance() {
        Account sales2 = accountRepository.save(
                Account.of("40200", "기타매출", AccountType.REVENUE, NormalBalance.CREDIT, null, false));
        BigDecimal rate = new BigDecimal("1234.5678"); // 소수 환율 — 0.01 × rate = 12.345678 (계정별 ROUND 시 12.35)
        BigDecimal cents = new BigDecimal("0.01");
        // 차변 현금 0.01 / 대변 4개 계정 0.01 — 4건. 차변 합 0.04×rate=49.382712, 대변 합도 동일(원시).
        seedPosted(period2025, LocalDate.of(2025, 7, 1), "JE-DEC-1", "USD", rate,
                new Posting(cash, cents, null), new Posting(payable, null, cents));
        seedPosted(period2025, LocalDate.of(2025, 7, 2), "JE-DEC-2", "USD", rate,
                new Posting(cash, cents, null), new Posting(capital, null, cents));
        seedPosted(period2025, LocalDate.of(2025, 7, 3), "JE-DEC-3", "USD", rate,
                new Posting(cash, cents, null), new Posting(sales, null, cents));
        seedPosted(period2025, LocalDate.of(2025, 7, 4), "JE-DEC-4", "USD", rate,
                new Posting(cash, cents, null), new Posting(sales2, null, cents));

        // 시산표: 총차변 == 총대변 (원시 49.382712, 표시 49.38) — 정확 일치
        TrialBalanceResponse tb = trialBalanceService.getTrialBalance(2025);
        assertThat(tb.totalDebit()).isEqualByComparingTo("49.38");
        assertThat(tb.totalCredit()).isEqualByComparingTo("49.38");
        assertThat(tb.totalDebit()).isEqualByComparingTo(tb.totalCredit());
        assertThat(tb.excludedEntryCount()).isZero();

        // 재무상태표: 자산 == 부채+자본+당기순이익, 균형 유지 (수정 전이라면 0.02 오차로 false)
        BalanceSheetResponse bs = balanceSheetService.getBalanceSheet(2025);
        assertThat(bs.totalAssets()).isEqualByComparingTo("49.38");
        assertThat(bs.balanced()).isTrue();
    }

    // --- 시드 헬퍼 ---

    private record Posting(Account account, BigDecimal debit, BigDecimal credit) {}

    private enum EntryState { DRAFT, PENDING, REVERSED }

    private JournalEntry buildEntry(FiscalPeriod period, LocalDate date, String entryNo,
                                    String currency, Posting... postings) {
        JournalEntry je = JournalEntry.create(entryNo, date, period, "테스트 분개",
                JournalEntryType.MANUAL, currency);
        int lineNo = 1;
        for (Posting p : postings) {
            je.addLine(JournalLine.of(je, lineNo++, p.account(),
                    p.debit() != null ? p.debit() : BigDecimal.ZERO,
                    p.credit() != null ? p.credit() : BigDecimal.ZERO, null, null));
        }
        return je;
    }

    private void seedPosted(FiscalPeriod period, LocalDate date, String entryNo, String currency,
                            BigDecimal rate, Posting... postings) {
        JournalEntry je = buildEntry(period, date, entryNo, currency, postings);
        je.submitForApproval();
        je.post("poster");
        if (rate != null) {
            je.applyBaseSnapshot(je.getTotalDebit().multiply(rate), rate);
        }
        journalEntryRepository.save(je);
    }

    /** 비-POSTED 상태 분개(환율은 산정) — 상태로만 제외되는지 검증용. 현금/매출 균형 1건. */
    private void seedWithStatus(EntryState state, String entryNo, BigDecimal amount) {
        JournalEntry je = buildEntry(period2025, LocalDate.of(2025, 4, 1), entryNo, "KRW",
                new Posting(cash, amount, null),
                new Posting(sales, null, amount));
        switch (state) {
            case DRAFT -> { /* DRAFT 유지 */ }
            case PENDING -> je.submitForApproval();
            case REVERSED -> {
                je.submitForApproval();
                je.post("poster");
                je.markReversed();
            }
        }
        je.applyBaseSnapshot(amount, BigDecimal.ONE);
        journalEntryRepository.save(je);
    }
}
