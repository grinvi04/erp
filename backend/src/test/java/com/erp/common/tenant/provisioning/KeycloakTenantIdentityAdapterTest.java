package com.erp.common.tenant.provisioning;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class KeycloakTenantIdentityAdapterTest {

  private MockRestServiceServer server;
  private KeycloakTenantIdentityAdapter adapter;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder();
    server = MockRestServiceServer.bindTo(builder).build();
    adapter =
        new KeycloakTenantIdentityAdapter(
            builder, new ObjectMapper(), "http://keycloak.test", "erp", "ops-client", "secret");
  }

  @Test
  void assignTenant_preservesExistingAttributesAndAddsTenantId() {
    expectToken();
    server
        .expect(once(), requestTo("http://keycloak.test/admin/realms/erp/users/user-1"))
        .andExpect(method(GET))
        .andRespond(
            withSuccess(
                "{\"id\":\"user-1\",\"username\":\"admin\","
                    + "\"attributes\":{\"department\":[\"sales\"]}}",
                MediaType.APPLICATION_JSON));
    server
        .expect(once(), requestTo("http://keycloak.test/admin/realms/erp/users/user-1"))
        .andExpect(method(PUT))
        .andExpect(
            content()
                .json("{\"attributes\":{\"department\":[\"sales\"]," + "\"tenant_id\":[\"42\"]}}"))
        .andRespond(withSuccess());

    adapter.assignTenant("user-1", 42L);

    server.verify();
  }

  @Test
  void assignTenant_sameTenantIsIdempotentWithoutUpdate() {
    expectToken();
    server
        .expect(once(), requestTo("http://keycloak.test/admin/realms/erp/users/user-1"))
        .andExpect(method(GET))
        .andRespond(
            withSuccess(
                "{\"id\":\"user-1\",\"attributes\":{\"tenant_id\":[\"42\"]}}",
                MediaType.APPLICATION_JSON));

    adapter.assignTenant("user-1", 42L);

    server.verify();
  }

  @Test
  void assignTenant_differentExistingTenantIsRejectedWithoutOverwrite() {
    expectToken();
    server
        .expect(once(), requestTo("http://keycloak.test/admin/realms/erp/users/user-1"))
        .andExpect(method(GET))
        .andRespond(
            withSuccess(
                "{\"id\":\"user-1\",\"attributes\":{\"tenant_id\":[\"7\"]}}",
                MediaType.APPLICATION_JSON));

    assertThatThrownBy(() -> adapter.assignTenant("user-1", 42L))
        .isInstanceOf(TenantIdentityConflictException.class);
    server.verify();
  }

  private void expectToken() {
    server
        .expect(once(), requestTo("http://keycloak.test/realms/erp/protocol/openid-connect/token"))
        .andExpect(method(POST))
        .andRespond(withSuccess("{\"access_token\":\"token-123\"}", MediaType.APPLICATION_JSON));
  }
}
