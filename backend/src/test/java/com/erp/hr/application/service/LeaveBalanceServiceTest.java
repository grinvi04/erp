package com.erp.hr.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.hr.application.dto.LeaveBalanceResponse;
import com.erp.hr.domain.model.Department;
import com.erp.hr.domain.model.Employee;
import com.erp.hr.domain.model.EmploymentType;
import com.erp.hr.domain.model.LeaveBalance;
import com.erp.hr.domain.model.PersonalInfo;
import com.erp.hr.domain.model.Position;
import com.erp.hr.domain.repository.EmployeeRepository;
import com.erp.hr.domain.repository.LeaveBalanceRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LeaveBalanceServiceTest {

  @Mock private LeaveBalanceRepository leaveBalanceRepository;
  @Mock private EmployeeRepository employeeRepository;
  @Mock private com.erp.common.security.PermissionChecker permissionChecker;
  @Mock private HrDataScopeResolver dataScopeResolver;

  @InjectMocks private LeaveBalanceService leaveBalanceService;

  @Test
  void findByEmployeeAndYear_employeeNotFound_throwsNotFound() {
    given(employeeRepository.findById(99L)).willReturn(Optional.empty());

    ErpException ex =
        assertThrows(
            ErpException.class, () -> leaveBalanceService.findByEmployeeAndYear(99L, 2024));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.EMPLOYEE_NOT_FOUND);
  }

  @Test
  void findByEmployeeAndYear_existing_returnsBalances() {
    Department dept = Department.createRoot("DEV", "개발팀");
    Position pos = Position.of("P001", "Engineer", 2);
    PersonalInfo info =
        new PersonalInfo(
            "김", "개발", LocalDate.of(1990, 1, 1), PersonalInfo.Gender.MALE, null, null, null);
    Employee emp =
        Employee.create(
            "EMP001",
            info,
            dept,
            pos,
            null,
            LocalDate.of(2020, 3, 1),
            EmploymentType.REGULAR,
            "dev@test.com",
            BigDecimal.valueOf(50000000));
    given(employeeRepository.findById(1L)).willReturn(Optional.of(emp));
    given(dataScopeResolver.isEmployeeInScope(emp)).willReturn(true);

    LeaveBalance balance = mock(LeaveBalance.class);
    given(balance.getId()).willReturn(1L);
    given(balance.getEmployee()).willReturn(emp);
    given(balance.getLeavePolicy()).willReturn(mock(com.erp.hr.domain.model.LeavePolicy.class));
    given(balance.getLeavePolicy().getId()).willReturn(1L);
    given(balance.getLeavePolicy().getName()).willReturn("연차");
    given(balance.getYear()).willReturn(2024);
    given(balance.getEntitledDays()).willReturn(BigDecimal.valueOf(15));
    given(balance.getUsedDays()).willReturn(BigDecimal.valueOf(3));
    given(balance.getCarryOverDays()).willReturn(BigDecimal.valueOf(2));
    given(balance.getRemainingDays()).willReturn(BigDecimal.valueOf(14));
    given(leaveBalanceRepository.findByEmployeeIdAndYear(1L, 2024)).willReturn(List.of(balance));

    List<LeaveBalanceResponse> result = leaveBalanceService.findByEmployeeAndYear(1L, 2024);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).year()).isEqualTo(2024);
  }
}
