package com.erp.common.security;

import java.util.List;
import java.util.Set;

public interface TenantUserOnboardingStore {

  TenantUser begin(String email, String requestKey, String requestFingerprint);

  TenantUser retry(String requestKey);

  TenantUser activate(String requestKey, String keycloakUserId, Set<Long> roleIds);

  TenantUser markFailed(String requestKey, String failureCode);

  TenantUser beginReinvite(Long id);

  TenantUser disable(Long id);

  TenantUser find(Long id);

  List<TenantUser> list();
}
