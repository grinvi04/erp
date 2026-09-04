package com.erp.common.security.dto;

import com.erp.common.security.TenantUser;
import com.erp.common.security.TenantUserStatus;

public record TenantUserResponse(
    Long id, String email, String userId, TenantUserStatus status, String failureCode) {

  public static TenantUserResponse from(TenantUser user) {
    return new TenantUserResponse(
        user.getId(),
        user.getNormalizedEmail(),
        user.getKeycloakUserId(),
        user.getStatus(),
        user.getFailureCode());
  }
}
