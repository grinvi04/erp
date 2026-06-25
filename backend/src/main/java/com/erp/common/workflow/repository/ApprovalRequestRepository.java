package com.erp.common.workflow.repository;

import com.erp.common.workflow.ApprovalRequest;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, Long> {
    List<ApprovalRequest> findByEntityTypeAndEntityId(String entityType, Long entityId);

    /**
     * 현재 결재 단계의 결재자가 주어진 사용자인 PENDING 결재 요청(내가 처리할 대기 건).
     * EXISTS로 필터링하고 @EntityGraph로 steps를 한 번에 로드 — DTO 매핑 시 N+1 방지.
     */
    @EntityGraph(attributePaths = "steps")
    @Query("SELECT a FROM ApprovalRequest a "
            + "WHERE a.status = com.erp.common.workflow.ApprovalStatus.PENDING "
            + "AND EXISTS (SELECT 1 FROM ApprovalStep s WHERE s.approvalRequest = a "
            + "AND s.stepOrder = a.currentStep AND s.approverId = :userId) "
            + "ORDER BY a.requestedAt DESC")
    List<ApprovalRequest> findPendingForApprover(@Param("userId") String userId);

    /** 내가 요청(상신)한 결재 요청 — 상태 추적용. steps를 함께 로드해 N+1 방지. */
    @EntityGraph(attributePaths = "steps")
    List<ApprovalRequest> findByRequesterIdOrderByRequestedAtDesc(String requesterId);
}
