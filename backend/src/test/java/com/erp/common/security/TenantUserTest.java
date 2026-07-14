package com.erp.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class TenantUserTest {

  @Test
  void pending_normalizesEmailAndStartsWithoutIdentity() {
    TenantUser user = TenantUser.pending("  Admin@Example.COM ", "request-1");

    assertThat(user.getNormalizedEmail()).isEqualTo("admin@example.com");
    assertThat(user.getRequestKey()).isEqualTo("request-1");
    assertThat(user.getStatus()).isEqualTo(TenantUserStatus.PENDING);
    assertThat(user.getKeycloakUserId()).isNull();
  }

  @Test
  void activate_bindsIdentityOnce() {
    TenantUser user = TenantUser.pending("admin@example.com", "request-1");

    user.activate("keycloak-user-1");
    user.activate("keycloak-user-1");

    assertThat(user.getStatus()).isEqualTo(TenantUserStatus.ACTIVE);
    assertThat(user.getKeycloakUserId()).isEqualTo("keycloak-user-1");
    assertThatThrownBy(() -> user.activate("another-keycloak-user"))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void failedInvitation_canRetryWithoutChangingIdentity() {
    TenantUser user = TenantUser.pending("admin@example.com", "request-1");
    user.activate("keycloak-user-1");
    user.fail("INVITE_EMAIL_FAILED");

    user.retry();

    assertThat(user.getStatus()).isEqualTo(TenantUserStatus.PENDING);
    assertThat(user.getFailureCode()).isNull();
    assertThat(user.getKeycloakUserId()).isEqualTo("keycloak-user-1");
  }

  @Test
  void disabledUser_canBeginReinviteAndReactivateSameIdentity() {
    TenantUser user = TenantUser.pending("admin@example.com", "request-1");
    user.activate("keycloak-user-1");
    user.disable();

    user.beginReinvite();
    user.activate("keycloak-user-1");

    assertThat(user.getStatus()).isEqualTo(TenantUserStatus.ACTIVE);
    assertThat(user.getKeycloakUserId()).isEqualTo("keycloak-user-1");
  }

  @Test
  void invalidTransitionsAndBlankIdentifiersAreRejected() {
    assertThatThrownBy(() -> TenantUser.pending(" ", "request-1"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> TenantUser.pending("admin@example.com", " "))
        .isInstanceOf(IllegalArgumentException.class);

    TenantUser user = TenantUser.pending("admin@example.com", "request-1");
    assertThatThrownBy(user::disable).isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(user::retry).isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> user.activate(" ")).isInstanceOf(IllegalArgumentException.class);
  }
}
