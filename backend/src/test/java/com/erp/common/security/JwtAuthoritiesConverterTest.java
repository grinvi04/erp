package com.erp.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class JwtAuthoritiesConverterTest {

  @Mock private AuthorizationResolver authorizationResolver;

  @InjectMocks private JwtAuthoritiesConverter converter;

  private Jwt jwt(Object tenantId) {
    Jwt.Builder b = Jwt.withTokenValue("t").header("alg", "none").subject("u");
    if (tenantId != null) {
      b.claim("tenant_id", tenantId);
    }
    return b.build();
  }

  @Test
  void convert_resolvesPermissionsFromDbByTenantAndUser() {
    given(authorizationResolver.permissionCodes(1L, "u"))
        .willReturn(Set.of("hr:employee:write", "finance:read"));

    var authorities = converter.convert(jwt(1L));

    assertThat(authorities)
        .extracting(GrantedAuthority::getAuthority)
        .containsExactlyInAnyOrder("hr:employee:write", "finance:read");
  }

  @Test
  void convert_tenantIdAsString_parsed() {
    given(authorizationResolver.permissionCodes(7L, "u")).willReturn(Set.of("crm:read"));

    assertThat(converter.convert(jwt("7")))
        .extracting(GrantedAuthority::getAuthority)
        .containsExactly("crm:read");
  }

  @Test
  void convert_noTenantClaim_resolvesWithNullTenant_empty() {
    given(authorizationResolver.permissionCodes(null, "u")).willReturn(Set.of());

    assertThat(converter.convert(jwt(null))).isEmpty();
  }
}
