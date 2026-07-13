package com.erp.common.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class TenantTest {

  @Test
  void startProvisioning_normalizesIdentityAndStartsInactive() {
    Tenant tenant =
        Tenant.startProvisioning(" acme-kr ", " 에이씨미 코리아 ", TenantPlan.STANDARD, "kc-123");

    assertThat(tenant.getCode()).isEqualTo("ACME-KR");
    assertThat(tenant.getName()).isEqualTo("에이씨미 코리아");
    assertThat(tenant.getPlan()).isEqualTo(TenantPlan.STANDARD);
    assertThat(tenant.getAdminUserId()).isEqualTo("kc-123");
    assertThat(tenant.getStatus()).isEqualTo(TenantStatus.PROVISIONING);
    assertThat(tenant.getProvisioningError()).isNull();
  }

  @Test
  void failedProvisioning_canRetryAndActivate() {
    Tenant tenant = Tenant.startProvisioning("ACME", "Acme", TenantPlan.TRIAL, "kc-123");

    tenant.fail("Keycloak unavailable");
    assertThat(tenant.getStatus()).isEqualTo(TenantStatus.FAILED);
    assertThat(tenant.getProvisioningError()).isEqualTo("Keycloak unavailable");

    tenant.retry();
    assertThat(tenant.getStatus()).isEqualTo(TenantStatus.PROVISIONING);
    assertThat(tenant.getProvisioningError()).isNull();

    tenant.activate();
    assertThat(tenant.getStatus()).isEqualTo(TenantStatus.ACTIVE);
  }

  @Test
  void activeTenant_canBeSuspendedButCannotBeReactivatedThroughProvisioning() {
    Tenant tenant = Tenant.startProvisioning("ACME", "Acme", TenantPlan.TRIAL, "kc-123");
    tenant.activate();

    tenant.suspend();
    assertThat(tenant.getStatus()).isEqualTo(TenantStatus.SUSPENDED);
    assertThatThrownBy(tenant::activate).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void invalidIdentityAndInvalidTransitionsAreRejected() {
    assertThatThrownBy(() -> Tenant.startProvisioning("!", "Acme", TenantPlan.TRIAL, "kc-123"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> Tenant.startProvisioning("ACME", " ", TenantPlan.TRIAL, "kc-123"))
        .isInstanceOf(IllegalArgumentException.class);

    Tenant tenant = Tenant.startProvisioning("ACME", "Acme", TenantPlan.TRIAL, "kc-123");
    assertThatThrownBy(tenant::retry).isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(tenant::suspend).isInstanceOf(IllegalStateException.class);
  }
}
