package com.erp.common.security;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

    /**
     * 사용자의 모든 역할에 매핑된 권한 코드 집합 — 인가 해석의 핵심 쿼리.
     * tenant_id 명시 필터(인증 단계에서 TenantContext 없이 호출됨).
     */
    @Query("SELECT DISTINCT p FROM UserRole ur JOIN ur.role r JOIN r.permissions p "
        + "WHERE ur.tenantId = :tenantId AND ur.userId = :userId")
    Set<String> findPermissionCodes(@Param("tenantId") Long tenantId, @Param("userId") String userId);

    List<UserRole> findByTenantIdAndUserId(Long tenantId, String userId);

    Optional<UserRole> findByTenantIdAndUserIdAndRoleId(Long tenantId, String userId, Long roleId);

    boolean existsByTenantIdAndUserIdAndRoleId(Long tenantId, String userId, Long roleId);
}
