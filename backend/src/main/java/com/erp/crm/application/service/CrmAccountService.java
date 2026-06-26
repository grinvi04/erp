package com.erp.crm.application.service;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.response.PageResponse;
import com.erp.common.security.Permission;
import com.erp.common.security.PermissionChecker;
import com.erp.crm.application.dto.AccountCreateRequest;
import com.erp.crm.application.dto.AccountResponse;
import com.erp.crm.application.dto.AccountUpdateRequest;
import com.erp.crm.domain.model.Account;
import com.erp.crm.domain.repository.CrmAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CrmAccountService {

    private final CrmAccountRepository accountRepository;
    private final PermissionChecker permissionChecker;
    private final CrmDataScopeResolver dataScopeResolver;

    public PageResponse<AccountResponse> search(String keyword, Boolean isActive, Pageable pageable) {
        permissionChecker.require(Permission.CRM_READ);
        var s = dataScopeResolver.ownerScope();
        return PageResponse.from(
                accountRepository.search(keyword, isActive, s.scoped(), s.ownerIds(), pageable)
                .map(AccountResponse::from));
    }

    public AccountResponse findById(Long id) {
        permissionChecker.require(Permission.CRM_READ);
        var account = getOrThrow(id);
        dataScopeResolver.requireOwnerAccess(account.getOwnerId());
        return AccountResponse.from(account);
    }

    @Transactional
    public AccountResponse create(AccountCreateRequest req) {
        permissionChecker.require(Permission.CRM_WRITE);
        if (accountRepository.existsByCode(req.code())) {
            throw new ErpException(ErrorCode.CRM_ACCOUNT_CODE_DUPLICATE);
        }
        Account account = Account.of(req.code(), req.name(), req.businessNo(), req.industry(),
                req.website(), req.phone(), req.address(), req.employeeCount(),
                req.annualRevenue(), req.accountType(), req.ownerId());
        return AccountResponse.from(accountRepository.save(account));
    }

    @Transactional
    public AccountResponse update(Long id, AccountUpdateRequest req) {
        permissionChecker.require(Permission.CRM_WRITE);
        Account account = getOrThrow(id);
        account.checkVersion(req.version());
        account.update(req.name(), req.businessNo(), req.industry(), req.website(),
                req.phone(), req.address(), req.employeeCount(), req.annualRevenue(),
                req.accountType(), req.ownerId());
        return AccountResponse.from(account);
    }

    @Transactional
    public void deactivate(Long id) {
        permissionChecker.require(Permission.CRM_WRITE);
        getOrThrow(id).deactivate();
    }

    public Account getOrThrow(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new ErpException(ErrorCode.ACCOUNT_COMPANY_NOT_FOUND));
    }
}
