package com.erp.common.tenant;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

@Component
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<Long> {

  // 0L = system/bootstrap context (Flyway, health-check 등 테넌트 없는 요청)
  private static final Long SYSTEM_TENANT = 0L;

  @Override
  public Long resolveCurrentTenantIdentifier() {
    Long tenantId = TenantContext.getTenantId();
    return tenantId != null ? tenantId : SYSTEM_TENANT;
  }

  @Override
  public boolean validateExistingCurrentSessions() {
    return false;
  }
}
