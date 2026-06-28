package com.erp.hr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.erp.common.AbstractIntegrationTest;
import com.erp.common.audit.AuditLog;
import com.erp.common.audit.AuditLogRepository;
import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.security.Permission;
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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

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
  @Autowired private AuditLogRepository auditLogRepository;

  private LeaveRequest savedLeaveRequest;
  private LeaveBalance savedBalance;

  @BeforeEach
  void setUp() {
    Department dept = departmentRepository.save(Department.createRoot("DEV", "개발팀"));
    Position pos = positionRepository.save(Position.of("P001", "Engineer", 2));

    PersonalInfo info =
        new PersonalInfo(
            "김", "개발", LocalDate.of(1990, 1, 1), PersonalInfo.Gender.MALE, null, null, null);
    Employee emp =
        employeeRepository.save(
            Employee.create(
                "EMP-IT001",
                info,
                dept,
                pos,
                null,
                LocalDate.of(2020, 3, 1),
                EmploymentType.REGULAR,
                "it001@test.com",
                BigDecimal.valueOf(50000000)));

    LeavePolicy policy =
        leavePolicyRepository.save(
            LeavePolicy.of("ANNUAL-IT", "연차", LeavePolicy.LeaveType.ANNUAL, 15, 5, true, 1));

    savedBalance =
        leaveBalanceRepository.save(
            LeaveBalance.create(emp, policy, 2024, BigDecimal.valueOf(15), BigDecimal.ZERO));

    LeaveRequest lr =
        LeaveRequest.create(
            emp,
            policy,
            LocalDate.of(2024, 4, 1),
            LocalDate.of(2024, 4, 3),
            BigDecimal.valueOf(3),
            "개인 사정");
    lr = leaveRequestRepository.save(lr);

    ApprovalRequest ar =
        ApprovalRequest.create(
            "LEAVE_REQUEST",
            lr.getId(),
            "연차 신청",
            "EMP-IT001",
            List.of(com.erp.common.workflow.ApprovalStep.of(1, "직속 상관 승인", "MANAGER")));
    ar = approvalRequestRepository.save(ar);
    lr.linkApprovalRequest(ar.getId());

    savedLeaveRequest = lr;
  }

  private void authenticateAs(String subject) {
    authenticate(subject, "ROLE_USER");
  }

  @Test
  void approve_finalStep_approvesLeaveAndDeductsBalance() {
    authenticateAs("MANAGER");
    LeaveRequestResponse result =
        leaveRequestService.approve(savedLeaveRequest.getId(), new ApprovalActionRequest("승인합니다"));

    assertThat(result.approvalStatus()).isEqualTo(ApprovalStatus.APPROVED);

    ApprovalRequest ar =
        approvalRequestRepository.findById(savedLeaveRequest.getApprovalRequestId()).orElseThrow();
    assertThat(ar.getStatus()).isEqualTo(ApprovalStatus.APPROVED);

    LeaveBalance balance = leaveBalanceRepository.findById(savedBalance.getId()).orElseThrow();
    assertThat(balance.getUsedDays()).isEqualByComparingTo(BigDecimal.valueOf(3));
    assertThat(balance.getRemainingDays()).isEqualByComparingTo(BigDecimal.valueOf(12));
  }

  @Test
  void reject_pendingRequest_rejectsLeaveRequest() {
    authenticateAs("MANAGER");
    LeaveRequestResponse result =
        leaveRequestService.reject(
            savedLeaveRequest.getId(), new ApprovalActionRequest("일정 조정 필요"));

    assertThat(result.approvalStatus()).isEqualTo(ApprovalStatus.REJECTED);

    ApprovalRequest ar =
        approvalRequestRepository.findById(savedLeaveRequest.getApprovalRequestId()).orElseThrow();
    assertThat(ar.getStatus()).isEqualTo(ApprovalStatus.REJECTED);

    LeaveBalance balance = leaveBalanceRepository.findById(savedBalance.getId()).orElseThrow();
    assertThat(balance.getUsedDays()).isEqualByComparingTo(BigDecimal.ZERO);
  }

  @Test
  void approve_alreadyApproved_throwsAlreadyProcessed() {
    authenticateAs("MANAGER");
    leaveRequestService.approve(savedLeaveRequest.getId(), new ApprovalActionRequest("승인"));

    ErpException ex =
        assertThrows(
            ErpException.class,
            () ->
                leaveRequestService.approve(
                    savedLeaveRequest.getId(), new ApprovalActionRequest("재승인")));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.APPROVAL_ALREADY_PROCESSED);
  }

  @Test
  void approve_unauthenticated_throwsNotAuthorized() {
    // 보안: 인증되지 않은(현재 사용자 null) 요청이 SYSTEM으로 승격돼 결재를 우회하지 못한다.
    clearAuth();

    ErpException ex =
        assertThrows(
            ErpException.class,
            () ->
                leaveRequestService.approve(
                    savedLeaveRequest.getId(), new ApprovalActionRequest("우회 시도")));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.APPROVER_NOT_AUTHORIZED);
  }

  @Test
  void approve_byNonApprover_throwsNotAuthorized() {
    // 현재 단계 결재자(MANAGER)가 아닌 다른 사용자는 결재할 수 없다.
    authenticateAs("OTHER-USER");

    ErpException ex =
        assertThrows(
            ErpException.class,
            () ->
                leaveRequestService.approve(
                    savedLeaveRequest.getId(), new ApprovalActionRequest("권한 없음")));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.APPROVER_NOT_AUTHORIZED);
  }

  @Test
  void approve_writesApproveAuditLog() {
    // 감사 추적: 결재 승인이 누가(MANAGER)·무엇을(LEAVE_REQUEST id) 결재했는지 로그로 남는다.
    authenticateAs("MANAGER");
    leaveRequestService.approve(savedLeaveRequest.getId(), new ApprovalActionRequest("승인합니다"));

    List<AuditLog> logs =
        auditLogRepository
            .search(
                TEST_TENANT_ID,
                "LEAVE_REQUEST",
                savedLeaveRequest.getId(),
                "MANAGER",
                null,
                null,
                null,
                PageRequest.of(0, 10))
            .getContent();

    assertThat(logs).hasSize(1);
    assertThat(logs.get(0).getAction()).isEqualTo(AuditLog.AuditAction.APPROVE);
    assertThat(logs.get(0).getPerformedBy()).isEqualTo("MANAGER");
  }

  @Test
  void reject_writesRejectAuditLog() {
    authenticateAs("MANAGER");
    leaveRequestService.reject(savedLeaveRequest.getId(), new ApprovalActionRequest("일정 조정 필요"));

    List<AuditLog> logs =
        auditLogRepository
            .search(
                TEST_TENANT_ID,
                "LEAVE_REQUEST",
                savedLeaveRequest.getId(),
                "MANAGER",
                null,
                null,
                null,
                PageRequest.of(0, 10))
            .getContent();

    assertThat(logs).hasSize(1);
    assertThat(logs.get(0).getAction()).isEqualTo(AuditLog.AuditAction.REJECT);
  }

  /** create()를 통해 매니저 기반 결재선을 세운 휴가신청을 만든다. managerSub가 null이면 매니저에 로그인 계정을 연결하지 않는다. */
  private LeaveRequestResponse createManagedLeave(String suffix, String managerSub) {
    Department dept = departmentRepository.save(Department.createRoot("D" + suffix, "팀" + suffix));
    Position pos = positionRepository.save(Position.of("PS" + suffix, "Eng", 2));

    PersonalInfo mInfo =
        new PersonalInfo(
            "이", "매니저", LocalDate.of(1980, 1, 1), PersonalInfo.Gender.MALE, null, null, null);
    Employee manager =
        Employee.create(
            "MGR-" + suffix,
            mInfo,
            dept,
            pos,
            null,
            LocalDate.of(2015, 1, 1),
            EmploymentType.REGULAR,
            "mgr" + suffix + "@test.com",
            BigDecimal.valueOf(70000000));
    if (managerSub != null) {
      manager.linkUserAccount(managerSub);
    }
    manager = employeeRepository.save(manager);

    PersonalInfo eInfo =
        new PersonalInfo(
            "김", "사원", LocalDate.of(1990, 1, 1), PersonalInfo.Gender.MALE, null, null, null);
    Employee emp =
        Employee.create(
            "EMP-" + suffix,
            eInfo,
            dept,
            pos,
            null,
            LocalDate.of(2021, 1, 1),
            EmploymentType.REGULAR,
            "emp" + suffix + "@test.com",
            BigDecimal.valueOf(50000000));
    emp.assignManager(manager);
    emp = employeeRepository.save(emp);

    LeavePolicy policy =
        leavePolicyRepository.save(
            LeavePolicy.of("AN-" + suffix, "연차", LeavePolicy.LeaveType.ANNUAL, 15, 5, true, 1));
    leaveBalanceRepository.save(
        LeaveBalance.create(emp, policy, 2024, BigDecimal.valueOf(15), BigDecimal.ZERO));

    authenticate("applicant-" + suffix, Permission.HR_LEAVE_WRITE);
    return leaveRequestService.create(
        new com.erp.hr.application.dto.LeaveRequestCreateRequest(
            emp.getId(),
            policy.getId(),
            LocalDate.of(2024, 5, 1),
            LocalDate.of(2024, 5, 3),
            BigDecimal.valueOf(3),
            "개인 사정"));
  }

  @Test
  void create_thenApproveByManagerSub_approvesAndDeductsBalance() {
    // 결재자 = 신청자 매니저의 sub. 그 sub로 로그인한 사용자가 승인하면 성공하고 잔여가 차감된다(T1-10 동반 해소).
    LeaveRequestResponse created = createManagedLeave("A", "manager-sub-A");

    clearAuth();
    authenticateAs("manager-sub-A");
    LeaveRequestResponse approved =
        leaveRequestService.approve(created.id(), new ApprovalActionRequest("승인합니다"));

    assertThat(approved.approvalStatus()).isEqualTo(ApprovalStatus.APPROVED);

    LeaveRequest lr = leaveRequestRepository.findById(created.id()).orElseThrow();
    int year = lr.getStartDate().getYear();
    LeaveBalance balance =
        leaveBalanceRepository
            .findByEmployeeIdAndLeavePolicyIdAndYear(
                lr.getEmployee().getId(), lr.getLeavePolicy().getId(), year)
            .orElseThrow();
    assertThat(balance.getUsedDays()).isEqualByComparingTo(BigDecimal.valueOf(3));
    assertThat(balance.getRemainingDays()).isEqualByComparingTo(BigDecimal.valueOf(12));
  }

  @Test
  void create_managerWithoutUserAccount_throwsApproverNotResolved() {
    // 차단 정책: 매니저의 로그인 계정이 없어 결재자를 해소하지 못하면 제출 자체를 막는다.
    ErpException ex = assertThrows(ErpException.class, () -> createManagedLeave("B", null));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.APPROVER_NOT_RESOLVED);
  }

  @Test
  void create_thenApproveByNonManager_throwsNotAuthorized() {
    // 매니저(=결재자)가 아닌 다른 사용자는 승인할 수 없다.
    LeaveRequestResponse created = createManagedLeave("C", "manager-sub-C");

    clearAuth();
    authenticateAs("someone-else");
    ErpException ex =
        assertThrows(
            ErpException.class,
            () -> leaveRequestService.approve(created.id(), new ApprovalActionRequest("권한 없음")));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.APPROVER_NOT_AUTHORIZED);
  }

  @Test
  void reject_blankComment_throwsInvalidInput() {
    // 반려 사유는 서버에서도 필수 — 빈 사유는 거부한다(클라이언트 전용 규칙 아님).
    authenticateAs("MANAGER");

    ErpException ex =
        assertThrows(
            ErpException.class,
            () ->
                leaveRequestService.reject(
                    savedLeaveRequest.getId(), new ApprovalActionRequest("  ")));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT);
  }
}
