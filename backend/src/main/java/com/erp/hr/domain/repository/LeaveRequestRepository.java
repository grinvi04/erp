package com.erp.hr.domain.repository;

import com.erp.common.workflow.ApprovalStatus;
import com.erp.hr.domain.model.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    List<LeaveRequest> findByEmployeeId(Long employeeId);
    List<LeaveRequest> findByEmployeeIdAndApprovalStatus(Long employeeId, ApprovalStatus status);
    List<LeaveRequest> findByApprovalStatus(ApprovalStatus status);

    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.employee.id = :employeeId "
        + "AND lr.approvalStatus = :status "
        + "AND lr.startDate <= :endDate AND lr.endDate >= :startDate")
    List<LeaveRequest> findApprovedOverlapping(@Param("employeeId") Long employeeId,
                                               @Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate,
                                               @Param("status") ApprovalStatus status);

    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.employee.id = :employeeId "
        + "AND lr.approvalStatus IN :statuses "
        + "AND lr.startDate <= :endDate AND lr.endDate >= :startDate")
    List<LeaveRequest> findOverlappingByStatuses(@Param("employeeId") Long employeeId,
                                                 @Param("startDate") LocalDate startDate,
                                                 @Param("endDate") LocalDate endDate,
                                                 @Param("statuses") java.util.List<ApprovalStatus> statuses);
}
