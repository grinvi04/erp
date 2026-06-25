package com.erp.hr;

import com.erp.common.AbstractIntegrationTest;
import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.workflow.ApprovalRequest;
import com.erp.common.workflow.ApprovalStatus;
import com.erp.common.workflow.repository.ApprovalRequestRepository;
import com.erp.hr.application.dto.ApprovalActionRequest;
import com.erp.hr.application.dto.LeaveRequestResponse;
import com.erp.hr.application.service.LeaveRequestService;
import com.erp.hr.domain.model.Department;
import com.erp.hr.domain.model.Employee;
import com.erp.hr.domain.model.EmploymentType;
import com.erp.hr.domain.model.LeaveBalance;
import com.erp.hr.domain.model.LeavePolicy;
import com.erp.hr.domain.model.LeaveRequest;
import com.erp.hr.domain.model.PersonalInfo;
import com.erp.hr.domain.model.Position;
import com.erp.hr.domain.repository.DepartmentRepository;
import com.erp.hr.domain.repository.EmployeeRepository;
import com.erp.hr.domain.repository.LeaveBalanceRepository;
import com.erp.hr.domain.repository.LeavePolicyRepository;
import com.erp.hr.domain.repository.LeaveRequestRepository;
import com.erp.hr.domain.repository.PositionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Transactional
class LeaveApprovalIntegrationTest extends AbstractIntegrationTest {

    @Autowired private LeaveRequestService leaveRequestService;
    @Autowired private LeaveRequestRepository leaveRequestRepository;
    @Autowired private LeaveBalanceRepository leaveBalanceRepository;
    @Autowired private ApprovalRequestRepository approvalRequestRepository;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private PositionRepository positionRepository;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private LeavePolicyRepository leavePolicyRepository;

    private LeaveRequest savedLeaveRequest;
    private LeaveBalance savedBalance;

    @BeforeEach
    void setUp() {
        Department dept = departmentRepository.save(Department.createRoot("DEV", "개발팀"));
        Position pos = positionRepository.save(Position.of("P001", "Engineer", 2));

        PersonalInfo info = new PersonalInfo("김", "개발", LocalDate.of(1990, 1, 1),
            PersonalInfo.Gender.MALE, null, null, null);
        Employee emp = employeeRepository.save(
            Employee.create("EMP-IT001", info, dept, pos, null,
                LocalDate.of(2020, 3, 1), EmploymentType.REGULAR, "it001@test.com", BigDecimal.valueOf(50000000)));

        LeavePolicy policy = leavePolicyRepository.save(
            LeavePolicy.of("ANNUAL-IT", "연차", LeavePolicy.LeaveType.ANNUAL, 15, 5, true, 1));

        savedBalance = leaveBalanceRepository.save(
            LeaveBalance.create(emp, policy, 2024, BigDecimal.valueOf(15), BigDecimal.ZERO));

        LeaveRequest lr = LeaveRequest.create(emp, policy,
            LocalDate.of(2024, 4, 1), LocalDate.of(2024, 4, 3),
            BigDecimal.valueOf(3), "개인 사정");
        lr = leaveRequestRepository.save(lr);

        ApprovalRequest ar = ApprovalRequest.create(
            "LEAVE_REQUEST", lr.getId(), "연차 신청",
            "EMP-IT001",
            List.of(com.erp.common.workflow.ApprovalStep.of(1, "직속 상관 승인", "MANAGER")));
        ar = approvalRequestRepository.save(ar);
        lr.linkApprovalRequest(ar.getId());

        savedLeaveRequest = lr;

        // 결재 검증은 인증된 현재 사용자(sub)가 단계 결재자와 일치해야 통과한다 —
        // 결재자 "MANAGER"로 인증 컨텍스트를 설정한다.
        authenticateAs("MANAGER");
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(String subject) {
        Jwt jwt = Jwt.withTokenValue("test-token").header("alg", "none")
            .subject(subject).claim("sub", subject).build();
        // 2-인자 생성자는 authenticated=true로 설정한다(단일 인자 생성자는 false).
        SecurityContextHolder.getContext()
            .setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
    }

    @Test
    void approve_finalStep_approvesLeaveAndDeductsBalance() {
        LeaveRequestResponse result = leaveRequestService.approve(
            savedLeaveRequest.getId(), new ApprovalActionRequest("승인합니다"));

        assertThat(result.approvalStatus()).isEqualTo(ApprovalStatus.APPROVED);

        ApprovalRequest ar = approvalRequestRepository
            .findById(savedLeaveRequest.getApprovalRequestId()).orElseThrow();
        assertThat(ar.getStatus()).isEqualTo(ApprovalStatus.APPROVED);

        LeaveBalance balance = leaveBalanceRepository.findById(savedBalance.getId()).orElseThrow();
        assertThat(balance.getUsedDays()).isEqualByComparingTo(BigDecimal.valueOf(3));
        assertThat(balance.getRemainingDays()).isEqualByComparingTo(BigDecimal.valueOf(12));
    }

    @Test
    void reject_pendingRequest_rejectsLeaveRequest() {
        LeaveRequestResponse result = leaveRequestService.reject(
            savedLeaveRequest.getId(), new ApprovalActionRequest("일정 조정 필요"));

        assertThat(result.approvalStatus()).isEqualTo(ApprovalStatus.REJECTED);

        ApprovalRequest ar = approvalRequestRepository
            .findById(savedLeaveRequest.getApprovalRequestId()).orElseThrow();
        assertThat(ar.getStatus()).isEqualTo(ApprovalStatus.REJECTED);

        LeaveBalance balance = leaveBalanceRepository.findById(savedBalance.getId()).orElseThrow();
        assertThat(balance.getUsedDays()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void approve_alreadyApproved_throwsAlreadyProcessed() {
        leaveRequestService.approve(savedLeaveRequest.getId(), new ApprovalActionRequest("승인"));

        ErpException ex = assertThrows(ErpException.class, () ->
            leaveRequestService.approve(savedLeaveRequest.getId(), new ApprovalActionRequest("재승인")));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.APPROVAL_ALREADY_PROCESSED);
    }

    @Test
    void approve_unauthenticated_throwsNotAuthorized() {
        // 보안: 인증되지 않은(현재 사용자 null) 요청이 SYSTEM으로 승격돼 결재를 우회하지 못한다.
        SecurityContextHolder.clearContext();

        ErpException ex = assertThrows(ErpException.class, () ->
            leaveRequestService.approve(savedLeaveRequest.getId(), new ApprovalActionRequest("우회 시도")));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.APPROVER_NOT_AUTHORIZED);
    }

    @Test
    void approve_byNonApprover_throwsNotAuthorized() {
        // 현재 단계 결재자(MANAGER)가 아닌 다른 사용자는 결재할 수 없다.
        authenticateAs("OTHER-USER");

        ErpException ex = assertThrows(ErpException.class, () ->
            leaveRequestService.approve(savedLeaveRequest.getId(), new ApprovalActionRequest("권한 없음")));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.APPROVER_NOT_AUTHORIZED);
    }
}
