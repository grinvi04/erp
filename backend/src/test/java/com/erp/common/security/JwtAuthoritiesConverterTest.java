package com.erp.common.security;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthoritiesConverterTest {

    private final JwtAuthoritiesConverter converter = new JwtAuthoritiesConverter();

    private Jwt jwtWithClaim(String name, Object value) {
        return Jwt.withTokenValue("t").header("alg", "none").subject("u").claim(name, value).build();
    }

    @Test
    void convert_permissionsClaim_mapsToAuthorities() {
        var authorities = converter.convert(
                jwtWithClaim("permissions", List.of("hr:employee:write", "finance:read")));

        assertThat(authorities).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("hr:employee:write", "finance:read");
    }

    @Test
    void convert_noPermissionsClaim_returnsEmpty() {
        assertThat(converter.convert(jwtWithClaim("sub", "u"))).isEmpty();
    }

    @Test
    void convert_nonStringEntries_areIgnored() {
        var authorities = converter.convert(
                jwtWithClaim("permissions", List.of("hr:employee:read", 123, true)));

        assertThat(authorities).extracting(GrantedAuthority::getAuthority)
                .containsExactly("hr:employee:read");
    }
}
