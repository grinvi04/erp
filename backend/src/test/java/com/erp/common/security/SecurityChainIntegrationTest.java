package com.erp.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "spring.datasource.url=jdbc:tc:postgresql:16:///erp_security_test",
      "spring.datasource.username=erp_ci",
      "spring.datasource.password=erp_ci_pass"
    })
@ActiveProfiles("security-integration")
class SecurityChainIntegrationTest {

  private static final long ACTIVE_TENANT_ID = 91001L;
  private static final long SUSPENDED_TENANT_ID = 91002L;
  private static final long ROLE_ID = 91001L;
  private static final String USER_ID = "security-chain-user";

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private JdbcTemplate jdbcTemplate;
  @MockitoBean private JwtDecoder jwtDecoder;

  @BeforeEach
  void setUpAuthorizationData() {
    jdbcTemplate.update(
        "DELETE FROM common.user_directory WHERE tenant_id IN (?, ?)",
        ACTIVE_TENANT_ID,
        SUSPENDED_TENANT_ID);
    jdbcTemplate.update(
        "DELETE FROM common.user_role WHERE tenant_id IN (?, ?)",
        ACTIVE_TENANT_ID,
        SUSPENDED_TENANT_ID);
    jdbcTemplate.update("DELETE FROM common.role_permission WHERE role_id = ?", ROLE_ID);
    jdbcTemplate.update("DELETE FROM common.role WHERE id = ?", ROLE_ID);
    jdbcTemplate.update(
        "DELETE FROM common.tenant WHERE id IN (?, ?)", ACTIVE_TENANT_ID, SUSPENDED_TENANT_ID);

    jdbcTemplate.update(
        "INSERT INTO common.tenant (id, code, name, status) VALUES (?, ?, ?, ?)",
        ACTIVE_TENANT_ID,
        "SEC_ACTIVE",
        "Security Active",
        "ACTIVE");
    jdbcTemplate.update(
        "INSERT INTO common.tenant (id, code, name, status) VALUES (?, ?, ?, ?)",
        SUSPENDED_TENANT_ID,
        "SEC_SUSPENDED",
        "Security Suspended",
        "SUSPENDED");
    jdbcTemplate.update(
        "INSERT INTO common.role (id, tenant_id, code, name) VALUES (?, ?, ?, ?)",
        ROLE_ID,
        ACTIVE_TENANT_ID,
        "SECURITY_TEST",
        "Security Test");
    jdbcTemplate.update(
        "INSERT INTO common.role_permission (tenant_id, role_id, permission_code) VALUES (?, ?, ?)",
        ACTIVE_TENANT_ID,
        ROLE_ID,
        Permission.HR_EMPLOYEE_READ);
    jdbcTemplate.update(
        "INSERT INTO common.user_role (tenant_id, user_id, role_id) VALUES (?, ?, ?)",
        ACTIVE_TENANT_ID,
        USER_ID,
        ROLE_ID);

    when(jwtDecoder.decode("active-token")).thenReturn(jwt("active-token", ACTIVE_TENANT_ID));
    when(jwtDecoder.decode("suspended-token"))
        .thenReturn(jwt("suspended-token", SUSPENDED_TENANT_ID));
  }

  @Test
  void businessApiWithoutBearerTokenIsUnauthorized() {
    var response = restTemplate.getForEntity("/api/hr/employees", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void healthEndpointRemainsPublic() {
    var response = restTemplate.getForEntity("/actuator/health", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void validJwtLoadsDatabaseAuthoritiesForActiveTenant() {
    var response =
        restTemplate.exchange(
            "/api/me/permissions", HttpMethod.GET, bearer("active-token"), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains(Permission.HR_EMPLOYEE_READ);
  }

  @Test
  void validJwtForSuspendedTenantIsRejected() {
    var response =
        restTemplate.exchange(
            "/api/me/permissions", HttpMethod.GET, bearer("suspended-token"), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody()).contains("C004");
  }

  private HttpEntity<Void> bearer(String token) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return new HttpEntity<>(headers);
  }

  private Jwt jwt(String tokenValue, long tenantId) {
    Instant now = Instant.now();
    return Jwt.withTokenValue(tokenValue)
        .header("alg", "RS256")
        .subject(USER_ID)
        .issuedAt(now.minusSeconds(30))
        .expiresAt(now.plusSeconds(300))
        .claim("tenant_id", tenantId)
        .claim("preferred_username", USER_ID)
        .build();
  }
}
