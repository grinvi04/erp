package com.erp.common.tenant.provisioning;

public class TenantIdentityConflictException extends RuntimeException {
  public TenantIdentityConflictException(String message) {
    super(message);
  }
}
