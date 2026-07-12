package com.erp.common.tenant.provisioning;

public class TenantProvisioningNotFoundException extends RuntimeException {
  public TenantProvisioningNotFoundException(String identity) {
    super("tenant not found: " + identity);
  }
}
