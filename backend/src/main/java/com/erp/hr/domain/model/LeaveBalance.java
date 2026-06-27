package com.erp.hr.domain.model;

import com.erp.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/** 직원별 휴가 잔여 일수 — 연도별 관리. */
@Entity
@Table(name = "leave_balance", schema = "hr")
public class LeaveBalance extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "leave_balance_seq")
  @SequenceGenerator(
      name = "leave_balance_seq",
      sequenceName = "hr.leave_balance_id_seq",
      allocationSize = 50)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "employee_id", nullable = false)
  private Employee employee;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "leave_policy_id", nullable = false)
  private LeavePolicy leavePolicy;

  @Column(name = "year", nullable = false)
  private int year;

  @Column(name = "entitled_days", nullable = false, precision = 5, scale = 1)
  private BigDecimal entitledDays;

  @Column(name = "used_days", nullable = false, precision = 5, scale = 1)
  private BigDecimal usedDays;

  @Column(name = "carry_over_days", nullable = false, precision = 5, scale = 1)
  private BigDecimal carryOverDays;

  protected LeaveBalance() {}

  public static LeaveBalance create(
      Employee employee,
      LeavePolicy leavePolicy,
      int year,
      BigDecimal entitledDays,
      BigDecimal carryOverDays) {
    LeaveBalance lb = new LeaveBalance();
    lb.employee = employee;
    lb.leavePolicy = leavePolicy;
    lb.year = year;
    lb.entitledDays = entitledDays;
    lb.usedDays = BigDecimal.ZERO;
    lb.carryOverDays = carryOverDays;
    return lb;
  }

  public BigDecimal getRemainingDays() {
    return entitledDays.add(carryOverDays).subtract(usedDays);
  }

  public boolean hasSufficientBalance(BigDecimal requestedDays) {
    return getRemainingDays().compareTo(requestedDays) >= 0;
  }

  public void deduct(BigDecimal days) {
    if (!hasSufficientBalance(days)) {
      throw new IllegalStateException("Insufficient leave balance");
    }
    this.usedDays = this.usedDays.add(days);
  }

  public void restore(BigDecimal days) {
    this.usedDays = this.usedDays.subtract(days);
    if (this.usedDays.compareTo(BigDecimal.ZERO) < 0) {
      this.usedDays = BigDecimal.ZERO;
    }
  }

  public Long getId() {
    return id;
  }

  public Employee getEmployee() {
    return employee;
  }

  public LeavePolicy getLeavePolicy() {
    return leavePolicy;
  }

  public int getYear() {
    return year;
  }

  public BigDecimal getEntitledDays() {
    return entitledDays;
  }

  public BigDecimal getUsedDays() {
    return usedDays;
  }

  public BigDecimal getCarryOverDays() {
    return carryOverDays;
  }
}
