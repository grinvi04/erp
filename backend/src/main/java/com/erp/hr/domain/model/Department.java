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
import org.hibernate.annotations.SQLRestriction;

/** 조직 단위 — 트리 구조 (parent_id 자기 참조). null parent = 최상위 법인(Company). */
@Entity
@Table(name = "department", schema = "hr")
@SQLRestriction("deleted_at IS NULL")
public class Department extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "department_seq")
  @SequenceGenerator(
      name = "department_seq",
      sequenceName = "hr.department_id_seq",
      allocationSize = 50)
  private Long id;

  @Column(name = "code", nullable = false, length = 30)
  private String code;

  @Column(name = "name", nullable = false, length = 100)
  private String name;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_id")
  private Department parent;

  @Column(name = "depth", nullable = false)
  private int depth;

  @Column(name = "sort_order", nullable = false)
  private int sortOrder;

  @Column(name = "head_employee_id")
  private Long headEmployeeId;

  @Column(name = "is_active", nullable = false)
  private boolean active;

  protected Department() {}

  public static Department createRoot(String code, String name) {
    Department dept = new Department();
    dept.code = code;
    dept.name = name;
    dept.parent = null;
    dept.depth = 0;
    dept.sortOrder = 0;
    dept.active = true;
    return dept;
  }

  public static Department createChild(String code, String name, Department parent, int sortOrder) {
    Department dept = new Department();
    dept.code = code;
    dept.name = name;
    dept.parent = parent;
    dept.depth = parent.depth + 1;
    dept.sortOrder = sortOrder;
    dept.active = true;
    return dept;
  }

  public void rename(String newName) {
    this.name = newName;
  }

  public void assignHead(Long employeeId) {
    this.headEmployeeId = employeeId;
  }

  public void deactivate() {
    this.active = false;
  }

  public void activate() {
    this.active = true;
  }

  /** 상위 부서를 변경하고 depth를 재계산한다. null이면 최상위로 승격. 순환 검증은 호출 측(서비스)이 수행. */
  public void changeParent(Department newParent) {
    this.parent = newParent;
    refreshDepthFromParent();
  }

  /** 현재 parent 기준으로 depth를 재계산한다. 상위 이동 후 하위 트리 depth 보정에 사용. */
  public void refreshDepthFromParent() {
    this.depth = (parent == null) ? 0 : parent.depth + 1;
  }

  public boolean isRoot() {
    return parent == null;
  }

  public Long getId() {
    return id;
  }

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public Department getParent() {
    return parent;
  }

  public int getDepth() {
    return depth;
  }

  public int getSortOrder() {
    return sortOrder;
  }

  public Long getHeadEmployeeId() {
    return headEmployeeId;
  }

  public boolean isActive() {
    return active;
  }
}
