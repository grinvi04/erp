package com.erp.finance.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VendorUpdateRequest(
    @NotBlank @Size(max = 200) String name,
    @Size(max = 30) String businessNo,
    @Size(max = 100) String contactName,
    @Email @Size(max = 200) String contactEmail,
    @Size(max = 30) String contactPhone,
    @Min(0) int paymentTerms,
    Long payablesAccountId
) {}
