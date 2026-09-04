package com.erp.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.erp.common.AbstractIntegrationTest;
import com.erp.common.audit.AuditLog;
import com.erp.common.audit.AuditLogRepository;
import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.observability.TraceIdFilter;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

class JpaTenantUserOnboardingStoreIntegrationTest extends AbstractIntegrationTest {

  private static final String TRACE_ID = "0123456789abcdef0123456789abcdef";

  @Autowired private TenantUserOnboardingStore store;
  @Autowired private TenantUserRepository tenantUserRepository;
  @Autowired private RoleRepository roleRepository;
  @Autowired private UserRoleRepository userRoleRepository;
  @Autowired private AuditLogRepository auditLogRepository;

  @BeforeEach
  void authenticateOperator() {
    authenticate("operator", Permission.IAM_READ, Permission.IAM_WRITE);
    MDC.put(TraceIdFilter.MDC_TRACE_ID, TRACE_ID);
  }

  @AfterEach
  void clearTraceId() {
    MDC.remove(TraceIdFilter.MDC_TRACE_ID);
  }

  @Test
  void begin_rejectsInProgressDuplicateAndIsIdempotentAfterCompletion() {
    String email = uniqueEmail();
    String requestKey = uniqueKey();

    TenantUser first = store.begin(email, requestKey, fingerprint("payload-1"));
    assertThatThrownBy(() -> store.begin(email.toUpperCase(), requestKey, fingerprint("payload-1")))
        .isInstanceOfSatisfying(
            ErpException.class,
            exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.IDENTITY_CONFLICT));

    TenantUser active = store.activate(requestKey, "keycloak-" + suffix(), Set.of());
    TenantUser repeated = store.begin(email.toUpperCase(), requestKey, fingerprint("payload-1"));

