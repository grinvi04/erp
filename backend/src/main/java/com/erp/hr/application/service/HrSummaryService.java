package com.erp.hr.application.service;

import com.erp.common.workflow.ApprovalStatus;
import com.erp.hr.application.dto.HrSummaryResponse;
import com.erp.hr.domain.model.EmployeeStatus;
import com.erp.hr.domain.repository.EmployeeRepository;
import com.erp.hr.domain.repository.LeaveRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HrSummaryService {

    private final EmployeeRepository employeeRepository;
    private final LeaveRequestRepository leaveRequestRepository;

    public HrSummaryResponse getSummary() {
        return new HrSummaryResponse(
                employeeRepository.countByStatus(EmployeeStatus.ACTIVE),
                employeeRepository.countByStatus(EmployeeStatus.ON_LEAVE),
                leaveRequestRepository.countByApprovalStatus(ApprovalStatus.PENDING));
    }
}
