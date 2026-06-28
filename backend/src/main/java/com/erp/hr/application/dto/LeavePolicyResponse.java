package com.erp.hr.application.dto;

import com.erp.hr.domain.model.LeavePolicy;

public record LeavePolicyResponse(
    Long id,
    String code,
    String name,
    LeavePolicy.LeaveType leaveType,
    int annualDays,
    int carryOverDays,
    boolean requiresApproval,
    int minNoticeDays) {
  public static LeavePolicyResponse from(LeavePolicy policy) {
    return new LeavePolicyResponse(
        policy.getId(),
        policy.getCode(),
        policy.getName(),
        policy.getLeaveType(),
        policy.getAnnualDays(),
        policy.getCarryOverDays(),
        policy.isRequiresApproval(),
        policy.getMinNoticeDays());
  }
}
