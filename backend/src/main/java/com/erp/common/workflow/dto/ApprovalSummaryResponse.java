package com.erp.common.workflow.dto;

import com.erp.common.workflow.ApprovalRequest;
import com.erp.common.workflow.ApprovalStatus;
import com.erp.common.workflow.ApprovalStep;
import java.time.LocalDateTime;

/**
 * 결재함 목록용 요약 DTO. 도메인 엔티티(entityType+entityId)는 모듈 경계를 넘지 않고 참조만 전달한다 — 프론트가 entityType으로 해당 모듈
 * 화면으로 라우팅한다.
 */
public record ApprovalSummaryResponse(
    Long id,
    String entityType,
    Long entityId,
    String title,
    ApprovalStatus status,
    String requesterId,
    int currentStep,
    int totalSteps,
    String currentStepName,
    String currentApproverId,
    LocalDateTime requestedAt,
    LocalDateTime completedAt) {
  public static ApprovalSummaryResponse from(ApprovalRequest a) {
    ApprovalStep current =
        a.getSteps().stream()
            .filter(s -> s.getStepOrder() == a.getCurrentStep())
            .findFirst()
            .orElse(null);
    return new ApprovalSummaryResponse(
        a.getId(),
        a.getEntityType(),
        a.getEntityId(),
        a.getTitle(),
        a.getStatus(),
        a.getRequesterId(),
        a.getCurrentStep(),
        a.getTotalSteps(),
        current != null ? current.getStepName() : null,
        current != null ? current.getApproverId() : null,
        a.getRequestedAt(),
        a.getCompletedAt());
  }
}
