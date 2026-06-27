package com.erp.crm.application.dto;

import com.erp.crm.domain.model.Account;
import com.erp.crm.domain.model.AccountType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AccountResponse(
        Long id,
        String code,
        String name,
        String businessNo,
        String industry,
        String website,
        String phone,
        String address,
        Integer employeeCount,
        BigDecimal annualRevenue,
        AccountType accountType,
        String ownerId,
        boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Long version
) {
    public static AccountResponse from(Account a) {
        return new AccountResponse(a.getId(), a.getCode(), a.getName(), a.getBusinessNo(),
                a.getIndustry(), a.getWebsite(), a.getPhone(), a.getAddress(),
                a.getEmployeeCount(), a.getAnnualRevenue(), a.getAccountType(),
                a.getOwnerId(), a.isActive(), a.getCreatedAt(), a.getUpdatedAt(), a.getVersion());
    }
}
