package com.erp.common.security;

public record TenantIdentityUser(String id, Long tenantId, String invitationKey, boolean enabled) {}
