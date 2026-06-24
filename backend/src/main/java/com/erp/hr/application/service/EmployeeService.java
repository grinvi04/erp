package com.erp.hr.application.service;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
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

    public Page<EmployeeResponse> findAll(EmployeeStatus status, Long departmentId, Pageable pageable) {
        Specification<Employee> spec = Specification.where(null);
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (departmentId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("department").get("id"), departmentId));
        }
        return employeeRepository.findAll(spec, pageable).map(EmployeeResponse::from);
    }

    public EmployeeResponse findById(Long id) {
        return EmployeeResponse.from(getOrThrow(id));
    }

    @Transactional
    public EmployeeResponse create(EmployeeCreateRequest request) {
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
        Employee employee = getOrThrow(id);
        employee.updateInfo(
            request.lastName(), request.firstName(), request.phone(),
            request.personalEmail(), request.workEmail(), request.baseSalary());
        if (request.managerId() != null) {
            Employee manager = getOrThrow(request.managerId());
            employee.assignManager(manager);
        }
        return EmployeeResponse.from(employee);
    }

    private Employee getOrThrow(Long id) {
        return employeeRepository.findById(id)
            .orElseThrow(() -> new ErpException(ErrorCode.EMPLOYEE_NOT_FOUND));
    }
}
