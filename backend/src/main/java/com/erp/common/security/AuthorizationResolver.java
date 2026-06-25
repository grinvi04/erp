package com.erp.common.security;

import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인가(authorization) 해석 — (tenant, user) 기준으로 권한 코드와 접근 프로파일을 DB에서 읽는다.
 * auth-standards: 역할·권한·매핑은 DB 정본. Keycloak JWT는 신원(sub·tenant_id)만 제공한다.
 *
 * <p>tenant_id를 명시 인자로 받는다 — 권한 코드 해석은 JWT 인증 단계({@link JwtAuthoritiesConverter})에서
 * TenantContext가 세팅되기 전에 호출되므로 @TenantId 자동필터에 의존할 수 없다.
 */
@Service
@RequiredArgsConstructor
public class AuthorizationResolver {

    private final UserRoleRepository userRoleRepository;
    private final UserAccessProfileRepository accessProfileRepository;

    @Transactional(readOnly = true)
    public Set<String> permissionCodes(Long tenantId, String userId) {
        if (tenantId == null || userId == null) {
            return Set.of();
        }
        return userRoleRepository.findPermissionCodes(tenantId, userId);
    }

    @Transactional(readOnly = true)
    public Optional<UserAccessProfile> accessProfile(Long tenantId, String userId) {
        if (tenantId == null || userId == null) {
            return Optional.empty();
        }
        return accessProfileRepository.findByTenantIdAndUserId(tenantId, userId);
    }
}
