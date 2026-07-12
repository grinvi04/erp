package com.erp.common.tenant.provisioning;

public interface TenantIdentityProvisioningPort {
  void assignTenant(String userId, Long tenantId);
}
