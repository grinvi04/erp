package com.erp.common;

import com.erp.common.config.TestSecurityConfig;
import com.erp.common.tenant.TenantContext;
import java.util.Arrays;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
public abstract class AbstractIntegrationTest {

    protected static final Long TEST_TENANT_ID = 1L;

    @BeforeEach
    void setUpTenantContext() {
        TenantContext.setTenantId(TEST_TENANT_ID);
    }

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @AfterEach
    void clearAuthContext() {
        clearAuth();
    }

    /**
     * 테스트용 인증 컨텍스트를 설정한다 — subject·sub·tenant_id 클레임을 가진 JWT와
     * 전달된 권한 코드를 SecurityContext에 넣는다. 데이터 스코프·결재한도 등 DB 프로파일이
     * 필요한 테스트는 이 헬퍼로 신원을 세우고 프로파일 저장은 각 테스트에서 수행한다.
     */
    protected void authenticate(String sub, String... permissions) {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject(sub)
                .claim("sub", sub).claim("tenant_id", TEST_TENANT_ID).build();
        var authorities = Arrays.stream(permissions).map(SimpleGrantedAuthority::new).toList();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, authorities));
    }

    protected void clearAuth() {
        SecurityContextHolder.clearContext();
    }
}
