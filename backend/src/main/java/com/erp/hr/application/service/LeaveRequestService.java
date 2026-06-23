package com.erp.hr.application.service;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.security.CurrentUserProvider;
import com.erp.common.workflow.ApprovalRequest;
import com.erp.common.workflow.ApprovalStatus;
import com.erp.common.workflow.ApprovalStep;
import com.erp.common.workflow.repository.ApprovalRequestRepository;
import com.erp.hr.application.dto.LeaveRequestCreateRequest;
import com.erp.hr.application.dto.LeaveRequestResponse;
import com.erp.hr.domain.model.Employee;
import com.erp.hr.domain.model.LeaveBalance;
import com.erp.hr.domain.model.LeavePolicy;
import com.erp.hr.domain.model.LeaveRequest;
import com.erp.hr.domain.repository.EmployeeRepository;
import com.erp.hr.domain.repository.LeaveBalanceRepository;
import com.erp.hr.domain.repository.LeavePolicyRepository;
import com.erp.hr.domain.repository.LeaveRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LeaveRequestService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final EmployeeRepository employeeRepository;
    private final LeavePolicyRepository leavePolicyRepository;
    private final ApprovalRequestRepository approvalRequestRepository;
    private final CurrentUserProvider currentUserProvider;

    public List<LeaveRequestResponse> findByEmployee(Long employeeId) {
        return leaveRequestRepository.findByEmployeeId(employeeId).stream()
            .map(LeaveRequestResponse::from)
            .toList();
    }

    @Transactional
    public LeaveRequestResponse create(LeaveRequestCreateRequest request) {
        Employee employee = employeeRepository.findById(request.employeeId())
            .orElseThrow(() -> new ErpException(ErrorCode.EMPLOYEE_NOT_FOUND));
        LeavePolicy policy = leavePolicyRepository.findById(request.leavePolicyId())
            .orElseThrow(() -> new ErpException(ErrorCode.RESOURCE_NOT_FOUND));

        int year = request.startDate().getYear();
        LeaveBalance balance = leaveBalanceRepository
            .findByEmployeeIdAndLeavePolicyIdAndYear(request.employeeId(), request.leavePolicyId(), year)
            .orElseThrow(() -> new ErpException(ErrorCode.LEAVE_BALANCE_INSUFFICIENT));

        if (!balance.hasSufficientBalance(request.requestedDays())) {
            throw new ErpException(ErrorCode.LEAVE_BALANCE_INSUFFICIENT);
        }

        List<LeaveRequest> overlapping = leaveRequestRepository.findApprovedOverlapping(
            request.employeeId(), request.startDate(), request.endDate(), ApprovalStatus.APPROVED);
        if (!overlapping.isEmpty()) {
            throw new ErpException(ErrorCode.LEAVE_OVERLAP);
        }

        LeaveRequest leaveRequest = LeaveRequest.create(
            employee, policy, request.startDate(), request.endDate(),
            request.requestedDays(), request.reason());
        leaveRequest = leaveRequestRepository.save(leaveRequest);

        String requesterId = currentUserProvider.getCurrentUserId();
        if (requesterId == null) {
            requesterId = employee.getEmployeeNo();
        }
        String approverId = employee.getManager() != null
            ? employee.getManager().getEmployeeNo()
            : "SYSTEM";
        ApprovalStep step = ApprovalStep.of(1, "직속 상관 승인", approverId);
        ApprovalRequest approvalRequest = ApprovalRequest.create(
            "LEAVE_REQUEST", leaveRequest.getId(),
            policy.getName() + " 신청",
            requesterId,
            List.of(step));
        approvalRequest = approvalRequestRepository.save(approvalRequest);

        leaveRequest.linkApprovalRequest(approvalRequest.getId());

        return LeaveRequestResponse.from(leaveRequest);
    }
}
