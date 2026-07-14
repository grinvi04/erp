package com.erp.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.erp.common.AbstractIntegrationTest;
import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class JpaTenantUserOnboardingStoreIntegrationTest extends AbstractIntegrationTest {

  @Autowired private TenantUserOnboardingStore store;
  @Autowired private TenantUserRepository tenantUserRepository;
  @Autowired private RoleRepository roleRepository;
  @Autowired private UserRoleRepository userRoleRepository;

  @BeforeEach
  void authenticateOperator() {
    authenticate("operator", Permission.IAM_READ, Permission.IAM_WRITE);
  }

  @Test
  void begin_isIdempotentByRequestKeyAndRejectsIdentityReuse() {
    String email = uniqueEmail();
    String requestKey = uniqueKey();

    TenantUser first = store.begin(email, requestKey, fingerprint("payload-1"));
    TenantUser repeated = store.begin(email.toUpperCase(), requestKey, fingerprint("payload-1"));

    assertThat(repeated.getId()).isEqualTo(first.getId());
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
