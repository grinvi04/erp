package com.erp.finance;

import static org.assertj.core.api.Assertions.assertThat;

import com.erp.common.AbstractIntegrationTest;
import com.erp.common.tenant.TenantContext;
import com.erp.finance.application.dto.IncomeStatementResponse;
import com.erp.finance.application.dto.TrialBalanceResponse;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

class FinancialStatementTenantIsolationIntegrationTest extends AbstractIntegrationTest {

  private static final long TENANT_A = 9101L;
  private static final long TENANT_B = 9102L;
  private static final int YEAR = 2098;

  @Autowired private TransactionTemplate transactionTemplate;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private FiscalYearRepository fiscalYearRepository;
  @Autowired private FiscalPeriodRepository fiscalPeriodRepository;
  @Autowired private AccountRepository accountRepository;
  @Autowired private JournalEntryRepository journalEntryRepository;
  @Autowired private TrialBalanceService trialBalanceService;
  @Autowired private IncomeStatementService incomeStatementService;

  @AfterEach
  void removeSeedData() {
    TenantContext.clear();
    jdbcTemplate.update(
        "delete from finance.journal_line where tenant_id in (?, ?)", TENANT_A, TENANT_B);
    jdbcTemplate.update(
        "delete from finance.journal_entry where tenant_id in (?, ?)", TENANT_A, TENANT_B);
    jdbcTemplate.update(
        "delete from finance.fiscal_period where tenant_id in (?, ?)", TENANT_A, TENANT_B);
    jdbcTemplate.update(
        "delete from finance.fiscal_year where tenant_id in (?, ?)", TENANT_A, TENANT_B);
    jdbcTemplate.update(
        "delete from finance.account where tenant_id in (?, ?)", TENANT_A, TENANT_B);
  }

  @Test
  void reportsExcludePostedEntriesFromAnotherTenant() {
    authenticate("report-reader", "finance:read");
    seedTenant(TENANT_A, "A", new BigDecimal("190000"));
    seedTenant(TENANT_B, "B", new BigDecimal("999999"));

    TenantContext.setTenantId(TENANT_A);
    TrialBalanceResponse trialBalance =
        transactionTemplate.execute(status -> trialBalanceService.getTrialBalance(YEAR));
    IncomeStatementResponse incomeStatement =
        transactionTemplate.execute(status -> incomeStatementService.getIncomeStatement(YEAR));

    assertThat(trialBalance).isNotNull();
    assertThat(trialBalance.totalDebit()).isEqualByComparingTo("190000");
    assertThat(trialBalance.totalCredit()).isEqualByComparingTo("190000");
    assertThat(trialBalance.rows()).noneMatch(row -> row.accountName().startsWith("B사"));
    assertThat(incomeStatement).isNotNull();
    assertThat(incomeStatement.totalRevenue()).isEqualByComparingTo("190000");
  }

  private void seedTenant(long tenantId, String marker, BigDecimal amount) {
    TenantContext.setTenantId(tenantId);
    transactionTemplate.executeWithoutResult(
        status -> {
          FiscalYear fiscalYear =
              fiscalYearRepository.save(
                  FiscalYear.of(YEAR, LocalDate.of(YEAR, 1, 1), LocalDate.of(YEAR, 12, 31)));
          FiscalPeriod period =
              fiscalPeriodRepository.save(
                  FiscalPeriod.of(
                      fiscalYear, 1, LocalDate.of(YEAR, 1, 1), LocalDate.of(YEAR, 12, 31)));
          Account cash =
              accountRepository.save(
                  Account.of(
                      marker + "-10100",
                      marker + "사 현금",
                      AccountType.ASSET,
                      NormalBalance.DEBIT,
                      null,
                      false));
          Account sales =
              accountRepository.save(
                  Account.of(
                      marker + "-40100",
                      marker + "사 매출",
                      AccountType.REVENUE,
                      NormalBalance.CREDIT,
                      null,
                      false));
          JournalEntry entry =
              JournalEntry.create(
                  marker + "-JE-1",
                  LocalDate.of(YEAR, 4, 1),
                  period,
                  marker + "사 매출",
                  JournalEntryType.MANUAL,
                  "KRW");
          entry.addLine(JournalLine.of(entry, 1, cash, amount, BigDecimal.ZERO, null, null));
          entry.addLine(JournalLine.of(entry, 2, sales, BigDecimal.ZERO, amount, null, null));
          entry.submitForApproval();
          entry.post("poster-" + marker);
          entry.applyBaseSnapshot(amount, BigDecimal.ONE);
          journalEntryRepository.save(entry);
        });
  }
}
