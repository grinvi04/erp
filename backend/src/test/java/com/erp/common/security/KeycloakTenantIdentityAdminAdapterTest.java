package com.erp.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withCreatedEntity;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class KeycloakTenantIdentityAdminAdapterTest {

  private MockRestServiceServer server;
  private KeycloakTenantIdentityAdminAdapter adapter;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder();
    server = MockRestServiceServer.bindTo(builder).build();
    adapter =
        new KeycloakTenantIdentityAdminAdapter(
            builder,
            new ObjectMapper(),
            "http://keycloak.test",
            "erp",
            "user-admin",
            "secret",
            "erp-frontend",
            "https://erp.example.com/login",
            900L);
  }

  @Test
  void findByEmail_usesExactSearchAndReadsOwnershipMarker() {
    expectToken();
    server
        .expect(
            once(),
            requestTo(
                "http://keycloak.test/admin/realms/erp/users?email=admin@example.com&exact=true&max=2"))
        .andExpect(method(GET))
        .andRespond(
            withSuccess(
                "[{\"id\":\"user-1\",\"enabled\":true,\"attributes\":{"
                    + "\"tenant_id\":[\"42\"],\"erp_invitation_key\":[\"request-1\"]}}]",
                MediaType.APPLICATION_JSON));

    var result = adapter.findByEmail("Admin@Example.COM").orElseThrow();

    assertThat(result).isEqualTo(new TenantIdentityUser("user-1", 42L, "request-1", true));
    server.verify();
  }

  @Test
  void createUser_setsImmutableTenantAndInvitationAttributes() {
    expectToken();
    server
        .expect(once(), requestTo("http://keycloak.test/admin/realms/erp/users"))
        .andExpect(method(POST))
        .andExpect(
            content()
                .json(
                    "{\"username\":\"admin@example.com\",\"email\":\"admin@example.com\","
                        + "\"enabled\":true,\"emailVerified\":false,"
                        + "\"requiredActions\":[\"VERIFY_EMAIL\",\"UPDATE_PASSWORD\"],"
                        + "\"attributes\":{\"tenant_id\":[\"42\"],"
                        + "\"erp_invitation_key\":[\"request-1\"]}}"))
        .andRespond(
            withCreatedEntity(
                URI.create("http://keycloak.test/admin/realms/erp/users/keycloak-user-1")));

    var result =
        adapter.createUser(
            new TenantIdentityCreateRequest(42L, "admin@example.com", "ERP", "Admin", "request-1"));

    assertThat(result).isEqualTo(new TenantIdentityUser("keycloak-user-1", 42L, "request-1", true));
    server.verify();
  }

  @Test
  void sendInvite_usesConfiguredClientRedirectAndShortLifespan() {
    expectToken();
    server
        .expect(once(), requestTo("http://keycloak.test/admin/realms/erp/users/user-1"))
        .andExpect(method(GET))
        .andRespond(
            withSuccess(
                "{\"id\":\"user-1\",\"enabled\":true," + "\"attributes\":{\"tenant_id\":[\"42\"]}}",
                MediaType.APPLICATION_JSON));
    server
        .expect(
            once(),
            requestTo(
                "http://keycloak.test/admin/realms/erp/users/user-1/execute-actions-email"
                    + "?client_id=erp-frontend&redirect_uri=https://erp.example.com/login&lifespan=900"))
        .andExpect(method(PUT))
        .andExpect(content().json("[\"VERIFY_EMAIL\",\"UPDATE_PASSWORD\"]"))
        .andRespond(withSuccess());

    adapter.sendInvite("user-1", 42L);

    server.verify();
  }

  @Test
  void setEnabled_rejectsDifferentTenantWithoutUpdate() {
    expectToken();
    server
        .expect(once(), requestTo("http://keycloak.test/admin/realms/erp/users/user-1"))
        .andExpect(method(GET))
        .andRespond(
            withSuccess(
                "{\"id\":\"user-1\",\"enabled\":true," + "\"attributes\":{\"tenant_id\":[\"7\"]}}",
                MediaType.APPLICATION_JSON));

    assertThatThrownBy(() -> adapter.setEnabled("user-1", 42L, false))
        .isInstanceOf(TenantIdentityAdminException.class);
    server.verify();
  }

  private void expectToken() {
    server
        .expect(once(), requestTo("http://keycloak.test/realms/erp/protocol/openid-connect/token"))
        .andExpect(method(POST))
        .andRespond(withSuccess("{\"access_token\":\"token-123\"}", MediaType.APPLICATION_JSON));
  }
}
