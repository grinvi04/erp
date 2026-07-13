package com.erp.common.tenant;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TenantRepository extends JpaRepository<Tenant, Long> {
  boolean existsByCode(String code);

  @Query("SELECT t FROM Tenant t WHERE UPPER(t.code) = UPPER(:code)")
  Optional<Tenant> findByCode(@Param("code") String code);

  boolean existsByIdAndStatus(Long id, TenantStatus status);
}
