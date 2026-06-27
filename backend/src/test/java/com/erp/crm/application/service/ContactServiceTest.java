package com.erp.crm.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.crm.application.dto.ContactCreateRequest;
import com.erp.crm.application.dto.ContactResponse;
import com.erp.crm.application.dto.ContactUpdateRequest;
import com.erp.crm.domain.model.Account;
import com.erp.crm.domain.model.AccountType;
import com.erp.crm.domain.model.Contact;
import com.erp.crm.domain.repository.ContactRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ContactServiceTest {

  @Mock private ContactRepository contactRepository;
  @Mock private CrmAccountService accountService;
  @Mock private com.erp.common.security.PermissionChecker permissionChecker;
  @InjectMocks private ContactService contactService;

  private Account buildAccount() {
    Account account =
        Account.of(
            "ACC-001",
            "테스트고객사",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            AccountType.CUSTOMER,
            "user-001");
    ReflectionTestUtils.setField(account, "id", 1L);
    return account;
  }

  private Contact buildContact(Account account, boolean isPrimary, long version) {
    Contact contact =
        Contact.of(
            account,
            "홍",
            "길동",
            "팀장",
            "구매팀",
            "hong@example.com",
            "02-000-0000",
            "010-0000-0000",
            isPrimary);
    ReflectionTestUtils.setField(contact, "version", version);
    return contact;
  }

  @Test
  void create_nonPrimary_savesAndReturns() {
    Account account = buildAccount();
    Contact contact = buildContact(account, false, 0L);
    given(accountService.getOrThrow(1L)).willReturn(account);
    given(contactRepository.save(any())).willReturn(contact);

    ContactCreateRequest req =
        new ContactCreateRequest(
            1L, "홍", "길동", "팀장", "구매팀", "hong@example.com", "02-000-0000", "010-0000-0000", false);

    ContactResponse result = contactService.create(req);

    assertThat(result.lastName()).isEqualTo("홍");
    assertThat(result.isPrimary()).isFalse();
  }

  @Test
  void create_primaryWhenNoneExists_savesAndReturns() {
    Account account = buildAccount();
    Contact contact = buildContact(account, true, 0L);
    given(contactRepository.existsByAccount_IdAndIsPrimaryTrue(1L)).willReturn(false);
    given(accountService.getOrThrow(1L)).willReturn(account);
    given(contactRepository.save(any())).willReturn(contact);

    ContactCreateRequest req =
        new ContactCreateRequest(
            1L, "홍", "길동", "팀장", "구매팀", "hong@example.com", "02-000-0000", "010-0000-0000", true);

    ContactResponse result = contactService.create(req);

    assertThat(result.isPrimary()).isTrue();
  }

  @Test
  void create_primaryWhenAlreadyExists_throwsContactPrimaryDuplicate() {
    given(contactRepository.existsByAccount_IdAndIsPrimaryTrue(1L)).willReturn(true);

    ContactCreateRequest req =
        new ContactCreateRequest(1L, "홍", "길동", null, null, null, null, null, true);

    ErpException ex = assertThrows(ErpException.class, () -> contactService.create(req));
    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CONTACT_PRIMARY_DUPLICATE);
  }

  @Test
  void findById_notFound_throwsContactNotFound() {
    given(contactRepository.findById(99L)).willReturn(Optional.empty());

    ErpException ex = assertThrows(ErpException.class, () -> contactService.findById(99L));
    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CONTACT_NOT_FOUND);
  }

  @Test
  void update_matchingVersion_updatesFields() {
    Account account = buildAccount();
    Contact contact = buildContact(account, false, 0L);
    given(contactRepository.findById(1L)).willReturn(Optional.of(contact));

    ContactUpdateRequest req =
        new ContactUpdateRequest(
            "김", "철수", "부장", "영업팀", "kim@example.com", "02-111-1111", "010-1111-1111", false, 0L);

    ContactResponse result = contactService.update(1L, req);

    assertThat(result.lastName()).isEqualTo("김");
    assertThat(result.firstName()).isEqualTo("철수");
  }

  @Test
  void update_staleVersion_throwsOptimisticLock() {
    Account account = buildAccount();
    Contact contact = buildContact(account, false, 3L);
    given(contactRepository.findById(1L)).willReturn(Optional.of(contact));

    ContactUpdateRequest req =
        new ContactUpdateRequest("김", "철수", null, null, null, null, null, false, 1L);

    assertThrows(
        ObjectOptimisticLockingFailureException.class, () -> contactService.update(1L, req));
  }

  @Test
  void update_setPrimaryWhenAnotherPrimaryExists_throwsContactPrimaryDuplicate() {
    Account account = buildAccount();
    Contact contact = buildContact(account, false, 0L);
    given(contactRepository.findById(1L)).willReturn(Optional.of(contact));
    given(contactRepository.existsByAccount_IdAndIsPrimaryTrueAndIdNot(anyLong(), any()))
        .willReturn(true);

    ContactUpdateRequest req =
        new ContactUpdateRequest("홍", "길동", null, null, null, null, null, true, 0L);

    ErpException ex = assertThrows(ErpException.class, () -> contactService.update(1L, req));
    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CONTACT_PRIMARY_DUPLICATE);
  }
}
