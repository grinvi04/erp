package com.erp.common.security;

import java.util.Collection;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * JWT 신원(sub·tenant_id) → 권한 authority. 권한은 더 이상 JWT 클레임이 아니라 DB(역할→권한)에서
 * 해석한다(auth-standards: DB 관리). authority 이름 = 권한 코드 그대로(예: {@code hr:employee:write}).
 *
 * <p>이 변환은 JWT 인증 단계에서 일어나 TenantContext가 아직 없으므로, JWT의 tenant_id 클레임을
 * 명시적으로 읽어 {@link AuthorizationResolver}에 전달한다.
 *
 * <p>스프링 빈이 아니다(=Converter 빈으로 스캔되지 않음 — @WebMvcTest 슬라이스 오염 방지).
 * {@link SecurityConfig}가 {@code AuthorizationResolver}를 주입해 직접 생성한다(!test 프로파일).
 */
@RequiredArgsConstructor
public class JwtAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private final AuthorizationResolver authorizationResolver;

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Long tenantId = extractTenantId(jwt);
        String userId = jwt.getSubject();
        return authorizationResolver.permissionCodes(tenantId, userId).stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    // JwtTenantFilter와 동일 규칙(tenant_id 클레임은 Long/Integer/String 형태일 수 있음).
    private Long extractTenantId(Jwt jwt) {
        Object claim = jwt.getClaim("tenant_id");
        if (claim instanceof Long l) {
            return l;
        }
        if (claim instanceof Integer i) {
            return i.longValue();
        }
        if (claim instanceof String s) {
            try {
                return Long.parseLong(s.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
