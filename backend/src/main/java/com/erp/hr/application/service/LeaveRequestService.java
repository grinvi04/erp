package com.erp.hr.application.service;

import com.erp.common.audit.AuditLog;
import com.erp.common.audit.AuditService;
import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.security.CurrentUserProvider;
import com.erp.common.security.Permission;
import com.erp.common.security.PermissionChecker;
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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
    private final PermissionChecker permissionChecker;
    private final HrDataScopeResolver dataScopeResolver;
    private final AuditService auditService;

    public Page<LeaveRequestResponse> findAll(Pageable pageable) {
        permissionChecker.require(Permission.HR_LEAVE_READ);
        // 데이터 스코프 공통 필터 — 범위 밖 직원의 휴가는 목록에서 제외
        return leaveRequestRepository.findAll(dataScopeResolver.leaveRequestScope(), pageable)
            .map(LeaveRequestResponse::from);
    }

    public Page<LeaveRequestResponse> findByEmployee(Long employeeId, Pageable pageable) {
        permissionChecker.require(Permission.HR_LEAVE_READ);
        Employee employee = employeeRepository.findById(employeeId)
            .orElseThrow(() -> new ErpException(ErrorCode.EMPLOYEE_NOT_FOUND));
        if (!dataScopeResolver.isEmployeeInScope(employee)) {
            throw new ErpException(ErrorCode.FORBIDDEN);
        }
        return leaveRequestRepository.findByEmployeeId(employeeId, pageable)
            .map(LeaveRequestResponse::from);
    }

    @Transactional
    public LeaveRequestResponse create(LeaveRequestCreateRequest request) {
        permissionChecker.require(Permission.HR_LEAVE_WRITE);
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
        // 결재자 식별은 Keycloak sub(정본 신원)로 한다 — approve()의 검증도 sub 기준이므로
        // 매니저의 employeeNo가 아니라 연결된 user_id를 결재자로 지정해야 실제 로그인
        // 사용자가 결재할 수 있다. (미연결 매니저는 결재 불가 상태로 남으므로 SYSTEM fallback)
        Employee manager = employee.getManager();
        String approverId = manager != null && manager.getUserId() != null
            ? manager.getUserId()
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
        requireAuthorizedApprover(approverId, approvalRequest);
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

        auditService.record("LEAVE_REQUEST", leaveRequest.getId(),
            AuditLog.AuditAction.APPROVE, null, null);
        return LeaveRequestResponse.from(leaveRequest);
    }

    @Transactional
    public LeaveRequestResponse reject(Long leaveRequestId, ApprovalActionRequest request) {
        // 반려는 사유가 필수 — 클라이언트뿐 아니라 서버에서도 강제한다.
        if (request.comment() == null || request.comment().isBlank()) {
            throw new ErpException(ErrorCode.INVALID_INPUT);
        }
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
        requireAuthorizedApprover(approverId, approvalRequest);
        approvalRequest.reject(approverId, request.comment());
        leaveRequest.reject();

        auditService.record("LEAVE_REQUEST", leaveRequest.getId(),
            AuditLog.AuditAction.REJECT, null, null);
        return LeaveRequestResponse.from(leaveRequest);
    }

    /**
     * 결재 권한 검증. (1) 인증된 사용자여야 하고 — null 행위자를 SYSTEM으로 승격해 우회를
     * 허용하지 않는다, (2) 현재 단계의 결재자(매니저 sub)와 일치해야 하며, (3) 본인이
     * 상신한 건은 결재할 수 없다(직무 분리). AP 전표 결재와 동일한 기준.
     */
    private void requireAuthorizedApprover(String approverId, ApprovalRequest approvalRequest) {
        if (approverId == null
                || !approverId.equals(approvalRequest.getCurrentStepApproverId())
                || approverId.equals(approvalRequest.getRequesterId())) {
            throw new ErpException(ErrorCode.APPROVER_NOT_AUTHORIZED);
        }
    }

    private LeaveRequest getLeaveRequestOrThrow(Long id) {
        return leaveRequestRepository.findById(id)
            .orElseThrow(() -> new ErpException(ErrorCode.LEAVE_REQUEST_NOT_FOUND));
    }
}
