package com.erp.common.security;

import java.util.Optional;

public interface TenantIdentityAdminPort {

  Optional<TenantIdentityUser> findByEmail(String email);

  TenantIdentityUser createUser(TenantIdentityCreateRequest request);

  void sendInvite(String userId, Long tenantId);

  void setEnabled(String userId, Long tenantId, boolean enabled);
}
