package com.erp.common.workflow;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 결재 단계. BaseEntity를 상속하지 않아 @TenantId·소프트삭제 필터가 없다 —
 * 반드시 부모(ApprovalRequest, 테넌트 필터됨)를 통해서만 조회할 것.
 * ApprovalStep을 루트로 직접 쿼리하면 테넌트 격리가 깨진다.
 */
@Entity
@Table(name = "approval_step", schema = "common")
public class ApprovalStep {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "approval_step_seq")
    @SequenceGenerator(name = "approval_step_seq", sequenceName = "common.approval_step_id_seq", allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approval_request_id", nullable = false)
    private ApprovalRequest approvalRequest;

    @Column(name = "step_order", nullable = false)
    private int stepOrder;

    @Column(name = "step_name", nullable = false, length = 100)
    private String stepName;

    @Column(name = "approver_id", nullable = false, length = 100)
    private String approverId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ApprovalStatus status;

    @Column(name = "comment", length = 1000)
    private String comment;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    protected ApprovalStep() {}

    public static ApprovalStep of(int stepOrder, String stepName, String approverId) {
        ApprovalStep step = new ApprovalStep();
        step.stepOrder = stepOrder;
        step.stepName = stepName;
        step.approverId = approverId;
        step.status = ApprovalStatus.PENDING;
        return step;
    }

    void approve(String approverId, String comment) {
        this.status = ApprovalStatus.APPROVED;
        this.comment = comment;
        this.processedAt = LocalDateTime.now();
    }

    void reject(String approverId, String comment) {
        this.status = ApprovalStatus.REJECTED;
        this.comment = comment;
        this.processedAt = LocalDateTime.now();
    }

    void cancel(String approverId, String comment) {
        this.status = ApprovalStatus.CANCELLED;
        this.comment = comment;
        this.processedAt = LocalDateTime.now();
    }

    void setApprovalRequest(ApprovalRequest request) {
        this.approvalRequest = request;
    }

    public Long getId() { return id; }
    public int getStepOrder() { return stepOrder; }
    public String getStepName() { return stepName; }
    public String getApproverId() { return approverId; }
    public ApprovalStatus getStatus() { return status; }
    public String getComment() { return comment; }
    public LocalDateTime getProcessedAt() { return processedAt; }
}
