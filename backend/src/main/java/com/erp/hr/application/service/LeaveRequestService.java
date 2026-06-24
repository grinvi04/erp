package com.erp.hr.application.service;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.security.CurrentUserProvider;
import com.erp.common.workflow.ApprovalRequest;
import com.erp.common.workflow.ApprovalStatus;
import com.erp.common.workflow.ApprovalStep;
import com.erp.common.workflow.repository.ApprovalRequestRepository;
import com.erp.hr.application.dto.ApprovalActionRequest;
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

        if (request.startDate().getYear() != request.endDate().getYear()) {
            throw new ErpException(ErrorCode.LEAVE_CROSS_YEAR);
        }

        int year = request.startDate().getYear();
        LeaveBalance balance = leaveBalanceRepository
            .findByEmployeeIdAndLeavePolicyIdAndYear(request.employeeId(), request.leavePolicyId(), year)
            .orElseThrow(() -> new ErpException(ErrorCode.LEAVE_BALANCE_NOT_FOUND));

        if (!balance.hasSufficientBalance(request.requestedDays())) {
            throw new ErpException(ErrorCode.LEAVE_BALANCE_INSUFFICIENT);
        }

        List<LeaveRequest> overlapping = leaveRequestRepository.findOverlappingByStatuses(
            request.employeeId(), request.startDate(), request.endDate(),
            List.of(ApprovalStatus.APPROVED, ApprovalStatus.PENDING));
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

    @Transactional
    public LeaveRequestResponse approve(Long leaveRequestId, ApprovalActionRequest request) {
        LeaveRequest leaveRequest = getLeaveRequestOrThrow(leaveRequestId);
        if (!leaveRequest.isPending()) {
            throw new ErpException(ErrorCode.APPROVAL_ALREADY_PROCESSED);
        }

        ApprovalRequest approvalRequest = approvalRequestRepository
            .findById(leaveRequest.getApprovalRequestId())
            .orElseThrow(() -> new ErpException(ErrorCode.APPROVAL_NOT_FOUND));

        if (!approvalRequest.isPending()) {
            throw new ErpException(ErrorCode.APPROVAL_ALREADY_PROCESSED);
        }

        String approverId = currentUserProvider.getCurrentUserId();
        if (approverId == null) {
            approverId = "SYSTEM";
        }
        if (!"SYSTEM".equals(approverId) && !approverId.equals(approvalRequest.getCurrentStepApproverId())) {
            throw new ErpException(ErrorCode.APPROVER_NOT_AUTHORIZED);
        }
        approvalRequest.approve(approverId, request.comment());

        if (approvalRequest.getStatus() == ApprovalStatus.APPROVED) {
            leaveRequest.approve();
            int year = leaveRequest.getStartDate().getYear();
            LeaveBalance balance = leaveBalanceRepository
                .findByEmployeeIdAndLeavePolicyIdAndYear(
                    leaveRequest.getEmployee().getId(),
                    leaveRequest.getLeavePolicy().getId(),
                    year)
                .orElseThrow(() -> new ErpException(ErrorCode.LEAVE_BALANCE_NOT_FOUND));
            balance.deduct(leaveRequest.getRequestedDays());
        }

        return LeaveRequestResponse.from(leaveRequest);
    }

    @Transactional
    public LeaveRequestResponse reject(Long leaveRequestId, ApprovalActionRequest request) {
        LeaveRequest leaveRequest = getLeaveRequestOrThrow(leaveRequestId);
        if (!leaveRequest.isPending()) {
            throw new ErpException(ErrorCode.APPROVAL_ALREADY_PROCESSED);
        }

        ApprovalRequest approvalRequest = approvalRequestRepository
            .findById(leaveRequest.getApprovalRequestId())
            .orElseThrow(() -> new ErpException(ErrorCode.APPROVAL_NOT_FOUND));

        if (!approvalRequest.isPending()) {
            throw new ErpException(ErrorCode.APPROVAL_ALREADY_PROCESSED);
        }

        String approverId = currentUserProvider.getCurrentUserId();
        if (approverId == null) {
            approverId = "SYSTEM";
        }
        if (!"SYSTEM".equals(approverId) && !approverId.equals(approvalRequest.getCurrentStepApproverId())) {
            throw new ErpException(ErrorCode.APPROVER_NOT_AUTHORIZED);
        }
        approvalRequest.reject(approverId, request.comment());
        leaveRequest.reject();

        return LeaveRequestResponse.from(leaveRequest);
    }

    private LeaveRequest getLeaveRequestOrThrow(Long id) {
        return leaveRequestRepository.findById(id)
            .orElseThrow(() -> new ErpException(ErrorCode.LEAVE_REQUEST_NOT_FOUND));
    }
}
