package com.erp.finance.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.finance.application.dto.BudgetCreateRequest;
import com.erp.finance.application.dto.BudgetResponse;
import com.erp.finance.application.dto.BudgetUpdateRequest;
import com.erp.finance.domain.model.Account;
import com.erp.finance.domain.model.AccountType;
import com.erp.finance.domain.model.Budget;
import com.erp.finance.domain.model.FiscalYear;
import com.erp.finance.domain.model.NormalBalance;
import com.erp.finance.domain.repository.AccountRepository;
import com.erp.finance.domain.repository.BudgetRepository;
import com.erp.finance.domain.repository.FiscalYearRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

  @Mock private BudgetRepository budgetRepository;
  @Mock private FiscalYearRepository fiscalYearRepository;
  @Mock private AccountRepository accountRepository;
  @Mock private com.erp.common.security.PermissionChecker permissionChecker;

  @InjectMocks private BudgetService budgetService;

  private FiscalYear buildFiscalYear() {
    return FiscalYear.of(2025, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));
  }

  private Account buildAccount() {
    return Account.of("5100", "인건비", AccountType.EXPENSE, NormalBalance.DEBIT, null, false);
  }

  @Test
  void create_noDuplicate_returnsBudgetResponse() {
    FiscalYear fy = buildFiscalYear();
    Account account = buildAccount();
    given(fiscalYearRepository.findById(1L)).willReturn(Optional.of(fy));
    given(accountRepository.findById(1L)).willReturn(Optional.of(account));
    given(budgetRepository.existsByFiscalYearIdAndAccountIdAndDepartmentId(1L, 1L, 10L))
        .willReturn(false);
    Budget budget = Budget.of(fy, account, 10L, new BigDecimal("5000000"));
    given(budgetRepository.save(any())).willReturn(budget);

    BudgetResponse result =
        budgetService.create(new BudgetCreateRequest(1L, 1L, 10L, new BigDecimal("5000000")));

    assertThat(result.budgetAmount()).isEqualByComparingTo("5000000");
  }

  @Test
  void create_duplicate_throwsBudgetDuplicate() {
    given(fiscalYearRepository.findById(1L)).willReturn(Optional.of(buildFiscalYear()));
    given(accountRepository.findById(1L)).willReturn(Optional.of(buildAccount()));
    given(budgetRepository.existsByFiscalYearIdAndAccountIdAndDepartmentId(1L, 1L, null))
        .willReturn(true);

    ErpException ex =
        assertThrows(
            ErpException.class,
            () ->
                budgetService.create(
                    new BudgetCreateRequest(1L, 1L, null, new BigDecimal("5000000"))));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.BUDGET_DUPLICATE);
  }

  @Test
  void create_summaryAccount_throwsAccountIsSummary() {
    FiscalYear fy = buildFiscalYear();
    Account summaryAccount =
        Account.of("1000", "자산", AccountType.ASSET, NormalBalance.DEBIT, null, true);
    given(fiscalYearRepository.findById(1L)).willReturn(Optional.of(fy));
    given(accountRepository.findById(1L)).willReturn(Optional.of(summaryAccount));

    ErpException ex =
        assertThrows(
            ErpException.class,
            () ->
                budgetService.create(
                    new BudgetCreateRequest(1L, 1L, null, new BigDecimal("5000000"))));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_IS_SUMMARY);
  }

  @Test
  void update_found_returnsBudgetResponse() {
    FiscalYear fy = buildFiscalYear();
    Account account = buildAccount();
    Budget budget = Budget.of(fy, account, null, new BigDecimal("3000000"));
    ReflectionTestUtils.setField(budget, "version", 3L);
    given(budgetRepository.findById(1L)).willReturn(Optional.of(budget));

    BudgetResponse result =
        budgetService.update(1L, new BudgetUpdateRequest(new BigDecimal("6000000"), 3L));

    assertThat(result.budgetAmount()).isEqualByComparingTo("6000000");
    assertThat(result.version()).isEqualTo(3L);
  }

  @Test
  void update_versionMismatch_throwsOptimisticLockConflict() {
    FiscalYear fy = buildFiscalYear();
    Account account = buildAccount();
    Budget budget = Budget.of(fy, account, null, new BigDecimal("3000000"));
    ReflectionTestUtils.setField(budget, "version", 5L);
    given(budgetRepository.findById(1L)).willReturn(Optional.of(budget));

    assertThrows(
        ObjectOptimisticLockingFailureException.class,
        () -> budgetService.update(1L, new BudgetUpdateRequest(new BigDecimal("6000000"), 3L)));
  }

  @Test
  void findById_notFound_throwsBudgetNotFound() {
    given(budgetRepository.findById(99L)).willReturn(Optional.empty());

    ErpException ex = assertThrows(ErpException.class, () -> budgetService.findById(99L));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.BUDGET_NOT_FOUND);
  }
}
