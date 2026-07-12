package com.erp.common.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.erp.common.AbstractIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

class TenantRepositoryIntegrationTest extends AbstractIntegrationTest {

  @Autowired private TenantRepository tenantRepository;

  @Test
  void provisioningTenantPersistsStateAndCodeIsGloballyUnique() {
    String code = "T" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    Tenant first =
        tenantRepository.saveAndFlush(
            Tenant.startProvisioning(code, "테스트 고객사", TenantPlan.STANDARD, "kc-user-1"));

    Tenant saved = tenantRepository.findByCode(code).orElseThrow();
    assertThat(saved.getId()).isEqualTo(first.getId());
    assertThat(saved.getStatus()).isEqualTo(TenantStatus.PROVISIONING);
    assertThat(saved.getAdminUserId()).isEqualTo("kc-user-1");
    assertThat(saved.getProvisioningAttemptedAt()).isNotNull();

    assertThatThrownBy(
            () ->
                tenantRepository.saveAndFlush(
                    Tenant.startProvisioning(code, "중복 고객사", TenantPlan.TRIAL, "kc-user-2")))
        .isInstanceOf(DataIntegrityViolationException.class);
  }
}
