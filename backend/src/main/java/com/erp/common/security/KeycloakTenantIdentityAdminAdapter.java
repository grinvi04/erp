package com.erp.common.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

public class KeycloakTenantIdentityAdminAdapter implements TenantIdentityAdminPort {

  private static final String TENANT_ATTRIBUTE = "tenant_id";
  private static final String INVITATION_ATTRIBUTE = "erp_invitation_key";
  private static final List<String> REQUIRED_ACTIONS = List.of("VERIFY_EMAIL", "UPDATE_PASSWORD");

  private final RestClient client;
  private final ObjectMapper objectMapper;
  private final String realm;
  private final String clientId;
  private final String clientSecret;
  private final String frontendClientId;
  private final String redirectUri;
  private final long inviteLifespanSeconds;

  public KeycloakTenantIdentityAdminAdapter(
      RestClient.Builder builder,
      ObjectMapper objectMapper,
      String baseUrl,
      String realm,
      String clientId,
      String clientSecret,
      String frontendClientId,
      String redirectUri,
      long inviteLifespanSeconds) {
    this.client = builder.baseUrl(requireText(baseUrl, "baseUrl")).build();
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    this.realm = requireText(realm, "realm");
    this.clientId = requireText(clientId, "clientId");
    this.clientSecret = requireText(clientSecret, "clientSecret");
    this.frontendClientId = requireText(frontendClientId, "frontendClientId");
    this.redirectUri = requireText(redirectUri, "redirectUri");
    if (inviteLifespanSeconds <= 0) {
      throw new IllegalArgumentException("inviteLifespanSeconds must be positive");
    }
    this.inviteLifespanSeconds = inviteLifespanSeconds;
  }

  @Override
  public Optional<TenantIdentityUser> findByEmail(String email) {
    String normalizedEmail = requireText(email, "email").toLowerCase(Locale.ROOT);
    try {
      String token = accessToken();
      JsonNode response =
          client
              .get()
              .uri(
                  builder ->
                      builder
                          .path("/admin/realms/{realm}/users")
                          .queryParam("email", normalizedEmail)
                          .queryParam("exact", true)
                          .queryParam("max", 2)
                          .build(realm))
              .headers(headers -> headers.setBearerAuth(token))
              .retrieve()
              .body(JsonNode.class);
      if (!(response instanceof ArrayNode users)) {
        throw new TenantIdentityAdminException("identity provider returned invalid user search");
      }
      if (users.isEmpty()) {
        return Optional.empty();
      }
      if (users.size() != 1) {
        throw new TenantIdentityAdminException("identity provider returned ambiguous users");
      }
      return Optional.of(toIdentity(requireObject(users.get(0))));
    } catch (RestClientException failure) {
      throw new TenantIdentityAdminException("identity provider user search failed", failure);
    }
  }

  @Override
  public TenantIdentityUser createUser(TenantIdentityCreateRequest request) {
    Objects.requireNonNull(request, "request");
    String email = requireText(request.email(), "email").toLowerCase(Locale.ROOT);
    Long tenantId = requirePositive(request.tenantId(), "tenantId");
    String invitationKey = requireText(request.invitationKey(), "invitationKey");
    try {
      String token = accessToken();
      ObjectNode user = objectMapper.createObjectNode();
      user.put("username", email);
      user.put("email", email);
      putIfText(user, "firstName", request.firstName());
      putIfText(user, "lastName", request.lastName());
      user.put("enabled", true);
      user.put("emailVerified", false);
      user.set("requiredActions", objectMapper.valueToTree(REQUIRED_ACTIONS));
      ObjectNode attributes = objectMapper.createObjectNode();
      attributes.set(TENANT_ATTRIBUTE, objectMapper.valueToTree(List.of(tenantId.toString())));
      attributes.set(INVITATION_ATTRIBUTE, objectMapper.valueToTree(List.of(invitationKey)));
      user.set("attributes", attributes);

      URI location =
          client
              .post()
              .uri("/admin/realms/{realm}/users", realm)
              .headers(headers -> headers.setBearerAuth(token))
              .contentType(MediaType.APPLICATION_JSON)
              .body(user)
              .retrieve()
              .toBodilessEntity()
              .getHeaders()
              .getLocation();
      if (location == null || location.getPath() == null) {
        throw new TenantIdentityAdminException("identity provider returned no user location");
      }
      String path = location.getPath();
      String userId = requireText(path.substring(path.lastIndexOf('/') + 1), "userId");
      return new TenantIdentityUser(userId, tenantId, invitationKey, true);
    } catch (RestClientException failure) {
      throw new TenantIdentityAdminException("identity provider user creation failed", failure);
    }
  }

