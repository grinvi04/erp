package com.erp.hr.application.service;

import com.erp.common.workflow.ApprovalStatus;
import com.erp.hr.application.dto.HrSummaryResponse;
import com.erp.hr.domain.model.EmployeeStatus;
import com.erp.hr.domain.repository.EmployeeRepository;
import com.erp.hr.domain.repository.LeaveRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class HrSummaryServiceTest {

    @Mock private EmployeeRepository employeeRepository;
    @Mock private LeaveRequestRepository leaveRequestRepository;
    @InjectMocks private HrSummaryService hrSummaryService;

    @Test
    void getSummary_aggregatesCounts() {
        given(employeeRepository.countByStatus(EmployeeStatus.ACTIVE)).willReturn(42L);
        given(employeeRepository.countByStatus(EmployeeStatus.ON_LEAVE)).willReturn(3L);
        given(leaveRequestRepository.countByApprovalStatus(ApprovalStatus.PENDING)).willReturn(7L);

        HrSummaryResponse result = hrSummaryService.getSummary();

        assertThat(result.activeEmployees()).isEqualTo(42L);
        assertThat(result.onLeaveEmployees()).isEqualTo(3L);
        assertThat(result.pendingLeaveRequests()).isEqualTo(7L);
    }
}
