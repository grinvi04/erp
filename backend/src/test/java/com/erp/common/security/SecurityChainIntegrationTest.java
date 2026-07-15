package com.erp.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "spring.datasource.url=jdbc:tc:postgresql:16:///erp_security_test",
      "spring.datasource.username=erp_ci",
      "spring.datasource.password=erp_ci_pass"
    })
@ActiveProfiles("security-integration")
@Import(SecurityChainIntegrationTest.JwtTestConfiguration.class)
class SecurityChainIntegrationTest {

  private static final long ACTIVE_TENANT_ID = 91001L;
  private static final long SUSPENDED_TENANT_ID = 91002L;
  private static final long ROLE_ID = 91001L;
  private static final String USER_ID = "security-chain-user";
  private static final KeyPair TRUSTED_KEY_PAIR = generateKeyPair();

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private JwtEncoder jwtEncoder;

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
            "/api/me/permissions",
            HttpMethod.GET,
            bearer(signedToken(jwtEncoder, ACTIVE_TENANT_ID, Instant.now().plusSeconds(300))),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains(Permission.HR_EMPLOYEE_READ);
  }

  @Test
  void validJwtForSuspendedTenantIsRejected() {
    var response =
        restTemplate.exchange(
            "/api/me/permissions",
            HttpMethod.GET,
            bearer(signedToken(jwtEncoder, SUSPENDED_TENANT_ID, Instant.now().plusSeconds(300))),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody()).contains("C004");
  }

  @Test
  void jwtSignedByUntrustedKeyIsUnauthorized() {
    JwtEncoder untrustedEncoder = encoder(generateKeyPair());

    var response =
        restTemplate.exchange(
            "/api/me/permissions",
            HttpMethod.GET,
            bearer(signedToken(untrustedEncoder, ACTIVE_TENANT_ID, Instant.now().plusSeconds(300))),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void tamperedJwtIsUnauthorized() {
    String valid = signedToken(jwtEncoder, ACTIVE_TENANT_ID, Instant.now().plusSeconds(300));
    String[] segments = valid.split("\\.");
    int last = segments[1].length() - 1;
    char replacement = segments[1].charAt(last) == 'A' ? 'B' : 'A';
    segments[1] = segments[1].substring(0, last) + replacement;

    var response =
        restTemplate.exchange(
            "/api/me/permissions",
            HttpMethod.GET,
            bearer(String.join(".", segments)),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void expiredJwtIsUnauthorized() {
    var response =
        restTemplate.exchange(
            "/api/me/permissions",
            HttpMethod.GET,
            bearer(signedToken(jwtEncoder, ACTIVE_TENANT_ID, Instant.now().minusSeconds(60))),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void signedJwtWithoutTenantClaimIsForbidden() {
    var response =
        restTemplate.exchange(
            "/api/me/permissions",
            HttpMethod.GET,
            bearer(signedToken(jwtEncoder, null, Instant.now().plusSeconds(300))),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody()).contains("C004");
  }

  private HttpEntity<Void> bearer(String token) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return new HttpEntity<>(headers);
  }

  private static String signedToken(JwtEncoder encoder, Long tenantId, Instant expiresAt) {
    Instant now = Instant.now();
    JwtClaimsSet.Builder claims =
        JwtClaimsSet.builder()
            .subject(USER_ID)
            .issuedAt(now.minusSeconds(120))
            .expiresAt(expiresAt)
            .claim("preferred_username", USER_ID);
    if (tenantId != null) {
      claims.claim("tenant_id", tenantId);
    }
    JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
    return encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
  }

  private static JwtEncoder encoder(KeyPair keyPair) {
    RSAKey rsaKey =
        new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
            .privateKey((RSAPrivateKey) keyPair.getPrivate())
            .keyID("test-key")
            .build();
    return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaKey)));
  }

  private static KeyPair generateKeyPair() {
    try {
      KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
      generator.initialize(2048);
      return generator.generateKeyPair();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("RSA is required by the Java runtime", e);
    }
  }

  @TestConfiguration
  static class JwtTestConfiguration {

    @Bean
    JwtDecoder jwtDecoder() {
      return NimbusJwtDecoder.withPublicKey((RSAPublicKey) TRUSTED_KEY_PAIR.getPublic()).build();
    }

    @Bean
    JwtEncoder jwtEncoder() {
      return encoder(TRUSTED_KEY_PAIR);
    }
  }
}
