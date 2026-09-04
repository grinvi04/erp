package com.erp.common.security;

import java.util.Optional;

final class UnavailableTenantIdentityAdminPort implements TenantIdentityAdminPort {

  @Override
  public Optional<TenantIdentityUser> findByEmail(String email) {
    throw unavailable();
  }

  @Override
  public TenantIdentityUser createUser(TenantIdentityCreateRequest request) {
    throw unavailable();
  }

  @Override
  public void sendInvite(String userId, Long tenantId) {
    throw unavailable();
  }

  @Override
  public void setEnabled(String userId, Long tenantId, boolean enabled) {
    throw unavailable();
  }

  private TenantIdentityAdminException unavailable() {
    return new TenantIdentityAdminException("tenant identity administration is disabled");
  }
}
