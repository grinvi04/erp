package com.erp.common.workflow;

import com.erp.common.audit.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.SQLRestriction;

/**
 * 제네릭 결재 요청 — HR 휴가, Finance AP 전표, 구매 발주 등 모든 결재 흐름에 사용. 결재 대상 도메인은 entity_type + entity_id로 참조
 * (직접 FK 없음 — 모듈 경계 유지).
 */
@Entity
@Table(name = "approval_request", schema = "common")
@SQLRestriction("deleted_at IS NULL")
public class ApprovalRequest extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "approval_request_seq")
  @SequenceGenerator(
      name = "approval_request_seq",
      sequenceName = "common.approval_request_id_seq",
      allocationSize = 50)
  private Long id;

  @Column(name = "entity_type", nullable = false, length = 100)
  private String entityType;

  @Column(name = "entity_id", nullable = false)
  private Long entityId;

  @Column(name = "title", nullable = false, length = 200)
  private String title;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private ApprovalStatus status;

  @Column(name = "requester_id", nullable = false, length = 100)
  private String requesterId;

  @Column(name = "requested_at", nullable = false)
  private LocalDateTime requestedAt;

  @Column(name = "completed_at")
  private LocalDateTime completedAt;

  @Column(name = "current_step", nullable = false)
  private int currentStep;

  @Column(name = "total_steps", nullable = false)
  private int totalSteps;

  @OneToMany(
      mappedBy = "approvalRequest",
      cascade = CascadeType.ALL,
      fetch = FetchType.LAZY,
      orphanRemoval = true)
  @OrderBy("stepOrder ASC")
  private List<ApprovalStep> steps = new ArrayList<>();

  protected ApprovalRequest() {}

  public static ApprovalRequest create(
      String entityType,
      Long entityId,
      String title,
      String requesterId,
      List<ApprovalStep> steps) {
    ApprovalRequest req = new ApprovalRequest();
    req.entityType = entityType;
    req.entityId = entityId;
    req.title = title;
    req.status = ApprovalStatus.PENDING;
    req.requesterId = requesterId;
    req.requestedAt = LocalDateTime.now();
    req.currentStep = 1;
    req.totalSteps = steps.size();
    req.steps = steps;
    steps.forEach(s -> s.setApprovalRequest(req));
    return req;
  }

  public void approve(String approverId, String comment) {
    ApprovalStep step = getCurrentStepEntity();
    step.approve(approverId, comment);
    if (currentStep >= totalSteps) {
      this.status = ApprovalStatus.APPROVED;
      this.completedAt = LocalDateTime.now();
    } else {
      this.currentStep++;
    }
  }

  public void reject(String approverId, String comment) {
    getCurrentStepEntity().reject(approverId, comment);
    this.status = ApprovalStatus.REJECTED;
    this.completedAt = LocalDateTime.now();
  }

  /** 상신자 철회 — PENDING 결재를 CANCELLED로 종료한다(현재 단계도 취소 처리). */
  public void cancel(String requesterId, String comment) {
    getCurrentStepEntity().cancel(requesterId, comment);
    this.status = ApprovalStatus.CANCELLED;
    this.completedAt = LocalDateTime.now();
  }

  private ApprovalStep getCurrentStepEntity() {
    return steps.stream()
        .filter(s -> s.getStepOrder() == currentStep)
        .findFirst()
        .orElseThrow(
            () -> new IllegalStateException("Current approval step not found: " + currentStep));
  }

  public boolean isPending() {
    return status == ApprovalStatus.PENDING;
  }

  public String getCurrentStepApproverId() {
    return steps.stream()
        .filter(s -> s.getStepOrder() == currentStep)
        .findFirst()
        .map(ApprovalStep::getApproverId)
        .orElseThrow(
            () -> new IllegalStateException("Current approval step not found: " + currentStep));
  }

  public Long getId() {
    return id;
  }

  public String getEntityType() {
    return entityType;
  }

  public Long getEntityId() {
    return entityId;
  }

  public String getTitle() {
    return title;
  }

  public ApprovalStatus getStatus() {
    return status;
  }

  public String getRequesterId() {
    return requesterId;
  }

  public LocalDateTime getRequestedAt() {
    return requestedAt;
  }

  public LocalDateTime getCompletedAt() {
    return completedAt;
  }

  public int getCurrentStep() {
    return currentStep;
  }

  public int getTotalSteps() {
    return totalSteps;
  }

  public List<ApprovalStep> getSteps() {
    return steps;
  }
}
