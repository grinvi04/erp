package com.erp.common.tenant.provisioning;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Objects;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

public class KeycloakTenantIdentityAdapter implements TenantIdentityProvisioningPort {

  private final RestClient client;
  private final ObjectMapper objectMapper;
  private final String realm;
  private final String clientId;
  private final String clientSecret;

  public KeycloakTenantIdentityAdapter(
      RestClient.Builder builder,
      ObjectMapper objectMapper,
      String baseUrl,
      String realm,
      String clientId,
      String clientSecret) {
    this.client = builder.baseUrl(requireText(baseUrl, "baseUrl")).build();
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    this.realm = requireText(realm, "realm");
    this.clientId = requireText(clientId, "clientId");
    this.clientSecret = requireText(clientSecret, "clientSecret");
  }

  @Override
  public void assignTenant(String userId, Long tenantId) {
    String safeUserId = requireText(userId, "userId");
    if (tenantId == null || tenantId <= 0) {
      throw new IllegalArgumentException("tenantId must be positive");
    }

    String token = accessToken();
    ObjectNode user =
        requireObject(
            client
                .get()
                .uri("/admin/realms/{realm}/users/{userId}", realm, safeUserId)
                .headers(headers -> headers.setBearerAuth(token))
                .retrieve()
                .body(JsonNode.class));
    ObjectNode attributes = attributes(user);
    String target = tenantId.toString();
    JsonNode existing = attributes.get("tenant_id");
    if (existing != null && !existing.isNull() && existing.size() > 0) {
      if (existing.isArray() && existing.size() == 1 && target.equals(existing.get(0).asText())) {
        return;
      }
      throw new TenantIdentityConflictException("user already belongs to another tenant");
    }

    ArrayNode values = objectMapper.createArrayNode().add(target);
    attributes.set("tenant_id", values);
    client
        .put()
        .uri("/admin/realms/{realm}/users/{userId}", realm, safeUserId)
        .headers(headers -> headers.setBearerAuth(token))
        .contentType(MediaType.APPLICATION_JSON)
        .body(user)
        .retrieve()
        .toBodilessEntity();
  }

  private String accessToken() {
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("grant_type", "client_credentials");
    form.add("client_id", clientId);
    form.add("client_secret", clientSecret);
    TokenResponse response =
        client
            .post()
            .uri("/realms/{realm}/protocol/openid-connect/token", realm)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(form)
            .retrieve()
            .body(TokenResponse.class);
    if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
      throw new IllegalStateException("identity provider returned no access token");
    }
    return response.accessToken();
  }

  private ObjectNode attributes(ObjectNode user) {
    JsonNode current = user.get("attributes");
    if (current == null || current.isNull()) {
      ObjectNode created = objectMapper.createObjectNode();
      user.set("attributes", created);
      return created;
    }
    return requireObject(current);
  }

  private static ObjectNode requireObject(JsonNode value) {
    if (!(value instanceof ObjectNode object)) {
      throw new IllegalStateException("identity provider returned an invalid user representation");
    }
    return object;
  }

  private static String requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " is required");
    }
    return value.trim();
  }

  private record TokenResponse(
      @com.fasterxml.jackson.annotation.JsonProperty("access_token") String accessToken) {}
}
