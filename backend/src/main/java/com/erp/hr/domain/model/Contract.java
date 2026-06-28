package com.erp.hr.domain.model;

import com.erp.common.audit.BaseEntity;
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
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "contract", schema = "hr")
@SQLRestriction("deleted_at IS NULL")
public class Contract extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "contract_seq")
  @SequenceGenerator(
      name = "contract_seq",
      sequenceName = "hr.contract_id_seq",
      allocationSize = 50)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "employee_id", nullable = false)
  private Employee employee;

  @Enumerated(EnumType.STRING)
  @Column(name = "contract_type", nullable = false, length = 20)
  private ContractType contractType;

  @Column(name = "start_date", nullable = false)
  private LocalDate startDate;

  @Column(name = "end_date")
  private LocalDate endDate;

  @Column(name = "base_salary", precision = 15, scale = 2)
  private BigDecimal baseSalary;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "position_id", nullable = false)
  private Position position;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "job_grade_id")
  private JobGrade jobGrade;

  @Column(name = "note", columnDefinition = "TEXT")
  private String note;

  protected Contract() {}

  public static Contract create(
      Employee employee,
      ContractType contractType,
      LocalDate startDate,
      LocalDate endDate,
      BigDecimal baseSalary,
      Position position,
      JobGrade jobGrade,
      String note) {
    Contract c = new Contract();
    c.employee = employee;
    c.contractType = contractType;
    c.startDate = startDate;
    c.endDate = endDate;
    c.baseSalary = baseSalary;
    c.position = position;
    c.jobGrade = jobGrade;
    c.note = note;
    return c;
  }

  public boolean isIndefinite() {
    return endDate == null;
  }

  public boolean isExpired(LocalDate asOf) {
    return endDate != null && endDate.isBefore(asOf);
  }

  public Long getId() {
    return id;
  }

  public Employee getEmployee() {
    return employee;
  }

  public ContractType getContractType() {
    return contractType;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public LocalDate getEndDate() {
    return endDate;
  }

  public BigDecimal getBaseSalary() {
    return baseSalary;
  }

  public Position getPosition() {
    return position;
  }

  public JobGrade getJobGrade() {
    return jobGrade;
  }

  public String getNote() {
    return note;
  }
}
