package com.erp.hr.application.service;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.hr.application.dto.EmployeeCreateRequest;
import com.erp.hr.application.dto.EmployeeResponse;
import com.erp.hr.domain.model.Department;
import com.erp.hr.domain.model.Employee;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock private EmployeeRepository employeeRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private PositionRepository positionRepository;
    @Mock private JobGradeRepository jobGradeRepository;
    @Mock private com.erp.common.security.PermissionChecker permissionChecker;

    @InjectMocks
    private EmployeeService employeeService;

    private Employee buildEmployee() {
        Department dept = Department.createRoot("DEV", "개발팀");
        Position pos = Position.of("P001", "Engineer", 2);
        PersonalInfo info = new PersonalInfo("김", "개발", LocalDate.of(1990, 1, 1),
            PersonalInfo.Gender.MALE, null, null, null);
        return Employee.create("EMP001", info, dept, pos, null,
            LocalDate.of(2020, 3, 1), EmploymentType.REGULAR, "dev@test.com", BigDecimal.valueOf(50000000));
    }

    @Test
    void findAll_noFilter_returnsPage() {
        Employee emp = buildEmployee();
        Page<Employee> page = new PageImpl<>(List.of(emp));
        given(employeeRepository.findAll(any(Specification.class), any(PageRequest.class))).willReturn(page);

        Page<EmployeeResponse> result = employeeService.findAll(null, null, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).employeeNo()).isEqualTo("EMP001");
    }

    @Test
    void findById_notFound_throwsEmployeeNotFound() {
        given(employeeRepository.findById(99L)).willReturn(Optional.empty());

        ErpException ex = assertThrows(ErpException.class, () -> employeeService.findById(99L));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.EMPLOYEE_NOT_FOUND);
    }

    @Test
    void create_duplicateEmployeeNo_throwsDuplicateCode() {
        given(employeeRepository.existsByEmployeeNo("EMP001")).willReturn(true);

        EmployeeCreateRequest request = new EmployeeCreateRequest(
            "EMP001", "김", "개발", null, null, null, null, null,
            1L, 1L, null, LocalDate.now(), EmploymentType.REGULAR, "dev@test.com", null, null);

        ErpException ex = assertThrows(ErpException.class, () -> employeeService.create(request));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_CODE);
    }

    @Test
    void create_duplicateWorkEmail_throwsDuplicateCode() {
        given(employeeRepository.existsByEmployeeNo("EMP001")).willReturn(false);
        given(employeeRepository.existsByWorkEmail("dev@test.com")).willReturn(true);

        EmployeeCreateRequest request = new EmployeeCreateRequest(
            "EMP001", "김", "개발", null, null, null, null, null,
            1L, 1L, null, LocalDate.now(), EmploymentType.REGULAR, "dev@test.com", null, null);

        ErpException ex = assertThrows(ErpException.class, () -> employeeService.create(request));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_CODE);
    }

    @Test
    void create_validRequest_returnsCreatedEmployee() {
        given(employeeRepository.existsByEmployeeNo("EMP001")).willReturn(false);
        given(employeeRepository.existsByWorkEmail("dev@test.com")).willReturn(false);

        Department dept = Department.createRoot("DEV", "개발팀");
        Position pos = Position.of("P001", "Engineer", 2);
        given(departmentRepository.findById(1L)).willReturn(Optional.of(dept));
        given(positionRepository.findById(1L)).willReturn(Optional.of(pos));

        Employee saved = buildEmployee();
        given(employeeRepository.save(any())).willReturn(saved);

        EmployeeCreateRequest request = new EmployeeCreateRequest(
            "EMP001", "김", "개발", LocalDate.of(1990, 1, 1), PersonalInfo.Gender.MALE,
            null, null, null, 1L, 1L, null, LocalDate.of(2020, 3, 1),
            EmploymentType.REGULAR, "dev@test.com", BigDecimal.valueOf(50000000), null);

        EmployeeResponse result = employeeService.create(request);

        assertThat(result.employeeNo()).isEqualTo("EMP001");
        assertThat(result.workEmail()).isEqualTo("dev@test.com");
    }
}
