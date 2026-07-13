package com.erp.common.tenant.provisioning;

import com.erp.ErpApplication;
import com.erp.common.tenant.TenantPlan;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;

public final class TenantProvisioningCommand {

  private static final Logger LOG = LoggerFactory.getLogger(TenantProvisioningCommand.class);

  private TenantProvisioningCommand() {}

  static TenantProvisioningRequest requestFrom(Map<String, String> environment) {
    String planValue = environment.getOrDefault("ERP_PROVISION_TENANT_PLAN", "STANDARD");
    TenantPlan plan;
    try {
      plan = TenantPlan.valueOf(planValue.trim().toUpperCase(Locale.ROOT));
    } catch (RuntimeException invalid) {
      throw new IllegalArgumentException("ERP_PROVISION_TENANT_PLAN is invalid", invalid);
    }
    return new TenantProvisioningRequest(
        required(environment, "ERP_PROVISION_TENANT_CODE"),
        required(environment, "ERP_PROVISION_TENANT_NAME"),
        plan,
        required(environment, "ERP_PROVISION_ADMIN_USER_ID"),
        required(environment, "ERP_PROVISIONED_BY"));
  }

  public static void main(String[] args) {
    Map<String, String> environment = System.getenv();
    SpringApplication application = new SpringApplication(ErpApplication.class);
    application.setWebApplicationType(WebApplicationType.NONE);
    application.setAdditionalProfiles("provision");
    try (ConfigurableApplicationContext context = application.run(args)) {
      TenantProvisioningService service = context.getBean(TenantProvisioningService.class);
      TenantProvisioningResult result;
      if (Boolean.parseBoolean(environment.getOrDefault("ERP_PROVISION_RETRY", "false"))) {
        result =
            service.retry(
                required(environment, "ERP_PROVISION_TENANT_CODE"),
                required(environment, "ERP_PROVISIONED_BY"));
      } else {
        result = service.provision(requestFrom(environment));
      }
      LOG.info(
          "tenant provisioning completed: tenantId={} code={} status={}",
          result.tenantId(),
          result.code(),
          result.status());
    }
  }

  private static String required(Map<String, String> environment, String name) {
    String value = environment.get(name);
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(name + " is required");
    }
    return value.trim();
  }
}
