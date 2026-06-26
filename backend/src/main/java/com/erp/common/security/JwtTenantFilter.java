package com.erp.common.security;

import com.erp.common.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
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
                    MDC.put("tenantId", String.valueOf(tenantId));
                } else {
                    log.warn("JWT has no tenant_id claim — sub={}", jwt.getSubject());
                }
                // 로그 상관용으로 sub(userId)만 — 이름·이메일 등 개인정보는 MDC에 넣지 않는다.
                String userId = jwt.getSubject();
                if (userId != null) {
                    MDC.put("userId", userId);
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
            MDC.remove("userId");
            MDC.remove("tenantId");
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
