package com.erp.common.security;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccessProfileRepository extends JpaRepository<UserAccessProfile, Long> {

  Optional<UserAccessProfile> findByTenantIdAndUserId(Long tenantId, String userId);
}
