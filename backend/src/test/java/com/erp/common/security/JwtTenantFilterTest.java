package com.erp.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.erp.common.tenant.TenantContext;
import com.erp.common.tenant.TenantRepository;
import com.erp.common.tenant.TenantStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class JwtTenantFilterTest {

  private final TenantRepository tenantRepository = mock(TenantRepository.class);
  private final JwtTenantFilter filter = new JwtTenantFilter(tenantRepository, new ObjectMapper());

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
    TenantContext.clear();
  }

  @Test
  void activeTenantContinuesRequestAndClearsThreadLocalAfterward() throws Exception {
    authenticate(Map.of("tenant_id", 42L));
    given(tenantRepository.existsByIdAndStatus(42L, TenantStatus.ACTIVE)).willReturn(true);
    FilterChain chain = mock(FilterChain.class);

    filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);

    verify(chain).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    assertThat(TenantContext.getTenantId()).isNull();
  }

  @Test
  void unknownOrInactiveTenantIsForbiddenBeforeApplicationCode() throws Exception {
    authenticate(Map.of("tenant_id", 42L));
    given(tenantRepository.existsByIdAndStatus(42L, TenantStatus.ACTIVE)).willReturn(false);
    FilterChain chain = mock(FilterChain.class);
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(new MockHttpServletRequest(), response, chain);

    assertThat(response.getStatus()).isEqualTo(403);
    assertThat(response.getContentAsString()).contains("\"code\":\"C004\"");
    verifyNoInteractions(chain);
  }

  @Test
  void missingOrMalformedTenantClaimIsForbidden() throws Exception {
    authenticate(Map.of("tenant_id", "not-a-number"));
    FilterChain chain = mock(FilterChain.class);
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(new MockHttpServletRequest(), response, chain);

    assertThat(response.getStatus()).isEqualTo(403);
    verifyNoInteractions(chain);
    verifyNoInteractions(tenantRepository);
  }

  private static void authenticate(Map<String, Object> claims) {
    Map<String, Object> claimsWithSubject = new HashMap<>(claims);
    claimsWithSubject.put("sub", "user-1");
    Jwt jwt =
        new Jwt(
            "token",
            Instant.now(),
            Instant.now().plusSeconds(300),
            Map.of("alg", "none"),
            claimsWithSubject);
    SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
  }
}
