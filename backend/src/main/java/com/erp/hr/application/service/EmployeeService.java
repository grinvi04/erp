package com.erp.hr.application.service;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.security.Permission;
import com.erp.common.security.PermissionChecker;
import com.erp.hr.application.dto.EmployeeCreateRequest;
import com.erp.hr.application.dto.EmployeePromoteRequest;
import com.erp.hr.application.dto.EmployeeResponse;
import com.erp.hr.application.dto.EmployeeTerminateRequest;
import com.erp.hr.application.dto.EmployeeTransferRequest;
import com.erp.hr.application.dto.EmployeeUpdateRequest;
import com.erp.hr.domain.model.Department;
import com.erp.hr.domain.model.Employee;
import com.erp.hr.domain.model.EmployeeStatus;
import com.erp.hr.domain.model.JobGrade;
import com.erp.hr.domain.model.PersonalInfo;
import com.erp.hr.domain.model.Position;
import com.erp.hr.domain.repository.DepartmentRepository;
import com.erp.hr.domain.repository.EmployeeRepository;
import com.erp.hr.domain.repository.JobGradeRepository;
import com.erp.hr.domain.repository.PositionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final PositionRepository positionRepository;
    private final JobGradeRepository jobGradeRepository;
    private final PermissionChecker permissionChecker;
    private final HrDataScopeResolver dataScopeResolver;

    public Page<EmployeeResponse> findAll(EmployeeStatus status, Long departmentId, Pageable pageable) {
        permissionChecker.require(Permission.HR_EMPLOYEE_READ);
        Specification<Employee> spec = Specification.where(null);
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (departmentId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("department").get("id"), departmentId));
        }
        // 데이터 스코프 공통 필터 — 부서/본인 범위 밖 직원은 목록에서 제외(정보 유출 방지)
        spec = spec.and(dataScopeResolver.employeeScope());
        return employeeRepository.findAll(spec, pageable).map(EmployeeResponse::from);
    }

    public EmployeeResponse findById(Long id) {
        permissionChecker.require(Permission.HR_EMPLOYEE_READ);
        Employee employee = getOrThrow(id);
        if (!dataScopeResolver.isInScope(employee)) {
            throw new ErpException(ErrorCode.FORBIDDEN);
        }
        return EmployeeResponse.from(employee);
    }

    @Transactional
    public EmployeeResponse create(EmployeeCreateRequest request) {
        permissionChecker.require(Permission.HR_EMPLOYEE_WRITE);
        if (employeeRepository.existsByEmployeeNo(request.employeeNo())) {
            throw new ErpException(ErrorCode.DUPLICATE_CODE);
        }
        if (employeeRepository.existsByWorkEmail(request.workEmail())) {
            throw new ErpException(ErrorCode.DUPLICATE_CODE);
        }
        Department department = departmentRepository.findById(request.departmentId())
            .orElseThrow(() -> new ErpException(ErrorCode.DEPARTMENT_NOT_FOUND));
        Position position = positionRepository.findById(request.positionId())
            .orElseThrow(() -> new ErpException(ErrorCode.POSITION_NOT_FOUND));
        JobGrade jobGrade = null;
        if (request.jobGradeId() != null) {
            jobGrade = jobGradeRepository.findById(request.jobGradeId())
                .orElseThrow(() -> new ErpException(ErrorCode.JOB_GRADE_NOT_FOUND));
        }
        PersonalInfo personalInfo = new PersonalInfo(
            request.lastName(), request.firstName(), request.dateOfBirth(),
            request.gender(), request.nationalId(), request.phone(), request.personalEmail());
        Employee employee = Employee.create(
            request.employeeNo(), personalInfo, department, position, jobGrade,
            request.hireDate(), request.employmentType(), request.workEmail(), request.baseSalary());
        if (request.managerId() != null) {
            Employee manager = getOrThrow(request.managerId());
            employee.assignManager(manager);
        }
        return EmployeeResponse.from(employeeRepository.save(employee));
    }

    @Transactional
    public EmployeeResponse transfer(Long id, EmployeeTransferRequest request) {
        permissionChecker.require(Permission.HR_EMPLOYEE_WRITE);
        Employee employee = getOrThrow(id);
        if (employee.isTerminated()) {
            throw new ErpException(ErrorCode.EMPLOYEE_ALREADY_TERMINATED);
        }
        Department department = departmentRepository.findById(request.departmentId())
            .orElseThrow(() -> new ErpException(ErrorCode.DEPARTMENT_NOT_FOUND));
        Position position = positionRepository.findById(request.positionId())
            .orElseThrow(() -> new ErpException(ErrorCode.POSITION_NOT_FOUND));
        employee.transfer(department, position);
        return EmployeeResponse.from(employee);
    }

    @Transactional
    public EmployeeResponse promote(Long id, EmployeePromoteRequest request) {
        permissionChecker.require(Permission.HR_EMPLOYEE_WRITE);
        Employee employee = getOrThrow(id);
        if (employee.isTerminated()) {
            throw new ErpException(ErrorCode.EMPLOYEE_ALREADY_TERMINATED);
        }
        Position position = positionRepository.findById(request.positionId())
            .orElseThrow(() -> new ErpException(ErrorCode.POSITION_NOT_FOUND));
        JobGrade jobGrade = null;
        if (request.jobGradeId() != null) {
            jobGrade = jobGradeRepository.findById(request.jobGradeId())
                .orElseThrow(() -> new ErpException(ErrorCode.JOB_GRADE_NOT_FOUND));
        }
        employee.promote(position, jobGrade, request.baseSalary());
        return EmployeeResponse.from(employee);
    }

    @Transactional
    public EmployeeResponse terminate(Long id, EmployeeTerminateRequest request) {
        permissionChecker.require(Permission.HR_EMPLOYEE_WRITE);
        Employee employee = getOrThrow(id);
        try {
            employee.terminate(request.terminationDate());
        } catch (IllegalStateException e) {
            throw new ErpException(ErrorCode.EMPLOYEE_ALREADY_TERMINATED);
        }
        return EmployeeResponse.from(employee);
    }

    @Transactional
    public EmployeeResponse onLeave(Long id) {
        permissionChecker.require(Permission.HR_EMPLOYEE_WRITE);
        Employee employee = getOrThrow(id);
        try {
            employee.onLeave();
        } catch (IllegalStateException e) {
            throw new ErpException(ErrorCode.EMPLOYEE_STATUS_CONFLICT);
        }
        return EmployeeResponse.from(employee);
    }

    @Transactional
    public EmployeeResponse returnFromLeave(Long id) {
        permissionChecker.require(Permission.HR_EMPLOYEE_WRITE);
        Employee employee = getOrThrow(id);
        try {
            employee.returnFromLeave();
        } catch (IllegalStateException e) {
            throw new ErpException(ErrorCode.EMPLOYEE_STATUS_CONFLICT);
        }
        return EmployeeResponse.from(employee);
    }

    @Transactional
    public EmployeeResponse update(Long id, EmployeeUpdateRequest request) {
        permissionChecker.require(Permission.HR_EMPLOYEE_WRITE);
        Employee employee = getOrThrow(id);
        if (employee.isTerminated()) {
            throw new ErpException(ErrorCode.EMPLOYEE_ALREADY_TERMINATED);
        }
        if (request.workEmail() != null
                && !request.workEmail().equals(employee.getWorkEmail())
                && employeeRepository.existsByWorkEmail(request.workEmail())) {
            throw new ErpException(ErrorCode.DUPLICATE_CODE);
        }
        employee.updateInfo(
            request.lastName(), request.firstName(), request.phone(),
            request.personalEmail(), request.workEmail(), request.baseSalary());
        if (request.managerId() != null) {
            if (request.managerId().equals(id)) {
                throw new ErpException(ErrorCode.INVALID_INPUT);
            }
            Employee manager = getOrThrow(request.managerId());
            employee.assignManager(manager);
        }
        if (request.userId() != null) {
            String newUserId = request.userId().isBlank() ? null : request.userId();
            if (newUserId != null && !newUserId.equals(employee.getUserId())
                    && employeeRepository.existsByUserId(newUserId)) {
                throw new ErpException(ErrorCode.DUPLICATE_CODE);
            }
            employee.linkUserAccount(newUserId);
        }
        return EmployeeResponse.from(employee);
    }

    private Employee getOrThrow(Long id) {
        return employeeRepository.findById(id)
            .orElseThrow(() -> new ErpException(ErrorCode.EMPLOYEE_NOT_FOUND));
    }
}
