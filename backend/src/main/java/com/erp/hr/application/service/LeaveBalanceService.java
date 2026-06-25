package com.erp.hr.application.service;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.security.Permission;
import com.erp.common.security.PermissionChecker;
import com.erp.hr.application.dto.LeaveBalanceResponse;
import com.erp.hr.domain.model.Employee;
import com.erp.hr.domain.repository.EmployeeRepository;
import com.erp.hr.domain.repository.LeaveBalanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LeaveBalanceService {

    private final LeaveBalanceRepository leaveBalanceRepository;
    private final EmployeeRepository employeeRepository;
    private final PermissionChecker permissionChecker;
    private final HrDataScopeResolver dataScopeResolver;

    public List<LeaveBalanceResponse> findByEmployeeAndYear(Long employeeId, int year) {
        permissionChecker.require(Permission.HR_LEAVE_READ);
        Employee employee = employeeRepository.findById(employeeId)
            .orElseThrow(() -> new ErpException(ErrorCode.EMPLOYEE_NOT_FOUND));
        if (!dataScopeResolver.isEmployeeInScope(employee)) {
            throw new ErpException(ErrorCode.FORBIDDEN);
        }
        return leaveBalanceRepository.findByEmployeeIdAndYear(employeeId, year).stream()
            .map(LeaveBalanceResponse::from)
            .toList();
    }
}
