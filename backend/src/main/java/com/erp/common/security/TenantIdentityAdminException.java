package com.erp.common.security;

public class TenantIdentityAdminException extends RuntimeException {

  public TenantIdentityAdminException(String message, Throwable cause) {
    super(message, cause);
  }

  public TenantIdentityAdminException(String message) {
    super(message);
  }
}
