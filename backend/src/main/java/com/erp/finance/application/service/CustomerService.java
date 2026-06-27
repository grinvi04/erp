package com.erp.finance.application.service;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.response.PageResponse;
import com.erp.common.security.Permission;
import com.erp.common.security.PermissionChecker;
import com.erp.finance.application.dto.CustomerCreateRequest;
import com.erp.finance.application.dto.CustomerResponse;
import com.erp.finance.application.dto.CustomerUpdateRequest;
import com.erp.finance.domain.model.Account;
import com.erp.finance.domain.model.Customer;
import com.erp.finance.domain.repository.AccountRepository;
import com.erp.finance.domain.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerService {

  private final CustomerRepository customerRepository;
  private final AccountRepository accountRepository;
  private final PermissionChecker permissionChecker;

  public PageResponse<CustomerResponse> findAll(String keyword, Pageable pageable) {
    permissionChecker.require(Permission.FINANCE_READ);
    return PageResponse.from(
        customerRepository.search(normalizeKeyword(keyword), pageable).map(CustomerResponse::from));
  }

  private static String normalizeKeyword(String keyword) {
    return (keyword == null || keyword.isBlank()) ? null : keyword.trim().toLowerCase();
  }

  public CustomerResponse findById(Long id) {
    permissionChecker.require(Permission.FINANCE_READ);
    return CustomerResponse.from(getOrThrow(id));
  }

  @Transactional
  public CustomerResponse create(CustomerCreateRequest request) {
    permissionChecker.require(Permission.FINANCE_WRITE);
    if (customerRepository.existsByCode(request.code())) {
      throw new ErpException(ErrorCode.CUSTOMER_CODE_DUPLICATE);
    }
    Customer customer =
        Customer.of(
            request.code(),
            request.name(),
            request.businessNo(),
            request.contactName(),
            request.contactEmail(),
            request.contactPhone(),
            request.paymentTerms());
    applyReceivablesAccount(customer, request.receivablesAccountId());
    return CustomerResponse.from(customerRepository.save(customer));
  }

  @Transactional
  public CustomerResponse update(Long id, CustomerUpdateRequest request) {
    permissionChecker.require(Permission.FINANCE_WRITE);
    Customer customer = getOrThrow(id);
    customer.checkVersion(request.version());
    customer.update(
        request.name(),
        request.businessNo(),
        request.contactName(),
        request.contactEmail(),
        request.contactPhone(),
        request.paymentTerms());
    applyReceivablesAccount(customer, request.receivablesAccountId());
    return CustomerResponse.from(customer);
  }

  private void applyReceivablesAccount(Customer customer, Long accountId) {
    if (accountId == null) {
      customer.assignReceivablesAccount(null);
      return;
    }
    Account account =
        accountRepository
            .findById(accountId)
            .orElseThrow(() -> new ErpException(ErrorCode.ACCOUNT_NOT_FOUND));
    customer.assignReceivablesAccount(account);
  }

  @Transactional
  public void deactivate(Long id) {
    permissionChecker.require(Permission.FINANCE_WRITE);
    getOrThrow(id).deactivate();
  }

  private Customer getOrThrow(Long id) {
    return customerRepository
        .findById(id)
        .orElseThrow(() -> new ErpException(ErrorCode.CUSTOMER_NOT_FOUND));
  }
}
