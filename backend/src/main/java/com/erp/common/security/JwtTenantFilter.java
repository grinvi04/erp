package com.erp.common.security;

import com.erp.common.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT에서 tenant_id를 추출해 TenantContext에 세팅.
 * Spring Security 필터 체인 뒤에 위치 (JWT 인증 완료 후 실행).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTenantFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
                Long tenantId = extractTenantId(jwt);
                if (tenantId != null) {
                    TenantContext.setTenantId(tenantId);
                } else {
                    log.warn("JWT has no tenant_id claim — sub={}", jwt.getSubject());
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

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
                return Long.parseLong(s);
            } catch (NumberFormatException ignored) {
                // non-numeric tenant_id claim — skip
            }
        }
        return null;
    }
}
