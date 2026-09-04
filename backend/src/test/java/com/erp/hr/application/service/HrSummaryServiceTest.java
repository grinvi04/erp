package com.erp.hr.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.security.Permission;
import com.erp.common.security.PermissionChecker;
import com.erp.common.workflow.ApprovalStatus;
import com.erp.hr.application.dto.HrSummaryResponse;
import com.erp.hr.application.service.HrDataScopeResolver.EmployeeAnalyticsScope;
import com.erp.hr.domain.model.EmployeeStatus;
import com.erp.hr.domain.repository.EmployeeRepository;
import com.erp.hr.domain.repository.LeaveRequestRepository;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HrSummaryServiceTest {

  @Mock private EmployeeRepository employeeRepository;
  @Mock private LeaveRequestRepository leaveRequestRepository;
  @Mock private HrDataScopeResolver dataScopeResolver;
  @Mock private PermissionChecker permissionChecker;
  @InjectMocks private HrSummaryService hrSummaryService;

  @Test
  void getSummary_requiresBothPermissionsAndAggregatesWithinDataScope() {
    EmployeeAnalyticsScope scope = new EmployeeAnalyticsScope(false, Set.of(10L, 11L), "user-1");
    given(dataScopeResolver.employeeAnalyticsScope()).willReturn(scope);
    given(
            employeeRepository.countByStatusInScope(
                EmployeeStatus.ACTIVE, false, "user-1", scope.deptIds()))
        .willReturn(42L);
    given(
            employeeRepository.countByStatusInScope(
                EmployeeStatus.ON_LEAVE, false, "user-1", scope.deptIds()))
        .willReturn(3L);
    given(
            leaveRequestRepository.countByApprovalStatusInScope(
                ApprovalStatus.PENDING, false, "user-1", scope.deptIds()))
        .willReturn(7L);

    HrSummaryResponse result = hrSummaryService.getSummary();

    assertThat(result.activeEmployees()).isEqualTo(42L);
    assertThat(result.onLeaveEmployees()).isEqualTo(3L);
    assertThat(result.pendingLeaveRequests()).isEqualTo(7L);

    InOrder authorizationOrder = inOrder(permissionChecker, dataScopeResolver);
    authorizationOrder.verify(permissionChecker).require(Permission.HR_EMPLOYEE_READ);
    authorizationOrder.verify(permissionChecker).require(Permission.HR_LEAVE_READ);
    authorizationOrder.verify(dataScopeResolver).employeeAnalyticsScope();
  }

  @Test
  void getSummary_withoutEmployeeReadPermission_doesNotQueryAggregates() {
    willThrow(new ErpException(ErrorCode.FORBIDDEN))
        .given(permissionChecker)
        .require(Permission.HR_EMPLOYEE_READ);

    assertThatThrownBy(hrSummaryService::getSummary)
        .isInstanceOf(ErpException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.FORBIDDEN);

    verifyNoInteractions(dataScopeResolver, employeeRepository, leaveRequestRepository);
  }

  @Test
  void getSummary_withoutLeaveReadPermission_doesNotQueryAggregates() {
    willAnswer(
            invocation -> {
              if (Permission.HR_LEAVE_READ.equals(invocation.getArgument(0))) {
                throw new ErpException(ErrorCode.FORBIDDEN);
              }
              return null;
            })
        .given(permissionChecker)
        .require(anyString());

    assertThatThrownBy(hrSummaryService::getSummary)
        .isInstanceOf(ErpException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.FORBIDDEN);

    then(permissionChecker).should().require(Permission.HR_EMPLOYEE_READ);
    verifyNoInteractions(dataScopeResolver, employeeRepository, leaveRequestRepository);
  }
}
