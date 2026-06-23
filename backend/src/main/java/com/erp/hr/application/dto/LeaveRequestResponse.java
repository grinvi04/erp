package com.erp.hr.application.dto;

import com.erp.common.workflow.ApprovalStatus;
import com.erp.hr.domain.model.LeaveRequest;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LeaveRequestResponse(
    Long id,
    Long employeeId,
    String employeeName,
    Long leavePolicyId,
    String leavePolicyName,
    LocalDate startDate,
    LocalDate endDate,
    BigDecimal requestedDays,
    ApprovalStatus approvalStatus,
    String reason,
    Long approvalRequestId
) {
    public static LeaveRequestResponse from(LeaveRequest req) {
        return new LeaveRequestResponse(
            req.getId(),
            req.getEmployee().getId(),
            req.getEmployee().getPersonalInfo().getFullName(),
            req.getLeavePolicy().getId(),
            req.getLeavePolicy().getName(),
            req.getStartDate(),
            req.getEndDate(),
            req.getRequestedDays(),
            req.getApprovalStatus(),
            req.getReason(),
            req.getApprovalRequestId()
        );
    }
}
