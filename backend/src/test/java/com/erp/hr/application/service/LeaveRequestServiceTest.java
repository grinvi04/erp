package com.erp.hr.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.security.CurrentUserProvider;
import com.erp.common.workflow.ApprovalRequest;
import com.erp.common.workflow.repository.ApprovalRequestRepository;
import com.erp.hr.application.dto.LeaveRequestCreateRequest;
import com.erp.hr.domain.model.Department;
import com.erp.hr.domain.model.Employee;
import com.erp.hr.domain.model.EmploymentType;
import com.erp.hr.domain.model.LeaveBalance;
import com.erp.hr.domain.model.LeavePolicy;
import com.erp.hr.domain.model.LeaveRequest;
import com.erp.hr.domain.model.PersonalInfo;
import com.erp.hr.domain.model.Position;
import com.erp.hr.domain.repository.EmployeeRepository;
import com.erp.hr.domain.repository.LeaveBalanceRepository;
import com.erp.hr.domain.repository.LeavePolicyRepository;
import com.erp.hr.domain.repository.LeaveRequestRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LeaveRequestServiceTest {

  @Mock private LeaveRequestRepository leaveRequestRepository;
  @Mock private LeaveBalanceRepository leaveBalanceRepository;
  @Mock private EmployeeRepository employeeRepository;
  @Mock private LeavePolicyRepository leavePolicyRepository;
  @Mock private ApprovalRequestRepository approvalRequestRepository;
  @Mock private CurrentUserProvider currentUserProvider;
  @Mock private com.erp.common.security.PermissionChecker permissionChecker;
  @Mock private HrDataScopeResolver dataScopeResolver;
  @Mock private com.erp.common.audit.AuditService auditService;

  @InjectMocks private LeaveRequestService leaveRequestService;

  private Employee buildEmployee() {
    Department dept = Department.createRoot("DEV", "개발팀");
    Position pos = Position.of("P001", "Engineer", 2);
    PersonalInfo info =
        new PersonalInfo(
            "김", "개발", LocalDate.of(1990, 1, 1), PersonalInfo.Gender.MALE, null, null, null);
    return Employee.create(
        "EMP001",
        info,
        dept,
        pos,
        null,
        LocalDate.of(2020, 3, 1),
        EmploymentType.REGULAR,
        "dev@test.com",
        BigDecimal.valueOf(50000000));
  }

  @Test
  void create_insufficientBalance_throwsLeaveBalanceInsufficient() {
    Employee emp = buildEmployee();
    given(employeeRepository.findById(1L)).willReturn(Optional.of(emp));

    LeavePolicy policy =
        LeavePolicy.of("ANNUAL", "연차", LeavePolicy.LeaveType.ANNUAL, 15, 5, true, 1);
    given(leavePolicyRepository.findById(1L)).willReturn(Optional.of(policy));

    LeaveBalance balance = mock(LeaveBalance.class);
    given(balance.hasSufficientBalance(any(BigDecimal.class))).willReturn(false);
    given(leaveBalanceRepository.findByEmployeeIdAndLeavePolicyIdAndYear(1L, 1L, 2024))
        .willReturn(Optional.of(balance));

    LeaveRequestCreateRequest request =
        new LeaveRequestCreateRequest(
            1L,
            1L,
            LocalDate.of(2024, 3, 1),
            LocalDate.of(2024, 3, 5),
            BigDecimal.valueOf(5),
            null);

    ErpException ex = assertThrows(ErpException.class, () -> leaveRequestService.create(request));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.LEAVE_BALANCE_INSUFFICIENT);
  }

  @Test
  void create_overlappingApprovedLeave_throwsLeaveOverlap() {
    Employee emp = buildEmployee();
    given(employeeRepository.findById(1L)).willReturn(Optional.of(emp));

    LeavePolicy policy =
        LeavePolicy.of("ANNUAL", "연차", LeavePolicy.LeaveType.ANNUAL, 15, 5, true, 1);
    given(leavePolicyRepository.findById(1L)).willReturn(Optional.of(policy));

    LeaveBalance balance = mock(LeaveBalance.class);
    given(balance.hasSufficientBalance(any(BigDecimal.class))).willReturn(true);
    given(leaveBalanceRepository.findByEmployeeIdAndLeavePolicyIdAndYear(1L, 1L, 2024))
        .willReturn(Optional.of(balance));

    given(
            leaveRequestRepository.findOverlappingByStatuses(
                eq(1L), any(LocalDate.class), any(LocalDate.class), any()))
        .willReturn(List.of(mock(LeaveRequest.class)));

    LeaveRequestCreateRequest request =
        new LeaveRequestCreateRequest(
            1L,
            1L,
            LocalDate.of(2024, 3, 1),
            LocalDate.of(2024, 3, 5),
            BigDecimal.valueOf(5),
            null);

    ErpException ex = assertThrows(ErpException.class, () -> leaveRequestService.create(request));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.LEAVE_OVERLAP);
  }

  @Test
  void create_valid_returnsLeaveRequestResponse() {
    Employee emp = buildEmployee();
    given(employeeRepository.findById(1L)).willReturn(Optional.of(emp));

    LeavePolicy policy =
        LeavePolicy.of("ANNUAL", "연차", LeavePolicy.LeaveType.ANNUAL, 15, 5, true, 1);
    given(leavePolicyRepository.findById(1L)).willReturn(Optional.of(policy));

    LeaveBalance balance = mock(LeaveBalance.class);
    given(balance.hasSufficientBalance(any(BigDecimal.class))).willReturn(true);
    given(leaveBalanceRepository.findByEmployeeIdAndLeavePolicyIdAndYear(1L, 1L, 2024))
        .willReturn(Optional.of(balance));

    given(
            leaveRequestRepository.findOverlappingByStatuses(
                eq(1L), any(LocalDate.class), any(LocalDate.class), any()))
        .willReturn(List.of());

    LeaveRequest savedRequest =
        LeaveRequest.create(
            emp,
            policy,
            LocalDate.of(2024, 3, 1),
            LocalDate.of(2024, 3, 5),
            BigDecimal.valueOf(5),
            null);
    given(leaveRequestRepository.save(any())).willReturn(savedRequest);

    ApprovalRequest approvalRequest = mock(ApprovalRequest.class);
    given(approvalRequest.getId()).willReturn(10L);
    given(approvalRequestRepository.save(any())).willReturn(approvalRequest);

    given(currentUserProvider.getCurrentUserId()).willReturn("user-001");

    LeaveRequestCreateRequest request =
        new LeaveRequestCreateRequest(
            1L,
            1L,
            LocalDate.of(2024, 3, 1),
            LocalDate.of(2024, 3, 5),
            BigDecimal.valueOf(5),
            null);

    var result = leaveRequestService.create(request);

    assertThat(result.employeeId()).isEqualTo(emp.getId());
    assertThat(result.leavePolicyName()).isEqualTo("연차");
  }

  @Test
  void create_managerWithLinkedUserAccount_setsApproverToManagerUserId() {
    // 신원 버그 수정 회귀: 결재자는 매니저의 사번이 아니라 연결된 Keycloak sub여야
    // approve() 검증(sub 기준)과 일치한다.
    Employee manager = buildEmployee();
    manager.linkUserAccount("manager-sub-123");
    Employee emp = buildEmployee();
    emp.assignManager(manager);
    given(employeeRepository.findById(1L)).willReturn(Optional.of(emp));

    LeavePolicy policy =
        LeavePolicy.of("ANNUAL", "연차", LeavePolicy.LeaveType.ANNUAL, 15, 5, true, 1);
    given(leavePolicyRepository.findById(1L)).willReturn(Optional.of(policy));

    LeaveBalance balance = mock(LeaveBalance.class);
    given(balance.hasSufficientBalance(any(BigDecimal.class))).willReturn(true);
    given(leaveBalanceRepository.findByEmployeeIdAndLeavePolicyIdAndYear(1L, 1L, 2024))
        .willReturn(Optional.of(balance));
    given(
            leaveRequestRepository.findOverlappingByStatuses(
                eq(1L), any(LocalDate.class), any(LocalDate.class), any()))
        .willReturn(List.of());
    given(leaveRequestRepository.save(any()))
        .willReturn(
            LeaveRequest.create(
                emp,
                policy,
                LocalDate.of(2024, 3, 1),
                LocalDate.of(2024, 3, 5),
                BigDecimal.valueOf(5),
                null));
    given(currentUserProvider.getCurrentUserId()).willReturn("user-001");

    ArgumentCaptor<ApprovalRequest> captor = ArgumentCaptor.forClass(ApprovalRequest.class);
    given(approvalRequestRepository.save(captor.capture())).willAnswer(inv -> inv.getArgument(0));

    leaveRequestService.create(
        new LeaveRequestCreateRequest(
            1L,
            1L,
            LocalDate.of(2024, 3, 1),
            LocalDate.of(2024, 3, 5),
            BigDecimal.valueOf(5),
            null));

    assertThat(captor.getValue().getCurrentStepApproverId()).isEqualTo("manager-sub-123");
  }
}
