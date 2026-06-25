package com.erp.hr.application.service;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.hr.application.dto.ContractCreateRequest;
import com.erp.hr.application.dto.ContractResponse;
import com.erp.hr.domain.model.Contract;
import com.erp.hr.domain.model.ContractType;
import com.erp.hr.domain.model.Department;
import com.erp.hr.domain.model.Employee;
import com.erp.hr.domain.model.EmploymentType;
import com.erp.hr.domain.model.PersonalInfo;
import com.erp.hr.domain.model.Position;
import com.erp.hr.domain.repository.ContractRepository;
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
import java.util.List;
import java.util.Optional;

import com.erp.common.security.Permission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ContractServiceTest {

    @Mock private ContractRepository contractRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private PositionRepository positionRepository;
    @Mock private JobGradeRepository jobGradeRepository;
    @Mock private com.erp.common.security.PermissionChecker permissionChecker;
    @Mock private HrDataScopeResolver dataScopeResolver;

    @InjectMocks
    private ContractService contractService;

    private Employee buildEmployee() {
        Department dept = Department.createRoot("DEV", "개발팀");
        Position pos = Position.of("P001", "Engineer", 2);
        PersonalInfo info = new PersonalInfo("김", "개발", LocalDate.of(1990, 1, 1),
            PersonalInfo.Gender.MALE, null, null, null);
        return Employee.create("EMP001", info, dept, pos, null,
            LocalDate.of(2020, 3, 1), EmploymentType.REGULAR, "dev@test.com", BigDecimal.valueOf(50000000));
    }

    @Test
    void findByEmployee_notFound_throwsEmployeeNotFound() {
        given(employeeRepository.findById(99L)).willReturn(Optional.empty());

        ErpException ex = assertThrows(ErpException.class, () -> contractService.findByEmployee(99L));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.EMPLOYEE_NOT_FOUND);
    }

    @Test
    void findByEmployee_existing_returnsList() {
        Employee emp = buildEmployee();
        given(employeeRepository.findById(1L)).willReturn(Optional.of(emp));
        given(dataScopeResolver.isEmployeeInScope(emp)).willReturn(true);

        Position pos = Position.of("P001", "Engineer", 2);
        Contract contract = Contract.create(emp, ContractType.REGULAR,
            LocalDate.of(2020, 3, 1), null, BigDecimal.valueOf(50000000), pos, null, null);
        given(contractRepository.findByEmployeeId(1L)).willReturn(List.of(contract));

        List<ContractResponse> result = contractService.findByEmployee(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).contractType()).isEqualTo(ContractType.REGULAR);
    }

    @Test
    void create_employeeNotFound_throwsNotFound() {
        given(employeeRepository.findById(99L)).willReturn(Optional.empty());

        ContractCreateRequest request = new ContractCreateRequest(
            ContractType.REGULAR, LocalDate.of(2024, 1, 1), null,
            BigDecimal.valueOf(50000000), 1L, null, null);

        ErpException ex = assertThrows(ErpException.class, () -> contractService.create(99L, request));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.EMPLOYEE_NOT_FOUND);
    }

    @Test
    void create_valid_returnsContractResponse() {
        Employee emp = buildEmployee();
        given(employeeRepository.findById(1L)).willReturn(Optional.of(emp));

        Position pos = Position.of("P001", "Engineer", 2);
        given(positionRepository.findById(1L)).willReturn(Optional.of(pos));

        Contract contract = Contract.create(emp, ContractType.REGULAR,
            LocalDate.of(2024, 1, 1), null, BigDecimal.valueOf(50000000), pos, null, null);
        given(contractRepository.save(any())).willReturn(contract);

        ContractCreateRequest request = new ContractCreateRequest(
            ContractType.REGULAR, LocalDate.of(2024, 1, 1), null,
            BigDecimal.valueOf(50000000), 1L, null, null);

        ContractResponse result = contractService.create(1L, request);

        assertThat(result.contractType()).isEqualTo(ContractType.REGULAR);
    }

    @Test
    void create_requiresWritePermission() {
        Employee emp = buildEmployee();
        given(employeeRepository.findById(1L)).willReturn(Optional.of(emp));

        Position pos = Position.of("P001", "Engineer", 2);
        given(positionRepository.findById(1L)).willReturn(Optional.of(pos));

        Contract contract = Contract.create(emp, ContractType.REGULAR,
            LocalDate.of(2024, 1, 1), null, BigDecimal.valueOf(50000000), pos, null, null);
        given(contractRepository.save(any())).willReturn(contract);

        ContractCreateRequest request = new ContractCreateRequest(
            ContractType.REGULAR, LocalDate.of(2024, 1, 1), null,
            BigDecimal.valueOf(50000000), 1L, null, null);

        contractService.create(1L, request);

        verify(permissionChecker).require(Permission.HR_EMPLOYEE_WRITE);
    }
}
