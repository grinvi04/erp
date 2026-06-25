package com.erp.common.security;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 역할 저장소. authz 테이블은 @TenantId 비대상이므로 tenant_id를 명시 필터링한다.
 */
public interface RoleRepository extends JpaRepository<Role, Long> {

    List<Role> findByTenantIdOrderByCodeAsc(Long tenantId);

    Optional<Role> findByTenantIdAndId(Long tenantId, Long id);

    Optional<Role> findByTenantIdAndCode(Long tenantId, String code);

    boolean existsByTenantIdAndCode(Long tenantId, String code);
}