  @Override
  public void sendInvite(String userId, Long tenantId) {
    String safeUserId = requireText(userId, "userId");
    Long safeTenantId = requirePositive(tenantId, "tenantId");
    try {
      String token = accessToken();
      requireTenant(loadUser(safeUserId, token), safeTenantId);
      client
          .put()
          .uri(
              builder ->
                  builder
                      .path("/admin/realms/{realm}/users/{userId}/execute-actions-email")
                      .queryParam("client_id", frontendClientId)
                      .queryParam("redirect_uri", redirectUri)
                      .queryParam("lifespan", inviteLifespanSeconds)
                      .build(realm, safeUserId))
          .headers(headers -> headers.setBearerAuth(token))
          .contentType(MediaType.APPLICATION_JSON)
          .body(REQUIRED_ACTIONS)
          .retrieve()
          .toBodilessEntity();
    } catch (RestClientException failure) {
      throw new TenantIdentityAdminException("identity provider invitation failed", failure);
    }
  }

  @Override
  public void setEnabled(String userId, Long tenantId, boolean enabled) {
    String safeUserId = requireText(userId, "userId");
    Long safeTenantId = requirePositive(tenantId, "tenantId");
    try {
      String token = accessToken();
      ObjectNode user = loadUser(safeUserId, token);
      requireTenant(user, safeTenantId);
      if (user.path("enabled").asBoolean(false) == enabled) {
        return;
      }
      user.put("enabled", enabled);
      client
          .put()
          .uri("/admin/realms/{realm}/users/{userId}", realm, safeUserId)
          .headers(headers -> headers.setBearerAuth(token))
          .contentType(MediaType.APPLICATION_JSON)
          .body(user)
          .retrieve()
          .toBodilessEntity();
    } catch (RestClientException failure) {
      throw new TenantIdentityAdminException("identity provider user update failed", failure);
    }
  }

  private ObjectNode loadUser(String userId, String token) {
    return requireObject(
        client
            .get()
            .uri("/admin/realms/{realm}/users/{userId}", realm, userId)
            .headers(headers -> headers.setBearerAuth(token))
            .retrieve()
            .body(JsonNode.class));
  }

  private void requireTenant(ObjectNode user, Long tenantId) {
    if (!tenantId.equals(readTenantId(user))) {
      throw new TenantIdentityAdminException("identity user belongs to another tenant");
    }
  }

  private TenantIdentityUser toIdentity(ObjectNode user) {
    String id = requireText(user.path("id").asText(null), "userId");
    return new TenantIdentityUser(
        id,
        readTenantId(user),
        readAttribute(user, INVITATION_ATTRIBUTE),
        user.path("enabled").asBoolean(false));
  }

  private Long readTenantId(ObjectNode user) {
    String value = readAttribute(user, TENANT_ATTRIBUTE);
    if (value == null) {
      return null;
    }
    try {
      return Long.valueOf(value);
    } catch (NumberFormatException invalid) {
      return null;
    }
  }

  private String readAttribute(ObjectNode user, String name) {
    JsonNode value = user.path("attributes").path(name);
    if (!value.isArray() || value.size() != 1 || !value.get(0).isTextual()) {
      return null;
    }
    return value.get(0).asText();
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
      throw new TenantIdentityAdminException("identity provider returned no access token");
    }
    return response.accessToken();
  }

  private void putIfText(ObjectNode target, String field, String value) {
    if (value != null && !value.isBlank()) {
      target.put(field, value.trim());
    }
  }

  private static ObjectNode requireObject(JsonNode value) {
    if (!(value instanceof ObjectNode object)) {
      throw new TenantIdentityAdminException("identity provider returned an invalid user");
    }
    return object;
  }

  private static Long requirePositive(Long value, String field) {
    if (value == null || value <= 0) {
      throw new IllegalArgumentException(field + " must be positive");
    }
    return value;
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
