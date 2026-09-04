package com.erp.common.tenant.provisioning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.erp.common.AbstractIntegrationTest;
import com.erp.common.audit.AuditLog;
import com.erp.common.audit.AuditLogRepository;
import com.erp.common.security.Permission;
import com.erp.common.security.RoleRepository;
import com.erp.common.security.UserRoleRepository;
import com.erp.common.tenant.Tenant;
import com.erp.common.tenant.TenantPlan;
import com.erp.common.tenant.TenantRepository;
import com.erp.common.tenant.TenantStatus;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class JpaTenantProvisioningStoreIntegrationTest extends AbstractIntegrationTest {

  @Autowired private TenantProvisioningStore store;
  @Autowired private TenantRepository tenantRepository;
  @Autowired private RoleRepository roleRepository;
  @Autowired private UserRoleRepository userRoleRepository;
  @Autowired private AuditLogRepository auditLogRepository;

  @Test
  void beginAndActivateCreatesSuperAdminAndAuditAtomically() {
    String code = uniqueCode();
    TenantProvisioningRequest request =
        new TenantProvisioningRequest(
            code, "유료 고객사", TenantPlan.STANDARD, "kc-admin-1", "ops-user");

    Tenant provisioning = store.begin(request);
    assertThat(provisioning.getStatus()).isEqualTo(TenantStatus.PROVISIONING);

    Tenant active = store.activate(provisioning.getId(), "ops-user");

    assertThat(active.getStatus()).isEqualTo(TenantStatus.ACTIVE);
    var role = roleRepository.findByTenantIdAndCode(active.getId(), "SUPER_ADMIN").orElseThrow();
    assertThat(role.getPermissions()).containsExactlyInAnyOrderElementsOf(Permission.all());
    assertThat(
            roleRepository
                .findByTenantIdAndCode(active.getId(), "BUSINESS_ADMIN")
                .orElseThrow()
                .getPermissions())
        .containsExactlyInAnyOrder(
            Permission.AUDIT_READ, Permission.IAM_READ, Permission.IAM_DELEGATE);
    assertThat(
            roleRepository
                .findByTenantIdAndCode(active.getId(), "FINANCE_USER")
                .orElseThrow()
                .getPermissions())
        .containsExactlyInAnyOrder(Permission.FINANCE_READ, Permission.FINANCE_WRITE);
    assertThat(
            roleRepository
                .findByTenantIdAndCode(active.getId(), "INVENTORY_USER")
                .orElseThrow()
                .getPermissions())
        .containsExactlyInAnyOrder(Permission.INVENTORY_READ, Permission.INVENTORY_WRITE);
    assertThat(
            roleRepository
                .findByTenantIdAndCode(active.getId(), "CRM_USER")
                .orElseThrow()
                .getPermissions())
        .containsExactlyInAnyOrder(Permission.CRM_READ, Permission.CRM_WRITE);
    assertThat(
            userRoleRepository.existsByTenantIdAndUserIdAndRoleId(
                active.getId(), "kc-admin-1", role.getId()))
        .isTrue();
    assertThat(
            auditLogRepository.findAll().stream()
                .filter(log -> active.getId().equals(log.getTenantId()))
                .filter(log -> "TENANT".equals(log.getEntityType()))
                .map(AuditLog::getAction))
        .contains(AuditLog.AuditAction.CREATE, AuditLog.AuditAction.UPDATE);
  }

  @Test
  void duplicateCodeIsRejectedCaseInsensitively() {
    String code = uniqueCode();
    store.begin(request(code));

    assertThatThrownBy(() -> store.begin(request(code.toLowerCase())))
        .isInstanceOf(TenantAlreadyExistsException.class);
  }

  @Test
  void failedTenantCanBeRetriedWithoutCreatingAnotherTenant() {
    String code = uniqueCode();
    Tenant tenant = store.begin(request(code));
    store.fail(tenant.getId(), "identity provider update failed", "ops-user");

    Tenant failed = tenantRepository.findById(tenant.getId()).orElseThrow();
    assertThat(failed.getStatus()).isEqualTo(TenantStatus.FAILED);
    assertThat(failed.getProvisioningError()).isEqualTo("identity provider update failed");

    Tenant retrying = store.beginRetry(code.toLowerCase(), "ops-user");
    assertThat(retrying.getId()).isEqualTo(tenant.getId());
    assertThat(retrying.getStatus()).isEqualTo(TenantStatus.PROVISIONING);
    assertThat(tenantRepository.count()).isGreaterThanOrEqualTo(1L);
  }

  private static TenantProvisioningRequest request(String code) {
    return new TenantProvisioningRequest(
        code, "유료 고객사", TenantPlan.STANDARD, "kc-admin-1", "ops-user");
  }

  private static String uniqueCode() {
    return "T" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
  }
}
