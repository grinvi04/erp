package com.erp.common.security;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * 사용자별 접근 프로파일 — 데이터 스코프(ALL/DEPARTMENT/SELF)·소속 부서·전결 한도(원). 기존 JWT
 * 클레임(data_scope·department_id·approval_limit)을 대체하는 DB 정본. tenant_id 명시 필터링({@link Role} 참조).
 */
@Entity
@Table(name = "user_access_profile", schema = "common")
public class UserAccessProfile {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_access_profile_seq")
  @SequenceGenerator(
      name = "user_access_profile_seq",
      sequenceName = "common.user_access_profile_id_seq",
      allocationSize = 50)
  private Long id;

  @Column(name = "tenant_id", nullable = false)
  private Long tenantId;

  @Column(name = "user_id", nullable = false, length = 100)
  private String userId;

  @Enumerated(EnumType.STRING)
  @Column(name = "data_scope", nullable = false, length = 20)
  private DataScope dataScope = DataScope.ALL;

  @Column(name = "department_id")
  private Long departmentId;

  @Column(name = "approval_limit")
  private BigDecimal approvalLimit;

  protected UserAccessProfile() {}

  public static UserAccessProfile of(
      Long tenantId,
      String userId,
      DataScope dataScope,
      Long departmentId,
      BigDecimal approvalLimit) {
    UserAccessProfile p = new UserAccessProfile();
    p.tenantId = tenantId;
    p.userId = userId;
    p.dataScope = dataScope != null ? dataScope : DataScope.ALL;
    p.departmentId = departmentId;
    p.approvalLimit = approvalLimit;
    return p;
  }

  public void update(DataScope dataScope, Long departmentId, BigDecimal approvalLimit) {
    this.dataScope = dataScope != null ? dataScope : DataScope.ALL;
    this.departmentId = departmentId;
    this.approvalLimit = approvalLimit;
  }

  public Long getId() {
    return id;
  }

  public Long getTenantId() {
    return tenantId;
  }

  public String getUserId() {
    return userId;
  }

  public DataScope getDataScope() {
    return dataScope;
  }

  public Long getDepartmentId() {
    return departmentId;
  }

  public BigDecimal getApprovalLimit() {
    return approvalLimit;
  }
}
