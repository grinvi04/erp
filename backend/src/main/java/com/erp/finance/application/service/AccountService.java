package com.erp.finance.application.service;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.finance.application.dto.AccountCreateRequest;
import com.erp.finance.application.dto.AccountResponse;
import com.erp.finance.application.dto.AccountUpdateRequest;
import com.erp.finance.domain.model.Account;
import com.erp.finance.domain.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountService {

    private final AccountRepository accountRepository;

    public List<AccountResponse> findAll() {
        return accountRepository.findAllByOrderByCodeAsc().stream()
            .map(AccountResponse::from)
            .toList();
    }

    public List<AccountResponse> findRoots() {
        return accountRepository.findByParentIsNullOrderByCodeAsc().stream()
            .map(AccountResponse::from)
            .toList();
    }

    public List<AccountResponse> findByParent(Long parentId) {
        getOrThrow(parentId);
        return accountRepository.findByParentIdOrderByCodeAsc(parentId).stream()
            .map(AccountResponse::from)
            .toList();
    }

    public AccountResponse findById(Long id) {
        return AccountResponse.from(getOrThrow(id));
    }

    @Transactional
    public AccountResponse create(AccountCreateRequest request) {
        if (accountRepository.existsByCode(request.code())) {
            throw new ErpException(ErrorCode.ACCOUNT_CODE_DUPLICATE);
        }
        Account parent = null;
        if (request.parentId() != null) {
            parent = getOrThrow(request.parentId());
        }
        Account account = Account.of(request.code(), request.name(), request.accountType(),
            request.normalBalance(), parent, request.isSummary());
        return AccountResponse.from(accountRepository.save(account));
    }

    @Transactional
    public AccountResponse update(Long id, AccountUpdateRequest request) {
        Account account = getOrThrow(id);
        account.update(request.name(), request.isSummary());
        return AccountResponse.from(account);
    }

    @Transactional
    public void deactivate(Long id) {
        Account account = getOrThrow(id);
        if (accountRepository.existsByParentId(id)) {
            throw new ErpException(ErrorCode.ACCOUNT_HAS_CHILDREN);
        }
        account.deactivate();
    }

    private Account getOrThrow(Long id) {
        return accountRepository.findById(id)
            .orElseThrow(() -> new ErpException(ErrorCode.ACCOUNT_NOT_FOUND));
    }
}
