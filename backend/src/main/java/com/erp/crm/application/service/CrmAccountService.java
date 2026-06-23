package com.erp.crm.application.service;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.response.PageResponse;
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

    public PageResponse<AccountResponse> search(String keyword, Boolean isActive, Pageable pageable) {
        return PageResponse.from(accountRepository.search(keyword, isActive, pageable)
                .map(AccountResponse::from));
    }

    public AccountResponse findById(Long id) {
        return AccountResponse.from(getOrThrow(id));
    }

    @Transactional
    public AccountResponse create(AccountCreateRequest req) {
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
        Account account = getOrThrow(id);
        account.update(req.name(), req.businessNo(), req.industry(), req.website(),
                req.phone(), req.address(), req.employeeCount(), req.annualRevenue(),
                req.accountType(), req.ownerId());
        return AccountResponse.from(account);
    }

    @Transactional
    public void deactivate(Long id) {
        getOrThrow(id).deactivate();
    }

    public Account getOrThrow(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new ErpException(ErrorCode.ACCOUNT_COMPANY_NOT_FOUND));
    }
}
