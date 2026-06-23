package com.erp.hr.domain.model;

import com.erp.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

/**
 * 휴가 유형 정책 — 테넌트별 설정 가능 (연차, 병가, 경조사 등).
 */
@Entity
@Table(name = "leave_policy", schema = "hr")
public class LeavePolicy extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "leave_policy_seq")
    @SequenceGenerator(name = "leave_policy_seq", sequenceName = "hr.leave_policy_id_seq", allocationSize = 50)
    private Long id;

    @Column(name = "code", nullable = false, length = 30)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "leave_type", nullable = false, length = 30)
    private LeaveType leaveType;

    @Column(name = "annual_days", nullable = false)
    private int annualDays;

    @Column(name = "carry_over_days", nullable = false)
    private int carryOverDays;

    @Column(name = "requires_approval", nullable = false)
    private boolean requiresApproval;

    @Column(name = "min_notice_days", nullable = false)
    private int minNoticeDays;

    protected LeavePolicy() {}

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public LeaveType getLeaveType() { return leaveType; }
    public int getAnnualDays() { return annualDays; }
    public int getCarryOverDays() { return carryOverDays; }
    public boolean isRequiresApproval() { return requiresApproval; }
    public int getMinNoticeDays() { return minNoticeDays; }

    public enum LeaveType {
        ANNUAL,      // 연차
        SICK,        // 병가
        PARENTAL,    // 육아휴직
        BEREAVEMENT, // 경조사
        UNPAID,      // 무급
        COMPENSATORY // 보상 휴가
    }
}