    assertThat(active.getId()).isEqualTo(first.getId());
    assertThat(repeated.getId()).isEqualTo(active.getId());
    assertThat(repeated.getStatus()).isEqualTo(TenantUserStatus.ACTIVE);
    assertThatThrownBy(() -> store.begin(uniqueEmail(), requestKey, fingerprint("payload-1")))
        .isInstanceOfSatisfying(
            ErpException.class,
            exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.IDENTITY_CONFLICT));
    assertThatThrownBy(() -> store.begin(email, uniqueKey(), fingerprint("payload-1")))
        .isInstanceOfSatisfying(
            ErpException.class,
            exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.IDENTITY_CONFLICT));
    assertThatThrownBy(() -> store.begin(email, requestKey, fingerprint("changed-payload")))
        .isInstanceOfSatisfying(
            ErpException.class,
            exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.IDENTITY_CONFLICT));
  }

  @Test
  void activate_assignsAllRolesAtomicallyAndDisableRevokesThem() {
    String requestKey = uniqueKey();
    TenantUser pending = store.begin(uniqueEmail(), requestKey, fingerprint("roles"));
    Role finance = role("FIN_" + suffix(), Permission.FINANCE_READ);
    Role crm = role("CRM_" + suffix(), Permission.CRM_READ);

    TenantUser active =
        store.activate(requestKey, "keycloak-" + suffix(), Set.of(finance.getId(), crm.getId()));

    assertThat(active.getStatus()).isEqualTo(TenantUserStatus.ACTIVE);
    assertThat(
            userRoleRepository.findByTenantIdAndUserId(TEST_TENANT_ID, active.getKeycloakUserId()))
        .hasSize(2);

    TenantUser disabled = store.disable(pending.getId());

    assertThat(disabled.getStatus()).isEqualTo(TenantUserStatus.DISABLED);
    assertThat(
            userRoleRepository.findByTenantIdAndUserId(TEST_TENANT_ID, active.getKeycloakUserId()))
        .isEmpty();

    var statusChanges =
        auditLogRepository
            .search(
                TEST_TENANT_ID,
                "TENANT_USER",
                pending.getId(),
                null,
                AuditLog.AuditAction.UPDATE,
                null,
                null,
                PageRequest.of(0, 10))
            .getContent();
    assertThat(statusChanges)
        .hasSize(2)
        .allSatisfy(
            log -> {
              assertThat(log.getBeforeData()).contains("status", "identityLinked");
              assertThat(log.getAfterData()).contains("status", "identityLinked", "event");
              assertThat(log.getTraceId()).isEqualTo(TRACE_ID);
            });
    assertThat(statusChanges)
        .anySatisfy(
            log -> {
              assertThat(log.getBeforeData()).contains("PENDING");
              assertThat(log.getAfterData()).contains("ACTIVE");
            })
        .anySatisfy(
            log -> {
              assertThat(log.getBeforeData()).contains("ACTIVE");
              assertThat(log.getAfterData()).contains("DISABLED");
            });
  }

  @Test
  void disable_withMutationPermissionOnly_revokesRolesAndUpdatesStatus() {
    String requestKey = uniqueKey();
    TenantUser pending = store.begin(uniqueEmail(), requestKey, fingerprint("write-only-disable"));
    Role finance = role("FIN_" + suffix(), Permission.FINANCE_READ);
    TenantUser active = store.activate(requestKey, "keycloak-" + suffix(), Set.of(finance.getId()));
    authenticate("writer", Permission.IAM_WRITE);

    TenantUser disabled = store.disable(pending.getId());

    assertThat(disabled.getStatus()).isEqualTo(TenantUserStatus.DISABLED);
    assertThat(
            userRoleRepository.findByTenantIdAndUserId(TEST_TENANT_ID, active.getKeycloakUserId()))
        .isEmpty();
  }

  @Test
  void failedInvitation_isDurableAndRetryable() {
    String requestKey = uniqueKey();
    store.begin(uniqueEmail(), requestKey, fingerprint("retry"));

    TenantUser failed = store.markFailed(requestKey, "IDENTITY_PROVIDER_UNAVAILABLE");
    assertThat(failed.getStatus()).isEqualTo(TenantUserStatus.FAILED);

    TenantUser pending = store.retry(requestKey);

    assertThat(pending.getStatus()).isEqualTo(TenantUserStatus.PENDING);
    assertThat(tenantUserRepository.findByRequestKey(requestKey)).isPresent();
    assertThatThrownBy(() -> store.retry(requestKey))
        .isInstanceOfSatisfying(
            ErpException.class,
            exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.IDENTITY_CONFLICT));
  }

  @Test
  void markFailed_doesNotDowngradeCompletedInvitation() {
    String requestKey = uniqueKey();
    store.begin(uniqueEmail(), requestKey, fingerprint("completed"));
    TenantUser active = store.activate(requestKey, "keycloak-" + suffix(), Set.of());

    TenantUser unchanged = store.markFailed(requestKey, "LATE_FAILURE");

    assertThat(unchanged.getId()).isEqualTo(active.getId());
    assertThat(unchanged.getStatus()).isEqualTo(TenantUserStatus.ACTIVE);
    assertThat(unchanged.getFailureCode()).isNull();
  }

  @Test
  void beginReinvite_failedInvitation_returnsToPending() {
    String requestKey = uniqueKey();
    TenantUser user = store.begin(uniqueEmail(), requestKey, fingerprint("reinvite"));
    store.markFailed(requestKey, "IDENTITY_PROVIDER_UNAVAILABLE");

    TenantUser pending = store.beginReinvite(user.getId());

    assertThat(pending.getStatus()).isEqualTo(TenantUserStatus.PENDING);
    assertThat(pending.getFailureCode()).isNull();
  }

  @Test
  void beginReinvite_delegateRejectsProtectedTargetWithoutChangingState() {
    String requestKey = uniqueKey();
    TenantUser user = store.begin(uniqueEmail(), requestKey, fingerprint("protected-reinvite"));
    store.activate(requestKey, "keycloak-" + suffix(), Set.of());
    TenantUser disabled = store.disable(user.getId());
    Role protectedRole = role("SUPER_ADMIN", Permission.FINANCE_READ);
    userRoleRepository.saveAndFlush(
        UserRole.of(TEST_TENANT_ID, disabled.getKeycloakUserId(), protectedRole));
    authenticate("delegate", Permission.IAM_READ, Permission.IAM_DELEGATE);

    assertThatThrownBy(() -> store.beginReinvite(user.getId()))
        .isInstanceOfSatisfying(
            ErpException.class,
            exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));

    assertThat(tenantUserRepository.findById(user.getId()).orElseThrow().getStatus())
        .isEqualTo(TenantUserStatus.DISABLED);
  }

  private Role role(String code, String permission) {
    Role role = Role.of(TEST_TENANT_ID, code, code, null);
    role.grant(permission);
    return roleRepository.saveAndFlush(role);
  }

  private static String uniqueEmail() {
    return suffix() + "@example.com";
  }

  private static String uniqueKey() {
    return UUID.randomUUID().toString();
  }

  private static String suffix() {
    return UUID.randomUUID().toString().replace("-", "").substring(0, 10);
  }

  private static String fingerprint(String seed) {
    try {
      byte[] digest =
          java.security.MessageDigest.getInstance("SHA-256")
              .digest(seed.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      return java.util.HexFormat.of().formatHex(digest);
    } catch (java.security.NoSuchAlgorithmException impossible) {
      throw new IllegalStateException(impossible);
    }
  }
}
