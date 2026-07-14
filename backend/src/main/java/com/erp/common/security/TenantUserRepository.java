package com.erp.common.security;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantUserRepository extends JpaRepository<TenantUser, Long> {

  Optional<TenantUser> findByNormalizedEmail(String normalizedEmail);

  Optional<TenantUser> findByRequestKey(String requestKey);

  Optional<TenantUser> findByKeycloakUserId(String keycloakUserId);

  List<TenantUser> findAllByOrderByNormalizedEmailAsc();
}
