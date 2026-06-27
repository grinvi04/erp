package com.erp.common.security.dto;

import com.erp.common.security.DataScope;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record AccessProfileRequest(
    @NotNull DataScope dataScope, Long departmentId, @PositiveOrZero BigDecimal approvalLimit) {}
