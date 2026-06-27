package com.erp.finance.application.dto;

import com.erp.finance.domain.model.Customer;

public record CustomerResponse(
    Long id,
    String code,
    String name,
    String businessNo,
    String contactName,
    String contactEmail,
    String contactPhone,
    int paymentTerms,
    boolean isActive,
    Long receivablesAccountId,
    Long version) {
  public static CustomerResponse from(Customer c) {
    return new CustomerResponse(
        c.getId(),
        c.getCode(),
        c.getName(),
        c.getBusinessNo(),
        c.getContactName(),
        c.getContactEmail(),
        c.getContactPhone(),
        c.getPaymentTerms(),
        c.isActive(),
        c.getReceivablesAccount() != null ? c.getReceivablesAccount().getId() : null,
        c.getVersion());
  }
}
