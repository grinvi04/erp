package com.erp.hr.application.service;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.hr.application.dto.DepartmentCreateRequest;
import com.erp.hr.application.dto.DepartmentResponse;
import com.erp.hr.domain.model.Department;
import com.erp.hr.domain.model.Employee;
import com.erp.hr.domain.repository.DepartmentRepository;
import com.erp.hr.domain.repository.EmployeeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private DepartmentService departmentService;

    @Test
    void create_newCode_returnsCreatedDept() {
        given(departmentRepository.existsByCode("DEV")).willReturn(false);
        Department dept = Department.createRoot("DEV", "개발팀");
        given(departmentRepository.save(any())).willReturn(dept);

        DepartmentResponse result = departmentService.create(
            new DepartmentCreateRequest("DEV", "개발팀", null, 0));

        assertThat(result.code()).isEqualTo("DEV");
        assertThat(result.name()).isEqualTo("개발팀");
    }

    @Test
    void create_duplicateCode_throwsDuplicateCode() {
        given(departmentRepository.existsByCode("DEV")).willReturn(true);

        ErpException ex = assertThrows(ErpException.class, () ->
            departmentService.create(new DepartmentCreateRequest("DEV", "개발팀", null, 0)));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_CODE);
    }

    @Test
    void delete_withChildren_throwsDepartmentHasChildren() {
        Department dept = Department.createRoot("ROOT", "루트");
        given(departmentRepository.findById(1L)).willReturn(Optional.of(dept));
        given(departmentRepository.findByParentId(1L))
            .willReturn(List.of(Department.createRoot("CHILD", "자식")));

        ErpException ex = assertThrows(ErpException.class, () -> departmentService.delete(1L));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.DEPARTMENT_HAS_CHILDREN);
    }

    @Test
    void delete_withMembers_throwsDepartmentHasMembers() {
        Department dept = Department.createRoot("ROOT", "루트");
        given(departmentRepository.findById(1L)).willReturn(Optional.of(dept));
        given(departmentRepository.findByParentId(1L)).willReturn(List.of());
        given(employeeRepository.findByDepartmentId(1L)).willReturn(List.of(mock(Employee.class)));

        ErpException ex = assertThrows(ErpException.class, () -> departmentService.delete(1L));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.DEPARTMENT_HAS_MEMBERS);
    }

    @Test
    void delete_empty_callsSoftDelete() {
        Department dept = Department.createRoot("ROOT", "루트");
        given(departmentRepository.findById(1L)).willReturn(Optional.of(dept));
        given(departmentRepository.findByParentId(1L)).willReturn(List.of());
        given(employeeRepository.findByDepartmentId(1L)).willReturn(List.of());

        departmentService.delete(1L);

        assertThat(dept.isDeleted()).isTrue();
    }

    @Test
    void findById_notFound_throwsDepartmentNotFound() {
        given(departmentRepository.findById(99L)).willReturn(Optional.empty());

        ErpException ex = assertThrows(ErpException.class, () -> departmentService.findById(99L));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.DEPARTMENT_NOT_FOUND);
    }
}
