package com.erp.hr.domain.model;

import com.erp.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
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
 * 직원 마스터 — 인적사항 + 재직 정보. 급여는 기록 목적(Finance 연산 없음). 조직 구조: Employee → Department → Department(상위) →
 * ... → Company(최상위 Department)
 */
@Entity
@Table(name = "employee", schema = "hr")
public class Employee extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "employee_seq")
  @SequenceGenerator(
      name = "employee_seq",
      sequenceName = "hr.employee_id_seq",
      allocationSize = 50)
  private Long id;

  @Column(name = "employee_no", nullable = false, length = 30)
  private String employeeNo;

  @Embedded private PersonalInfo personalInfo;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "department_id", nullable = false)
  private Department department;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "position_id", nullable = false)
  private Position position;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "job_grade_id")
  private JobGrade jobGrade;

  @Column(name = "hire_date", nullable = false)
  private LocalDate hireDate;

  @Column(name = "termination_date")
  private LocalDate terminationDate;

  @Enumerated(EnumType.STRING)
  @Column(name = "employment_type", nullable = false, length = 20)
  private EmploymentType employmentType;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private EmployeeStatus status;

  @Column(name = "base_salary", precision = 15, scale = 2)
  private BigDecimal baseSalary;

  @Column(name = "work_email", nullable = false, length = 200)
  private String workEmail;

  // Keycloak subject(sub) — 로그인 계정 연결. 결재자 식별의 정본 신원.
  @Column(name = "user_id", length = 100)
  private String userId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "manager_id")
  private Employee manager;

  protected Employee() {}

  public static Employee create(
      String employeeNo,
      PersonalInfo personalInfo,
      Department department,
      Position position,
      JobGrade jobGrade,
      LocalDate hireDate,
      EmploymentType employmentType,
      String workEmail,
      BigDecimal baseSalary) {
    Employee emp = new Employee();
    emp.employeeNo = employeeNo;
    emp.personalInfo = personalInfo;
    emp.department = department;
    emp.position = position;
    emp.jobGrade = jobGrade;
    emp.hireDate = hireDate;
    emp.employmentType = employmentType;
    emp.status = EmployeeStatus.ACTIVE;
    emp.workEmail = workEmail;
    emp.baseSalary = baseSalary;
    return emp;
  }

  public void transfer(Department newDepartment, Position newPosition) {
    this.department = newDepartment;
    this.position = newPosition;
  }

  public void promote(Position newPosition, JobGrade newGrade, BigDecimal newSalary) {
    this.position = newPosition;
    this.jobGrade = newGrade;
    this.baseSalary = newSalary;
  }

  public void onLeave() {
    if (status != EmployeeStatus.ACTIVE) {
      throw new IllegalStateException("Only ACTIVE employee can go on leave: " + status);
    }
    this.status = EmployeeStatus.ON_LEAVE;
  }

  public void returnFromLeave() {
    if (status != EmployeeStatus.ON_LEAVE) {
      throw new IllegalStateException("Employee is not on leave: " + status);
    }
    this.status = EmployeeStatus.ACTIVE;
  }

  public void terminate(LocalDate terminationDate) {
    if (status == EmployeeStatus.TERMINATED) {
      throw new IllegalStateException("Already terminated");
    }
    this.status = EmployeeStatus.TERMINATED;
    this.terminationDate = terminationDate;
  }

  public void updateInfo(
      String lastName,
      String firstName,
      String phone,
      String personalEmail,
      String workEmail,
      java.math.BigDecimal baseSalary) {
    this.personalInfo =
        new PersonalInfo(
            lastName != null ? lastName : this.personalInfo.getLastName(),
            firstName != null ? firstName : this.personalInfo.getFirstName(),
            this.personalInfo.getDateOfBirth(),
            this.personalInfo.getGender(),
            this.personalInfo.getNationalId(),
            phone != null ? phone : this.personalInfo.getPhone(),
            personalEmail != null ? personalEmail : this.personalInfo.getPersonalEmail());
    if (workEmail != null) {
      this.workEmail = workEmail;
    }
    if (baseSalary != null) {
      this.baseSalary = baseSalary;
    }
  }

  public void assignManager(Employee manager) {
    this.manager = manager;
  }

  /** Keycloak 로그인 계정(sub)을 직원에 연결한다. null이면 연결 해제. */
  public void linkUserAccount(String userId) {
    this.userId = userId;
  }

  public boolean isActive() {
    return status == EmployeeStatus.ACTIVE;
  }

  public boolean isTerminated() {
    return status == EmployeeStatus.TERMINATED;
  }

  public Long getId() {
    return id;
  }

  public String getEmployeeNo() {
    return employeeNo;
  }

  public PersonalInfo getPersonalInfo() {
    return personalInfo;
  }

  public Department getDepartment() {
    return department;
  }

  public Position getPosition() {
    return position;
  }

  public JobGrade getJobGrade() {
    return jobGrade;
  }

  public LocalDate getHireDate() {
    return hireDate;
  }

  public LocalDate getTerminationDate() {
    return terminationDate;
  }

  public EmploymentType getEmploymentType() {
    return employmentType;
  }

  public EmployeeStatus getStatus() {
    return status;
  }

  public BigDecimal getBaseSalary() {
    return baseSalary;
  }

  public String getWorkEmail() {
    return workEmail;
  }

  public String getUserId() {
    return userId;
  }

  public Employee getManager() {
    return manager;
  }
}
