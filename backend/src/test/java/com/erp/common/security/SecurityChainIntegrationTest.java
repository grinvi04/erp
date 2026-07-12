package com.erp.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
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

  @Autowired private TestRestTemplate restTemplate;
  @MockitoBean private JwtDecoder jwtDecoder;

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
}
