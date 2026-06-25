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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(String subject) {
        Jwt jwt = Jwt.withTokenValue("test-token").header("alg", "none")
            .subject(subject).claim("sub", subject).build();
        SecurityContextHolder.getContext().setAuthentication(
            new JwtAuthenticationToken(jwt,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @Test
    void approve_finalStep_approvesLeaveAndDeductsBalance() {
        authenticateAs("MANAGER");
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
        authenticateAs("MANAGER");
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
        authenticateAs("MANAGER");
        leaveRequestService.approve(savedLeaveRequest.getId(), new ApprovalActionRequest("승인"));

        ErpException ex = assertThrows(ErpException.class, () ->
            leaveRequestService.approve(savedLeaveRequest.getId(), new ApprovalActionRequest("재승인")));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.APPROVAL_ALREADY_PROCESSED);
    }
}
