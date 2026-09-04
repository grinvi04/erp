package com.erp.common.tenant.provisioning;

import com.erp.common.tenant.Tenant;

public interface TenantProvisioningStore {
  Tenant begin(TenantProvisioningRequest request);

  Tenant beginRetry(String code, String performedBy);

  Tenant activate(Long tenantId, String performedBy);

  void fail(Long tenantId, String error, String performedBy);
}
