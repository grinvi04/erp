package com.erp.hr.application.dto;

import com.erp.hr.domain.model.LeaveBalance;

import java.math.BigDecimal;

public record LeaveBalanceResponse(
    Long id,
    Long employeeId,
    Long leavePolicyId,
    String leavePolicyName,
    int year,
    BigDecimal entitledDays,
    BigDecimal usedDays,
    BigDecimal carryOverDays,
    BigDecimal remainingDays
) {
    public static LeaveBalanceResponse from(LeaveBalance balance) {
        return new LeaveBalanceResponse(
            balance.getId(),
            balance.getEmployee().getId(),
            balance.getLeavePolicy().getId(),
            balance.getLeavePolicy().getName(),
            balance.getYear(),
            balance.getEntitledDays(),
            balance.getUsedDays(),
            balance.getCarryOverDays(),
            balance.getRemainingDays()
        );
    }
}
