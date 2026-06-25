package com.erp.hr.application.service;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.security.Permission;
import com.erp.common.security.PermissionChecker;
import com.erp.hr.application.dto.ContractCreateRequest;
import com.erp.hr.application.dto.ContractResponse;
import com.erp.hr.domain.model.Contract;
import com.erp.hr.domain.model.Employee;
import com.erp.hr.domain.model.JobGrade;
import com.erp.hr.domain.model.Position;
import com.erp.hr.domain.repository.ContractRepository;
import com.erp.hr.domain.repository.EmployeeRepository;
import com.erp.hr.domain.repository.JobGradeRepository;
import com.erp.hr.domain.repository.PositionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContractService {

    private final ContractRepository contractRepository;
    private final EmployeeRepository employeeRepository;
    private final PositionRepository positionRepository;
    private final JobGradeRepository jobGradeRepository;
    private final PermissionChecker permissionChecker;

    public List<ContractResponse> findByEmployee(Long employeeId) {
        permissionChecker.require(Permission.HR_EMPLOYEE_READ);
        employeeRepository.findById(employeeId)
            .orElseThrow(() -> new ErpException(ErrorCode.EMPLOYEE_NOT_FOUND));
        return contractRepository.findByEmployeeId(employeeId).stream()
            .map(ContractResponse::from)
            .toList();
    }

    @Transactional
    public ContractResponse create(Long employeeId, ContractCreateRequest request) {
        permissionChecker.require(Permission.HR_EMPLOYEE_WRITE);
        Employee employee = employeeRepository.findById(employeeId)
            .orElseThrow(() -> new ErpException(ErrorCode.EMPLOYEE_NOT_FOUND));
        Position position = positionRepository.findById(request.positionId())
            .orElseThrow(() -> new ErpException(ErrorCode.POSITION_NOT_FOUND));
        JobGrade jobGrade = null;
        if (request.jobGradeId() != null) {
            jobGrade = jobGradeRepository.findById(request.jobGradeId())
                .orElseThrow(() -> new ErpException(ErrorCode.JOB_GRADE_NOT_FOUND));
        }
        Contract contract = Contract.create(
            employee, request.contractType(), request.startDate(), request.endDate(),
            request.baseSalary(), position, jobGrade, request.note());
        return ContractResponse.from(contractRepository.save(contract));
    }
}
