package com.erp.hr.domain.model;

import com.erp.common.audit.BaseEntity;
import com.erp.common.workflow.ApprovalStatus;
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

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 휴가 신청 — 신청→결재→반영 흐름.
 * ApprovalRequest와 entity_type='LEAVE_REQUEST', entity_id=this.id로 연결.
 */
@Entity
@Table(name = "leave_request", schema = "hr")
public class LeaveRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "leave_request_seq")
    @SequenceGenerator(name = "leave_request_seq", sequenceName = "hr.leave_request_id_seq", allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leave_policy_id", nullable = false)
    private LeavePolicy leavePolicy;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "requested_days", nullable = false, precision = 5, scale = 1)
    private BigDecimal requestedDays;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 20)
    private ApprovalStatus approvalStatus;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "approval_request_id")
    private Long approvalRequestId;

    protected LeaveRequest() {}

    public static LeaveRequest create(Employee employee, LeavePolicy leavePolicy,
                                       LocalDate startDate, LocalDate endDate,
                                       BigDecimal requestedDays, String reason) {
        LeaveRequest req = new LeaveRequest();
        req.employee = employee;
        req.leavePolicy = leavePolicy;
        req.startDate = startDate;
        req.endDate = endDate;
        req.requestedDays = requestedDays;
        req.approvalStatus = ApprovalStatus.PENDING;
        req.reason = reason;
        return req;
    }

    public void linkApprovalRequest(Long approvalRequestId) {
        this.approvalRequestId = approvalRequestId;
    }

    public void approve() {
        this.approvalStatus = ApprovalStatus.APPROVED;
    }

    public void reject() {
        this.approvalStatus = ApprovalStatus.REJECTED;
    }

    public boolean isPending() { return approvalStatus == ApprovalStatus.PENDING; }
    public boolean isApproved() { return approvalStatus == ApprovalStatus.APPROVED; }

    public Long getId() { return id; }
    public Employee getEmployee() { return employee; }
    public LeavePolicy getLeavePolicy() { return leavePolicy; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public BigDecimal getRequestedDays() { return requestedDays; }
    public ApprovalStatus getApprovalStatus() { return approvalStatus; }
    public String getReason() { return reason; }
    public Long getApprovalRequestId() { return approvalRequestId; }
}
