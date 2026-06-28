package com.erp.common.userdirectory;

import com.erp.common.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 인증된 요청마다 현재 사용자(JWT 클레임)를 user_directory에 upsert한다 — sub→이름 미러를 점진 누적. {@link
 * com.erp.common.security.JwtTenantFilter} 다음에 위치해 TenantContext가 채워진 상태에서 실행된다. 스프링 빈이 아니다(서블릿
 * 자동등록 회피) — {@link com.erp.common.security.SecurityConfig}가 직접 생성·체인에 추가한다(!test 프로파일).
 */
@Slf4j
@RequiredArgsConstructor
public class UserDirectorySyncFilter extends OncePerRequestFilter {

  private final UserDirectoryService userDirectoryService;

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    try {
      Authentication auth = SecurityContextHolder.getContext().getAuthentication();
      Long tenantId = TenantContext.getTenantId();
      if (tenantId != null && auth != null && auth.getPrincipal() instanceof Jwt jwt) {
        String displayName =
            firstNonBlank(
                jwt.getClaimAsString("name"),
                jwt.getClaimAsString("preferred_username"),
                jwt.getSubject());
        userDirectoryService.sync(
            tenantId, jwt.getSubject(), displayName, jwt.getClaimAsString("email"));
      }
    } catch (RuntimeException e) {
      // 미러 동기화 실패가 본 요청을 깨뜨리지 않도록 흡수한다(표시 캐시는 best-effort).
      log.warn("user_directory 동기화 실패 — 요청은 계속 진행", e);
    }
    filterChain.doFilter(request, response);
  }

  private static String firstNonBlank(String... values) {
    for (String v : values) {
      if (v != null && !v.isBlank()) {
        return v;
      }
    }
    return null;
  }
}
