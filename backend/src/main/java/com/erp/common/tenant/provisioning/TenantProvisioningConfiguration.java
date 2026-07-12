package com.erp.common.tenant.provisioning;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
@Profile("provision")
public class TenantProvisioningConfiguration {

  @Bean
  TenantIdentityProvisioningPort tenantIdentityProvisioningPort(
      RestClient.Builder builder,
      ObjectMapper objectMapper,
      @Value("${ERP_KEYCLOAK_BASE_URL:http://localhost:8180}") String baseUrl,
      @Value("${ERP_KEYCLOAK_REALM:erp}") String realm,
      @Value("${ERP_KEYCLOAK_PROVISIONING_CLIENT_ID:}") String clientId,
      @Value("${ERP_KEYCLOAK_PROVISIONING_CLIENT_SECRET:}") String clientSecret) {
    return new KeycloakTenantIdentityAdapter(
        builder, objectMapper, baseUrl, realm, clientId, clientSecret);
  }

  @Bean
  TenantProvisioningService tenantProvisioningService(
      TenantProvisioningStore store, TenantIdentityProvisioningPort identityPort) {
    return new TenantProvisioningService(store, identityPort);
  }
}
