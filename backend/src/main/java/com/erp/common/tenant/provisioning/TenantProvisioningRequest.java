package com.erp.common.tenant.provisioning;

import com.erp.common.tenant.TenantPlan;

public record TenantProvisioningRequest(
    String code, String name, TenantPlan plan, String adminUserId, String performedBy) {}
