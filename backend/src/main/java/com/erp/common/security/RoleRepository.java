package com.erp.common.security;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 역할 저장소. authz 테이블은 @TenantId 비대상이므로 tenant_id를 명시 필터링한다. */
public interface RoleRepository extends JpaRepository<Role, Long> {

  List<Role> findByTenantIdOrderByCodeAsc(Long tenantId);

  // RoleResponse.from은 role.permissions(@OneToMany)를 읽는다 — 비페이지 List이므로 컬렉션 페치로
  // N+1을 제거한다(DISTINCT로 조인 중복 행 제거). 페이지네이션이 아니라서 컬렉션 페치가 안전하다.
  @Query(
      "SELECT DISTINCT r FROM Role r LEFT JOIN FETCH r.permissions "
          + "WHERE r.tenantId = :tenantId ORDER BY r.code ASC")
  List<Role> findByTenantIdWithPermissionsOrderByCodeAsc(@Param("tenantId") Long tenantId);

  Optional<Role> findByTenantIdAndId(Long tenantId, Long id);

  Optional<Role> findByTenantIdAndCode(Long tenantId, String code);

  boolean existsByTenantIdAndCode(Long tenantId, String code);
}
