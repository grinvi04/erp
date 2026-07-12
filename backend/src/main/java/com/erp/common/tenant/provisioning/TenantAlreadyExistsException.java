package com.erp.common.tenant.provisioning;

public class TenantAlreadyExistsException extends RuntimeException {
  public TenantAlreadyExistsException(String code) {
    super("tenant already exists: " + code);
  }
}
