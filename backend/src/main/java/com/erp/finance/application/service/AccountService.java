package com.erp.finance.application.service;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.security.Permission;
import com.erp.common.security.PermissionChecker;
import com.erp.finance.application.dto.AccountCreateRequest;
import com.erp.finance.application.dto.AccountResponse;
import com.erp.finance.application.dto.AccountUpdateRequest;
import com.erp.finance.domain.model.Account;
import com.erp.finance.domain.repository.AccountRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountService {

  private final AccountRepository accountRepository;
  private final PermissionChecker permissionChecker;

  public List<AccountResponse> findAll() {
    permissionChecker.require(Permission.FINANCE_READ);
    return accountRepository.findAllByOrderByCodeAsc().stream().map(AccountResponse::from).toList();
  }

  public List<AccountResponse> findRoots() {
    permissionChecker.require(Permission.FINANCE_READ);
    return accountRepository.findByParentIsNullOrderByCodeAsc().stream()
        .map(AccountResponse::from)
        .toList();
  }

  public List<AccountResponse> findByParent(Long parentId) {
    permissionChecker.require(Permission.FINANCE_READ);
    getOrThrow(parentId);
    return accountRepository.findByParentIdOrderByCodeAsc(parentId).stream()
        .map(AccountResponse::from)
        .toList();
  }

  public AccountResponse findById(Long id) {
    permissionChecker.require(Permission.FINANCE_READ);
    return AccountResponse.from(getOrThrow(id));
  }

  @Transactional
  public AccountResponse create(AccountCreateRequest request) {
    permissionChecker.require(Permission.FINANCE_WRITE);
    if (accountRepository.existsByCode(request.code())) {
      throw new ErpException(ErrorCode.ACCOUNT_CODE_DUPLICATE);
    }
    Account parent = null;
    if (request.parentId() != null) {
      parent = getOrThrow(request.parentId());
    }
    Account account =
        Account.of(
            request.code(),
            request.name(),
            request.accountType(),
            request.normalBalance(),
            parent,
            request.isSummary());
    return AccountResponse.from(accountRepository.save(account));
  }

  @Transactional
  public AccountResponse update(Long id, AccountUpdateRequest request) {
    permissionChecker.require(Permission.FINANCE_WRITE);
    Account account = getOrThrow(id);
    account.checkVersion(request.version());
    account.update(request.name(), request.isSummary());
    return AccountResponse.from(account);
  }

  @Transactional
  public void deactivate(Long id) {
    permissionChecker.require(Permission.FINANCE_WRITE);
    Account account = getOrThrow(id);
    if (accountRepository.existsByParentId(id)) {
      throw new ErpException(ErrorCode.ACCOUNT_HAS_CHILDREN);
    }
    account.deactivate();
  }

  private Account getOrThrow(Long id) {
    return accountRepository
        .findById(id)
        .orElseThrow(() -> new ErpException(ErrorCode.ACCOUNT_NOT_FOUND));
  }
}
