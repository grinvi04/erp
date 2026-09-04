package com.erp.hr.application.service;

import com.erp.common.security.Permission;
import com.erp.common.security.PermissionChecker;
import com.erp.common.workflow.ApprovalStatus;
import com.erp.hr.application.dto.HrSummaryResponse;
import com.erp.hr.application.service.HrDataScopeResolver.EmployeeAnalyticsScope;
import com.erp.hr.domain.model.EmployeeStatus;
import com.erp.hr.domain.repository.EmployeeRepository;
import com.erp.hr.domain.repository.LeaveRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HrSummaryService {

  private final EmployeeRepository employeeRepository;
  private final LeaveRequestRepository leaveRequestRepository;
  private final HrDataScopeResolver dataScopeResolver;
  private final PermissionChecker permissionChecker;

  public HrSummaryResponse getSummary() {
    permissionChecker.require(Permission.HR_EMPLOYEE_READ);
    permissionChecker.require(Permission.HR_LEAVE_READ);
    EmployeeAnalyticsScope scope = dataScopeResolver.employeeAnalyticsScope();
    return new HrSummaryResponse(
        employeeRepository.countByStatusInScope(
            EmployeeStatus.ACTIVE, scope.unscoped(), scope.selfUserId(), scope.deptIds()),
        employeeRepository.countByStatusInScope(
            EmployeeStatus.ON_LEAVE, scope.unscoped(), scope.selfUserId(), scope.deptIds()),
        leaveRequestRepository.countByApprovalStatusInScope(
            ApprovalStatus.PENDING, scope.unscoped(), scope.selfUserId(), scope.deptIds()));
  }
}
