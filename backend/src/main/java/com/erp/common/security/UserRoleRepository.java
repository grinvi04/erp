package com.erp.common.security;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

  /**
   * 사용자의 모든 역할에 매핑된 권한 코드 집합 — 인가 해석의 핵심(권한 결정의 단일 지점). tenant_id 명시 필터(인증 단계에서 TenantContext 없이
   * 호출됨). 배정(user_role)과 역할(role) 양쪽의 tenant_id를 모두 검증한다 — 잘못 만들어진 배정이 타 테넌트 역할의 권한을 새지 못하도록 해석
   * 시점에서 차단(심층 방어). 배정 생성 API도 동일 테넌트를 강제한다.
   */
  @Query(
      "SELECT DISTINCT p.permissionCode FROM UserRole ur JOIN ur.role r JOIN r.permissions p "
          + "WHERE ur.tenantId = :tenantId AND ur.userId = :userId "
          + "AND r.tenantId = :tenantId AND p.tenantId = :tenantId")
  Set<String> findPermissionCodes(@Param("tenantId") Long tenantId, @Param("userId") String userId);

  List<UserRole> findByTenantIdAndUserId(Long tenantId, String userId);

  // getUserRoles는 RoleResponse.from을 통해 ur.role.permissions(@OneToMany)까지 읽는다 — 비페이지
  // List이므로 role과 그 permissions를 함께 페치해 N+1을 제거한다(DISTINCT로 조인 중복 행 제거).
  @Query(
      "SELECT DISTINCT ur FROM UserRole ur JOIN FETCH ur.role r LEFT JOIN FETCH r.permissions "
          + "WHERE ur.tenantId = :tenantId AND ur.userId = :userId")
  List<UserRole> findByTenantIdAndUserIdWithRolePermissions(
      @Param("tenantId") Long tenantId, @Param("userId") String userId);

  Optional<UserRole> findByTenantIdAndUserIdAndRoleId(Long tenantId, String userId, Long roleId);

  boolean existsByTenantIdAndUserIdAndRoleId(Long tenantId, String userId, Long roleId);
}
