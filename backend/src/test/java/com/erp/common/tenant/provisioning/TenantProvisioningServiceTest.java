package com.erp.common.tenant.provisioning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.erp.common.tenant.Tenant;
import com.erp.common.tenant.TenantPlan;
import com.erp.common.tenant.TenantStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TenantProvisioningServiceTest {

  @Mock private TenantProvisioningStore store;
  @Mock private TenantIdentityProvisioningPort identityPort;
  private TenantProvisioningService service;

  @BeforeEach
  void setUp() {
    service = new TenantProvisioningService(store, identityPort);
  }

  @Test
  void provision_assignsTenantThenActivates() {
    Tenant provisioning = tenant(42L, TenantStatus.PROVISIONING);
    Tenant active = tenant(42L, TenantStatus.PROVISIONING);
    active.activate();
    TenantProvisioningRequest request =
        new TenantProvisioningRequest("ACME", "Acme", TenantPlan.STANDARD, "kc-user-1", "ops-user");
    given(store.begin(request)).willReturn(provisioning);
    given(store.activate(42L, "ops-user")).willReturn(active);

    TenantProvisioningResult result = service.provision(request);

    verify(identityPort).assignTenant("kc-user-1", 42L);
    verify(store).activate(42L, "ops-user");
    assertThat(result.status()).isEqualTo(TenantStatus.ACTIVE);
    assertThat(result.tenantId()).isEqualTo(42L);
  }

  @Test
  void provision_identityFailurePersistsFailedStateAndRethrowsSafeError() {
    Tenant provisioning = tenant(42L, TenantStatus.PROVISIONING);
    TenantProvisioningRequest request =
        new TenantProvisioningRequest("ACME", "Acme", TenantPlan.STANDARD, "kc-user-1", "ops-user");
    given(store.begin(request)).willReturn(provisioning);
    org.mockito.Mockito.doThrow(new RuntimeException("remote secret detail"))
        .when(identityPort)
        .assignTenant("kc-user-1", 42L);

    assertThatThrownBy(() -> service.provision(request))
        .isInstanceOf(TenantProvisioningException.class)
        .hasMessage("tenant provisioning failed");

    verify(store).fail(42L, "identity provider update failed", "ops-user");
  }

  @Test
  void retry_reusesFailedTenantAndRunsSameActivationFlow() {
    Tenant retrying = tenant(42L, TenantStatus.PROVISIONING);
    Tenant active = tenant(42L, TenantStatus.PROVISIONING);
    active.activate();
    given(store.beginRetry("ACME", "ops-user")).willReturn(retrying);
    given(store.activate(42L, "ops-user")).willReturn(active);

    TenantProvisioningResult result = service.retry("ACME", "ops-user");

    verify(identityPort).assignTenant("kc-user-1", 42L);
    assertThat(result.status()).isEqualTo(TenantStatus.ACTIVE);
  }

  private static Tenant tenant(Long id, TenantStatus status) {
    Tenant tenant = Tenant.startProvisioning("ACME", "Acme", TenantPlan.STANDARD, "kc-user-1");
    ReflectionTestUtils.setField(tenant, "id", id);
    if (status == TenantStatus.ACTIVE) {
      tenant.activate();
    }
    return tenant;
  }
}
