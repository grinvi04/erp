package com.erp.hr.domain.repository;

import com.erp.common.workflow.ApprovalStatus;
import com.erp.hr.domain.model.LeaveRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long>,
        JpaSpecificationExecutor<LeaveRequest> {
    Page<LeaveRequest> findByEmployeeId(Long employeeId, Pageable pageable);
    List<LeaveRequest> findByEmployeeIdAndApprovalStatus(Long employeeId, ApprovalStatus status);
    List<LeaveRequest> findByApprovalStatus(ApprovalStatus status);
    long countByApprovalStatus(ApprovalStatus status);

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

    // 휴가유형별 APPROVED 신청 건수·합계일수 — 스코프는 직원(lr.employee) 경로로 결합. 빈 유형은 서비스에서 enum 0채움.
    @Query("SELECT lr.leavePolicy.leaveType AS leaveType, COUNT(lr.id) AS count, "
            + "COALESCE(SUM(lr.requestedDays), 0) AS totalDays FROM LeaveRequest lr "
            + "WHERE lr.approvalStatus = com.erp.common.workflow.ApprovalStatus.APPROVED "
            + "AND (:unscoped = true OR lr.employee.userId = :selfUserId OR lr.employee.department.id IN :deptIds) "
            + "GROUP BY lr.leavePolicy.leaveType")
    List<LeaveTypeStatRow> leaveStatsByType(@Param("unscoped") boolean unscoped,
                                            @Param("selfUserId") String selfUserId,
                                            @Param("deptIds") java.util.Collection<Long> deptIds);
}
