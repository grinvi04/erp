package com.erp.common.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DataScopeProviderTest {

    private final DataScopeProvider provider = new DataScopeProvider();

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private void authWithClaims(Object dataScope, Object departmentId) {
        Jwt.Builder b = Jwt.withTokenValue("t").header("alg", "none").subject("u");
        if (dataScope != null) {
            b.claim("data_scope", dataScope);
        }
        if (departmentId != null) {
            b.claim("department_id", departmentId);
        }
        SecurityContextHolder.getContext()
                .setAuthentication(new JwtAuthenticationToken(b.build(), List.of()));
    }

    @Test
    void getDataScope_departmentClaim_parsed() {
        authWithClaims("DEPARTMENT", 5);
        assertThat(provider.getDataScope()).isEqualTo(DataScope.DEPARTMENT);
        assertThat(provider.getDepartmentId()).isEqualTo(5L);
    }

    @Test
    void getDataScope_caseInsensitive() {
        authWithClaims("self", null);
        assertThat(provider.getDataScope()).isEqualTo(DataScope.SELF);
    }

    @Test
    void getDataScope_noClaim_defaultsToAll() {
        authWithClaims(null, null);
        assertThat(provider.getDataScope()).isEqualTo(DataScope.ALL);
    }

    @Test
    void getDataScope_unauthenticated_defaultsToAll() {
        SecurityContextHolder.clearContext();
        assertThat(provider.getDataScope()).isEqualTo(DataScope.ALL);
        assertThat(provider.getDepartmentId()).isNull();
    }

    @Test
    void getDataScope_invalidClaim_defaultsToAll() {
        authWithClaims("GARBAGE", null);
        assertThat(provider.getDataScope()).isEqualTo(DataScope.ALL);
    }

    @Test
    void getDepartmentId_stringClaim_parsed() {
        authWithClaims("DEPARTMENT", "42");
        assertThat(provider.getDepartmentId()).isEqualTo(42L);
    }
}
