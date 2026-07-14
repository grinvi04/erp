package com.erp.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
public class TenantIdentityAdminConfiguration {

  @Bean
  @ConditionalOnProperty(name = "erp.keycloak.user-admin.enabled", havingValue = "true")
  TenantIdentityAdminPort keycloakTenantIdentityAdminPort(
      RestClient.Builder builder,
      ObjectMapper objectMapper,
      @Value("${erp.keycloak.user-admin.base-url}") String baseUrl,
      @Value("${erp.keycloak.user-admin.realm}") String realm,
      @Value("${erp.keycloak.user-admin.client-id}") String clientId,
      @Value("${erp.keycloak.user-admin.client-secret}") String clientSecret,
      @Value("${erp.keycloak.user-admin.frontend-client-id}") String frontendClientId,
      @Value("${erp.keycloak.user-admin.redirect-uri}") String redirectUri,
      @Value("${erp.keycloak.user-admin.invite-lifespan-seconds}") long inviteLifespanSeconds) {
    return new KeycloakTenantIdentityAdminAdapter(
        builder,
        objectMapper,
        baseUrl,
        realm,
        clientId,
        clientSecret,
        frontendClientId,
        redirectUri,
        inviteLifespanSeconds);
  }

  @Bean
  @ConditionalOnMissingBean(TenantIdentityAdminPort.class)
  TenantIdentityAdminPort unavailableTenantIdentityAdminPort() {
    return new UnavailableTenantIdentityAdminPort();
  }
}
