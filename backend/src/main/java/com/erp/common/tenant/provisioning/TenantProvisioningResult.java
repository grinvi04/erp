package com.erp.common.tenant.provisioning;

import com.erp.common.tenant.TenantStatus;

public record TenantProvisioningResult(
    Long tenantId, String code, TenantStatus status, String adminUserId) {}
