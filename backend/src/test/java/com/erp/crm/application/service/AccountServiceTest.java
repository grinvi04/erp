package com.erp.crm.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.crm.application.dto.AccountCreateRequest;
import com.erp.crm.application.dto.AccountResponse;
import com.erp.crm.application.dto.AccountUpdateRequest;
import com.erp.crm.domain.model.Account;
import com.erp.crm.domain.model.AccountType;
import com.erp.crm.domain.repository.CrmAccountRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

  @Mock private CrmAccountRepository accountRepository;
  @Mock private com.erp.common.security.PermissionChecker permissionChecker;
  @Mock private com.erp.common.security.CurrentUserProvider currentUserProvider;
  @InjectMocks private CrmAccountService accountService;

  private Account buildAccount() {
    return Account.of(
        "ACC-001",
        "테스트고객사",
        null,
        "IT",
        null,
        "02-000-0000",
        "서울시",
        100,
        new BigDecimal("5000000000"),
        AccountType.CUSTOMER,
        "user-sub-001");
  }

  @Test
  void create_newCode_savesWithCurrentUserAsOwner() {
    given(accountRepository.existsByCode("ACC-001")).willReturn(false);
    given(currentUserProvider.getCurrentUserId()).willReturn("auth-user-sub");
    given(accountRepository.save(any(Account.class))).willAnswer(inv -> inv.getArgument(0));

    AccountCreateRequest req =
        new AccountCreateRequest(
            "ACC-001",
            "테스트고객사",
            null,
            "IT",
            null,
            "02-000-0000",
            "서울시",
            100,
            new BigDecimal("5000000000"),
            AccountType.CUSTOMER);

    AccountResponse result = accountService.create(req);

    assertThat(result.code()).isEqualTo("ACC-001");
    assertThat(result.accountType()).isEqualTo(AccountType.CUSTOMER);
    assertThat(result.ownerId()).isEqualTo("auth-user-sub");
  }

  @Test
  void create_duplicateCode_throwsAccountCodeDuplicate() {
    given(accountRepository.existsByCode("ACC-001")).willReturn(true);

    AccountCreateRequest req =
        new AccountCreateRequest(
            "ACC-001", "테스트고객사", null, null, null, null, null, null, null, AccountType.PROSPECT);

    ErpException ex = assertThrows(ErpException.class, () -> accountService.create(req));
    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CRM_ACCOUNT_CODE_DUPLICATE);
  }

  @Test
  void findById_notFound_throwsAccountCompanyNotFound() {
    given(accountRepository.findById(99L)).willReturn(Optional.empty());

    ErpException ex = assertThrows(ErpException.class, () -> accountService.findById(99L));
    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_COMPANY_NOT_FOUND);
  }

  @Test
  void update_matchingVersion_appliesChanges() {
    Account account = buildAccount();
    ReflectionTestUtils.setField(account, "version", 0L);
    given(accountRepository.findById(1L)).willReturn(Optional.of(account));

    AccountUpdateRequest req =
        new AccountUpdateRequest(
            "변경된고객사",
            null,
            "금융",
            null,
            "02-999-9999",
            "부산시",
            200,
            new BigDecimal("9000000000"),
            AccountType.PARTNER,
            "user-sub-002",
            0L);

    AccountResponse result = accountService.update(1L, req);

    assertThat(result.name()).isEqualTo("변경된고객사");
    assertThat(result.accountType()).isEqualTo(AccountType.PARTNER);
  }

  @Test
  void update_staleVersion_throwsOptimisticLock() {
    Account account = buildAccount();
    ReflectionTestUtils.setField(account, "version", 3L);
    given(accountRepository.findById(1L)).willReturn(Optional.of(account));

    AccountUpdateRequest req =
        new AccountUpdateRequest(
            "변경된고객사",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            AccountType.PARTNER,
            "user-sub-002",
            1L);

    assertThrows(
        ObjectOptimisticLockingFailureException.class, () -> accountService.update(1L, req));
  }

  @Test
  void deactivate_existingAccount_setsInactive() {
    Account account = buildAccount();
    given(accountRepository.findById(1L)).willReturn(Optional.of(account));

    accountService.deactivate(1L);

    assertThat(account.isActive()).isFalse();
  }
}
