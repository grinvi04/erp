package com.erp.common.security;

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

/**
 * 역할 ↔ 권한코드 매핑 행. {@link Role}·{@link com.erp.common.audit.AuditLog}와 동일하게 {@code
 * BaseEntity}/@TenantId를 쓰지 않고 tenant_id를 명시 보유한다 — 권한 해석이 JWT 인증 단계(TenantContext 세팅 전)에서 일어나기 때문.
 * 부모 역할의 tenant_id를 그대로 들고 있어 권한 해석 쿼리가 DB 레벨에서 cross-tenant 격리를 검증할 수 있다(심층 방어).
 *
 * <p>반드시 부모 {@link Role}을 통해서만 다룬다(grant/revoke). RolePermission을 루트로 직접 쿼리하면 테넌트 격리 의도가 흐려진다.
 */
@Entity
@Table(name = "role_permission", schema = "common")
public class RolePermission {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "role_permission_seq")
  @SequenceGenerator(
      name = "role_permission_seq",
      sequenceName = "common.role_permission_id_seq",
      allocationSize = 50)
  private Long id;

  @Column(name = "tenant_id", nullable = false)
  private Long tenantId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "role_id", nullable = false)
  private Role role;

  @Column(name = "permission_code", nullable = false, length = 100)
  private String permissionCode;

  protected RolePermission() {}

  static RolePermission of(Long tenantId, Role role, String permissionCode) {
    RolePermission rp = new RolePermission();
    rp.tenantId = tenantId;
    rp.role = role;
    rp.permissionCode = permissionCode;
    return rp;
  }

  public String getPermissionCode() {
    return permissionCode;
  }
}
