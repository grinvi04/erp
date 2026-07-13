package com.erp.common.tenant.provisioning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.erp.common.tenant.TenantPlan;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TenantProvisioningCommandTest {

  @Test
  void requestFrom_readsNonSecretOperatorInputsAndDefaultsPlan() {
    TenantProvisioningRequest request =
        TenantProvisioningCommand.requestFrom(
            Map.of(
                "ERP_PROVISION_TENANT_CODE", "ACME",
                "ERP_PROVISION_TENANT_NAME", "Acme Korea",
                "ERP_PROVISION_ADMIN_USER_ID", "kc-user-1",
                "ERP_PROVISIONED_BY", "ops-user"));

    assertThat(request.code()).isEqualTo("ACME");
    assertThat(request.name()).isEqualTo("Acme Korea");
    assertThat(request.plan()).isEqualTo(TenantPlan.STANDARD);
    assertThat(request.adminUserId()).isEqualTo("kc-user-1");
    assertThat(request.performedBy()).isEqualTo("ops-user");
  }

  @Test
  void requestFrom_rejectsMissingRequiredInputAndInvalidPlan() {
    assertThatThrownBy(() -> TenantProvisioningCommand.requestFrom(Map.of()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("ERP_PROVISION_TENANT_CODE");

    assertThatThrownBy(
            () ->
                TenantProvisioningCommand.requestFrom(
                    Map.of(
                        "ERP_PROVISION_TENANT_CODE", "ACME",
                        "ERP_PROVISION_TENANT_NAME", "Acme",
                        "ERP_PROVISION_TENANT_PLAN", "UNKNOWN",
                        "ERP_PROVISION_ADMIN_USER_ID", "kc-user-1",
                        "ERP_PROVISIONED_BY", "ops-user")))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
