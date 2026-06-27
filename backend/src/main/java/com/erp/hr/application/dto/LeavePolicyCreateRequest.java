package com.erp.hr.application.dto;

import com.erp.hr.domain.model.LeavePolicy;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LeavePolicyCreateRequest(
    @NotBlank @Size(max = 30) String code,
    @NotBlank @Size(max = 100) String name,
    @NotNull LeavePolicy.LeaveType leaveType,
    @Min(0) int annualDays,
    @Min(0) int carryOverDays,
    boolean requiresApproval,
    @Min(0) int minNoticeDays) {}
