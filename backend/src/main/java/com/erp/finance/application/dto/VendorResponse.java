package com.erp.finance.application.dto;

import com.erp.finance.domain.model.Vendor;

public record VendorResponse(
    Long id,
    String code,
    String name,
    String businessNo,
    String contactName,
    String contactEmail,
    String contactPhone,
    int paymentTerms,
    boolean isActive,
    Long payablesAccountId
) {
    public static VendorResponse from(Vendor v) {
        return new VendorResponse(v.getId(), v.getCode(), v.getName(), v.getBusinessNo(),
            v.getContactName(), v.getContactEmail(), v.getContactPhone(),
            v.getPaymentTerms(), v.isActive(),
            v.getPayablesAccount() != null ? v.getPayablesAccount().getId() : null);
    }
}
