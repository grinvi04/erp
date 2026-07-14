package com.erp.common.security;

public record TenantIdentityCreateRequest(
    Long tenantId, String email, String firstName, String lastName, String invitationKey) {}
