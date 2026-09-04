package com.erp.common.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.erp.common.AbstractIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

class TenantRepositoryIntegrationTest extends AbstractIntegrationTest {

  @Autowired private TenantRepository tenantRepository;
  @Autowired private JdbcTemplate jdbcTemplate;

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
    assertThat(saved.getVersion()).isZero();

    saved.activate();
    saved = tenantRepository.saveAndFlush(saved);
    assertThat(saved.getVersion()).isEqualTo(1L);

    assertThatThrownBy(
            () ->
                tenantRepository.saveAndFlush(
                    Tenant.startProvisioning(code, "중복 고객사", TenantPlan.TRIAL, "kc-user-2")))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void deleteSoftDeletesTenantAndExcludesItFromRepositoryQueries() {
    String code = "T" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    Tenant tenant =
        tenantRepository.saveAndFlush(
            Tenant.startProvisioning(code, "삭제 고객사", TenantPlan.TRIAL, "kc-user-delete"));

    tenantRepository.delete(tenant);
    tenantRepository.flush();

    assertThat(tenantRepository.findById(tenant.getId())).isEmpty();
    assertThat(tenantRepository.findAll()).extracting(Tenant::getId).doesNotContain(tenant.getId());
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM common.tenant WHERE id = ?", Long.class, tenant.getId()))
        .isEqualTo(1L);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT deleted_at IS NOT NULL FROM common.tenant WHERE id = ?",
                Boolean.class,
                tenant.getId()))
        .isTrue();
  }

  @Test
  void softDeletedTenantCodeCanBeReused() {
    String code = "T" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    Tenant deleted =
        tenantRepository.saveAndFlush(
            Tenant.startProvisioning(code, "삭제 고객사", TenantPlan.TRIAL, "kc-user-deleted"));

    tenantRepository.delete(deleted);
    tenantRepository.flush();

    Tenant replacement =
        tenantRepository.saveAndFlush(
            Tenant.startProvisioning(code, "대체 고객사", TenantPlan.STANDARD, "kc-user-new"));

    assertThat(replacement.getId()).isNotEqualTo(deleted.getId());
  }
}
