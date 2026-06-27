package com.erp.hr.application.service;

import com.erp.common.security.Permission;
import com.erp.common.security.PermissionChecker;
import com.erp.hr.application.dto.DepartmentHeadcountResponse;
import com.erp.hr.application.dto.EmployeeStatusCountResponse;
import com.erp.hr.application.dto.EmploymentTypeCountResponse;
import com.erp.hr.application.dto.LeaveTypeStatResponse;
import com.erp.hr.application.dto.MonthlyHiresTerminationsResponse;
import com.erp.hr.application.dto.PositionHeadcountResponse;
import com.erp.hr.application.service.HrDataScopeResolver.EmployeeAnalyticsScope;
import com.erp.hr.domain.model.EmployeeStatus;
import com.erp.hr.domain.model.EmploymentType;
import com.erp.hr.domain.model.LeavePolicy.LeaveType;
import com.erp.hr.domain.repository.DepartmentRepository;
import com.erp.hr.domain.repository.EmployeeRepository;
import com.erp.hr.domain.repository.EmployeeStatusCountRow;
import com.erp.hr.domain.repository.EmploymentTypeCountRow;
import com.erp.hr.domain.repository.LeaveRequestRepository;
import com.erp.hr.domain.repository.LeaveTypeStatRow;
import com.erp.hr.domain.repository.MonthlyCountRow;
import com.erp.hr.domain.repository.PositionRepository;
import java.math.BigDecimal;
import java.time.Year;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * HR 분석 집계 — CrmAnalytics/FinanceAnalytics 청사진 복제. 기능 권한 + 데이터 스코프를 적용한다. 분포 지표는 enum 전체를 0채움,
 * 부서/직위는 빈 그룹을 count=0으로 보존, 월별은 1~12월 0채움.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HrAnalyticsService {

  private static final int MONTHS_IN_YEAR = 12;

  private final EmployeeRepository employeeRepository;
  private final DepartmentRepository departmentRepository;
  private final PositionRepository positionRepository;
  private final LeaveRequestRepository leaveRequestRepository;
  private final HrDataScopeResolver dataScopeResolver;
  private final PermissionChecker permissionChecker;

  public List<EmployeeStatusCountResponse> getStatusDistribution() {
    permissionChecker.require(Permission.HR_EMPLOYEE_READ);
    EmployeeAnalyticsScope s = dataScopeResolver.employeeAnalyticsScope();
    Map<EmployeeStatus, Long> countMap =
        employeeRepository.statusDistribution(s.unscoped(), s.selfUserId(), s.deptIds()).stream()
            .collect(
                Collectors.toMap(
                    EmployeeStatusCountRow::getStatus, EmployeeStatusCountRow::getCount));
    return Arrays.stream(EmployeeStatus.values())
        .map(status -> new EmployeeStatusCountResponse(status, countMap.getOrDefault(status, 0L)))
        .collect(Collectors.toList());
  }

  public List<DepartmentHeadcountResponse> getHeadcountByDepartment() {
    permissionChecker.require(Permission.HR_EMPLOYEE_READ);
    EmployeeAnalyticsScope s = dataScopeResolver.employeeAnalyticsScope();
    return departmentRepository.headcountByDepartment(s.unscoped(), s.deptIds()).stream()
        .map(
            r ->
                new DepartmentHeadcountResponse(
                    r.getDepartmentId(), r.getDepartmentName(), r.getCount()))
        .collect(Collectors.toList());
  }

  public List<PositionHeadcountResponse> getHeadcountByPosition() {
    permissionChecker.require(Permission.HR_EMPLOYEE_READ);
    EmployeeAnalyticsScope s = dataScopeResolver.employeeAnalyticsScope();
    return positionRepository
        .headcountByPosition(s.unscoped(), s.selfUserId(), s.deptIds())
        .stream()
        .map(
            r ->
                new PositionHeadcountResponse(r.getPositionId(), r.getPositionName(), r.getCount()))
        .collect(Collectors.toList());
  }

  public List<EmploymentTypeCountResponse> getEmploymentTypeDistribution() {
    permissionChecker.require(Permission.HR_EMPLOYEE_READ);
    EmployeeAnalyticsScope s = dataScopeResolver.employeeAnalyticsScope();
    Map<EmploymentType, Long> countMap =
        employeeRepository
            .employmentTypeDistribution(s.unscoped(), s.selfUserId(), s.deptIds())
            .stream()
            .collect(
                Collectors.toMap(
                    EmploymentTypeCountRow::getEmploymentType, EmploymentTypeCountRow::getCount));
    return Arrays.stream(EmploymentType.values())
        .map(type -> new EmploymentTypeCountResponse(type, countMap.getOrDefault(type, 0L)))
        .collect(Collectors.toList());
  }

  public List<MonthlyHiresTerminationsResponse> getHiresTerminations(Integer year) {
    permissionChecker.require(Permission.HR_EMPLOYEE_READ);
    int targetYear = year != null ? year : Year.now().getValue();
    EmployeeAnalyticsScope s = dataScopeResolver.employeeAnalyticsScope();

    Map<Integer, Long> hires =
        byMonth(
            employeeRepository.monthlyHires(targetYear, s.unscoped(), s.selfUserId(), s.deptIds()));
    Map<Integer, Long> terms =
        byMonth(
            employeeRepository.monthlyTerminations(
                targetYear, s.unscoped(), s.selfUserId(), s.deptIds()));

    List<MonthlyHiresTerminationsResponse> result = new ArrayList<>(MONTHS_IN_YEAR);
    for (int month = 1; month <= MONTHS_IN_YEAR; month++) {
      result.add(
          new MonthlyHiresTerminationsResponse(
              month, hires.getOrDefault(month, 0L), terms.getOrDefault(month, 0L)));
    }
    return result;
  }

  public List<LeaveTypeStatResponse> getLeavesByType() {
    permissionChecker.require(Permission.HR_LEAVE_READ);
    EmployeeAnalyticsScope s = dataScopeResolver.employeeAnalyticsScope();
    Map<LeaveType, LeaveTypeStatRow> rowMap =
        leaveRequestRepository.leaveStatsByType(s.unscoped(), s.selfUserId(), s.deptIds()).stream()
            .collect(Collectors.toMap(LeaveTypeStatRow::getLeaveType, r -> r));
    return Arrays.stream(LeaveType.values())
        .map(
            type -> {
              LeaveTypeStatRow row = rowMap.get(type);
              return row != null
                  ? new LeaveTypeStatResponse(type, row.getCount(), row.getTotalDays())
                  : new LeaveTypeStatResponse(type, 0L, BigDecimal.ZERO);
            })
        .collect(Collectors.toList());
  }

  private Map<Integer, Long> byMonth(List<MonthlyCountRow> rows) {
    Map<Integer, Long> map = new HashMap<>();
    for (MonthlyCountRow row : rows) {
      map.put(row.getMonth(), row.getCount());
    }
    return map;
  }
}
