package com.erp.hr.application.service;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.hr.application.dto.EmployeePromoteRequest;
import com.erp.hr.application.dto.EmployeeTerminateRequest;
import com.erp.hr.application.dto.EmployeeTransferRequest;
import com.erp.hr.domain.model.Department;
import com.erp.hr.domain.model.Employee;
import com.erp.hr.domain.model.EmployeeStatus;
import com.erp.hr.domain.model.EmploymentType;
import com.erp.hr.domain.model.PersonalInfo;
import com.erp.hr.domain.model.Position;
import com.erp.hr.domain.repository.DepartmentRepository;
import com.erp.hr.domain.repository.EmployeeRepository;
import com.erp.hr.domain.repository.JobGradeRepository;
import com.erp.hr.domain.repository.PositionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class EmployeeOperationServiceTest {

    @Mock private EmployeeRepository employeeRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private PositionRepository positionRepository;
    @Mock private JobGradeRepository jobGradeRepository;
    @Mock private com.erp.common.security.PermissionChecker permissionChecker;
    @Mock private HrDataScopeResolver dataScopeResolver;
    @Mock private com.erp.common.audit.AuditService auditService;

    @InjectMocks
    private EmployeeService employeeService;

    private Employee activeEmployee() {
        Department dept = Department.createRoot("DEV", "개발팀");
        Position pos = Position.of("P001", "Engineer", 2);
        PersonalInfo info = new PersonalInfo("김", "개발", LocalDate.of(1990, 1, 1),
            PersonalInfo.Gender.MALE, null, null, null);
        return Employee.create("EMP001", info, dept, pos, null,
            LocalDate.of(2020, 3, 1), EmploymentType.REGULAR, "dev@test.com", BigDecimal.valueOf(50000000));
    }

    @Test
    void transfer_activeEmployee_updatesOrganization() {
        Employee emp = activeEmployee();
        given(employeeRepository.findById(1L)).willReturn(Optional.of(emp));

        Department newDept = Department.createRoot("QA", "QA팀");
        Position newPos = Position.of("P002", "QA Lead", 3);
        given(departmentRepository.findById(2L)).willReturn(Optional.of(newDept));
        given(positionRepository.findById(2L)).willReturn(Optional.of(newPos));

        var result = employeeService.transfer(1L, new EmployeeTransferRequest(2L, 2L));

        assertThat(result.departmentName()).isEqualTo("QA팀");
        assertThat(result.positionName()).isEqualTo("QA Lead");
    }

    @Test
    void transfer_terminatedEmployee_throwsConflict() {
        Employee emp = activeEmployee();
        emp.terminate(LocalDate.now());
        given(employeeRepository.findById(1L)).willReturn(Optional.of(emp));

        ErpException ex = assertThrows(ErpException.class,
            () -> employeeService.transfer(1L, new EmployeeTransferRequest(2L, 2L)));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.EMPLOYEE_ALREADY_TERMINATED);
    }

    @Test
    void terminate_activeEmployee_setsTerminated() {
        Employee emp = activeEmployee();
        given(employeeRepository.findById(1L)).willReturn(Optional.of(emp));

        var result = employeeService.terminate(1L, new EmployeeTerminateRequest(LocalDate.now()));

        assertThat(result.status()).isEqualTo(EmployeeStatus.TERMINATED);
    }

    @Test
    void terminate_alreadyTerminated_throwsConflict() {
        Employee emp = activeEmployee();
        emp.terminate(LocalDate.now());
        given(employeeRepository.findById(1L)).willReturn(Optional.of(emp));

        ErpException ex = assertThrows(ErpException.class,
            () -> employeeService.terminate(1L, new EmployeeTerminateRequest(LocalDate.now())));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.EMPLOYEE_ALREADY_TERMINATED);
    }

    @Test
    void onLeave_activeEmployee_setsOnLeave() {
        Employee emp = activeEmployee();
        given(employeeRepository.findById(1L)).willReturn(Optional.of(emp));

        var result = employeeService.onLeave(1L);

        assertThat(result.status()).isEqualTo(EmployeeStatus.ON_LEAVE);
    }

    @Test
    void onLeave_alreadyOnLeave_throwsStatusConflict() {
        Employee emp = activeEmployee();
        emp.onLeave();
        given(employeeRepository.findById(1L)).willReturn(Optional.of(emp));

        ErpException ex = assertThrows(ErpException.class, () -> employeeService.onLeave(1L));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.EMPLOYEE_STATUS_CONFLICT);
    }

    @Test
    void returnFromLeave_onLeaveEmployee_setsActive() {
        Employee emp = activeEmployee();
        emp.onLeave();
        given(employeeRepository.findById(1L)).willReturn(Optional.of(emp));

        var result = employeeService.returnFromLeave(1L);

        assertThat(result.status()).isEqualTo(EmployeeStatus.ACTIVE);
    }

    @Test
    void returnFromLeave_activeEmployee_throwsStatusConflict() {
        Employee emp = activeEmployee();
        given(employeeRepository.findById(1L)).willReturn(Optional.of(emp));

        ErpException ex = assertThrows(ErpException.class, () -> employeeService.returnFromLeave(1L));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.EMPLOYEE_STATUS_CONFLICT);
    }

    @Test
    void promote_activeEmployee_updatesSalaryAndPosition() {
        Employee emp = activeEmployee();
        given(employeeRepository.findById(1L)).willReturn(Optional.of(emp));

        Position newPos = Position.of("P003", "Senior Engineer", 4);
        given(positionRepository.findById(3L)).willReturn(Optional.of(newPos));

        var result = employeeService.promote(1L, new EmployeePromoteRequest(3L, null, BigDecimal.valueOf(70000000)));

        assertThat(result.positionName()).isEqualTo("Senior Engineer");
        assertThat(result.baseSalary()).isEqualByComparingTo(BigDecimal.valueOf(70000000));
    }
}
