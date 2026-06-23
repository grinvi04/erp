package com.erp.hr.domain.repository;

import com.erp.common.workflow.ApprovalStatus;
import com.erp.hr.domain.model.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    List<LeaveRequest> findByEmployeeId(Long employeeId);
    List<LeaveRequest> findByEmployeeIdAndApprovalStatus(Long employeeId, ApprovalStatus status);
    List<LeaveRequest> findByApprovalStatus(ApprovalStatus status);
}
