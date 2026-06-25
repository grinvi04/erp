package com.erp.common.security;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * JWT의 {@code permissions} 클레임(권한 코드 목록)을 Spring authority로 변환한다.
 * Keycloak이 역할→권한 매핑을 발급해 이 클레임에 담는다(코드는 권한만 검사 — auth-standards).
 * authority 이름 = 권한 코드 그대로(예: {@code hr:employee:write}).
 */
public class JwtAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Object claim = jwt.getClaim("permissions");
        if (!(claim instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }
}
