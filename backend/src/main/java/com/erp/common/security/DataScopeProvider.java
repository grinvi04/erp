package com.erp.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * 현재 사용자의 데이터 스코프 정책·소속을 JWT 클레임에서 읽는다(Keycloak이 발급).
 * 클레임: {@code data_scope}(ALL|DEPARTMENT|SELF), {@code department_id}(소속 부서).
 * 미설정·비인증 시 {@link DataScope#ALL}(narrowing 없음).
 */
@Component
public class DataScopeProvider {

    public DataScope getDataScope() {
        Jwt jwt = currentJwt();
        if (jwt == null) {
            return DataScope.ALL;
        }
        String raw = jwt.getClaimAsString("data_scope");
        if (raw == null) {
            return DataScope.ALL;
        }
        try {
            return DataScope.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return DataScope.ALL;
        }
    }

    public Long getDepartmentId() {
        Jwt jwt = currentJwt();
        if (jwt == null) {
            return null;
        }
        Object claim = jwt.getClaim("department_id");
        if (claim instanceof Long l) {
            return l;
        }
        if (claim instanceof Integer i) {
            return i.longValue();
        }
        if (claim instanceof String s && !s.isBlank()) {
            try {
                return Long.parseLong(s.trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private Jwt currentJwt() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        return auth.getPrincipal() instanceof Jwt jwt ? jwt : null;
    }
}
