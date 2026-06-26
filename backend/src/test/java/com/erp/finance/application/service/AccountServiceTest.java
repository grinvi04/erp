package com.erp.finance.application.service;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.finance.application.dto.AccountCreateRequest;
import com.erp.finance.application.dto.AccountResponse;
import com.erp.finance.application.dto.AccountUpdateRequest;
import com.erp.finance.domain.model.Account;
import com.erp.finance.domain.model.AccountType;
import com.erp.finance.domain.model.NormalBalance;
import com.erp.finance.domain.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private com.erp.common.security.PermissionChecker permissionChecker;

    @InjectMocks
    private AccountService accountService;

    @Test
    void create_newCode_returnsAccountResponse() {
        given(accountRepository.existsByCode("1100")).willReturn(false);
        Account account = Account.of("1100", "현금", AccountType.ASSET, NormalBalance.DEBIT, null, false);
        given(accountRepository.save(any())).willReturn(account);

        AccountResponse result = accountService.create(
            new AccountCreateRequest("1100", "현금", AccountType.ASSET, NormalBalance.DEBIT, null, false));

        assertThat(result.code()).isEqualTo("1100");
        assertThat(result.accountType()).isEqualTo(AccountType.ASSET);
    }

    @Test
    void create_duplicateCode_throwsAccountCodeDuplicate() {
        given(accountRepository.existsByCode("1100")).willReturn(true);

        ErpException ex = assertThrows(ErpException.class, () ->
            accountService.create(
                new AccountCreateRequest("1100", "현금", AccountType.ASSET, NormalBalance.DEBIT, null, false)));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_CODE_DUPLICATE);
    }

    @Test
    void update_found_returnsUpdatedResponse() {
        Account account = Account.of("1100", "현금", AccountType.ASSET, NormalBalance.DEBIT, null, false);
        ReflectionTestUtils.setField(account, "version", 3L);
        given(accountRepository.findById(1L)).willReturn(Optional.of(account));

        AccountResponse result = accountService.update(1L, new AccountUpdateRequest("당좌현금", false, 3L));

        assertThat(result.name()).isEqualTo("당좌현금");
        assertThat(result.version()).isEqualTo(3L);
    }

    @Test
    void update_versionMismatch_throwsOptimisticLockConflict() {
        Account account = Account.of("1100", "현금", AccountType.ASSET, NormalBalance.DEBIT, null, false);
        ReflectionTestUtils.setField(account, "version", 5L);
        given(accountRepository.findById(1L)).willReturn(Optional.of(account));

        assertThrows(ObjectOptimisticLockingFailureException.class,
            () -> accountService.update(1L, new AccountUpdateRequest("당좌현금", false, 3L)));
    }

    @Test
    void findById_notFound_throwsAccountNotFound() {
        given(accountRepository.findById(99L)).willReturn(Optional.empty());

        ErpException ex = assertThrows(ErpException.class, () -> accountService.findById(99L));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_NOT_FOUND);
    }

    @Test
    void deactivate_withChildren_throwsAccountHasChildren() {
        Account account = Account.of("1000", "자산", AccountType.ASSET, NormalBalance.DEBIT, null, true);
        given(accountRepository.findById(1L)).willReturn(Optional.of(account));
        given(accountRepository.existsByParentId(1L)).willReturn(true);

        ErpException ex = assertThrows(ErpException.class, () -> accountService.deactivate(1L));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_HAS_CHILDREN);
    }

    @Test
    void deactivate_noChildren_deactivatesAccount() {
        Account account = Account.of("1100", "현금", AccountType.ASSET, NormalBalance.DEBIT, null, false);
        given(accountRepository.findById(1L)).willReturn(Optional.of(account));
        given(accountRepository.existsByParentId(1L)).willReturn(false);

        accountService.deactivate(1L);

        assertThat(account.isActive()).isFalse();
    }
}
