package com.erp.common.tenant.provisioning;

import com.erp.common.tenant.Tenant;

public class TenantProvisioningService {

  private final TenantProvisioningStore store;
  private final TenantIdentityProvisioningPort identityPort;

  public TenantProvisioningService(
      TenantProvisioningStore store, TenantIdentityProvisioningPort identityPort) {
    this.store = store;
    this.identityPort = identityPort;
  }

  public TenantProvisioningResult provision(TenantProvisioningRequest request) {
    return complete(store.begin(request), request.performedBy());
  }

  public TenantProvisioningResult retry(String code, String performedBy) {
    return complete(store.beginRetry(code, performedBy), performedBy);
  }

  private TenantProvisioningResult complete(Tenant tenant, String performedBy) {
    try {
      identityPort.assignTenant(tenant.getAdminUserId(), tenant.getId());
      return result(store.activate(tenant.getId(), performedBy));
    } catch (RuntimeException cause) {
      try {
        store.fail(tenant.getId(), "identity provider update failed", performedBy);
      } catch (RuntimeException persistenceFailure) {
        cause.addSuppressed(persistenceFailure);
      }
      throw new TenantProvisioningException("tenant provisioning failed", cause);
    }
  }

  private static TenantProvisioningResult result(Tenant tenant) {
    return new TenantProvisioningResult(
        tenant.getId(), tenant.getCode(), tenant.getStatus(), tenant.getAdminUserId());
  }
}
