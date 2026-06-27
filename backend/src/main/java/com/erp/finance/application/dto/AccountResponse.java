package com.erp.finance.application.dto;

import com.erp.finance.domain.model.Account;
import com.erp.finance.domain.model.AccountType;
import com.erp.finance.domain.model.NormalBalance;

public record AccountResponse(
    Long id,
    String code,
    String name,
    AccountType accountType,
    NormalBalance normalBalance,
    Long parentId,
    String parentCode,
    boolean isSummary,
    boolean isActive,
    Long version) {
  public static AccountResponse from(Account a) {
    return new AccountResponse(
        a.getId(),
        a.getCode(),
        a.getName(),
        a.getAccountType(),
        a.getNormalBalance(),
        a.getParent() != null ? a.getParent().getId() : null,
        a.getParent() != null ? a.getParent().getCode() : null,
        a.isSummary(),
        a.isActive(),
        a.getVersion());
  }
}
