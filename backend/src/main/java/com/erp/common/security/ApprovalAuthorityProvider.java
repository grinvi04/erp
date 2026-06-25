package com.erp.common.security;

import java.math.BigDecimal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * 현재 사용자의 전결 한도(approval limit)를 JWT 클레임에서 읽는다(Keycloak이 발급).
 *
 * <p>한국 기업의 전결규정(위임전결)은 금액 한도로 결재권을 위임한다 — 일정 금액 이하는
 * 하위 전결권자가 최종 결재한다. {@code approval_limit} 클레임은 그 사용자가 결재(전결)할 수
 * 있는 최대 금액(원)이다. 미설정·비인증·파싱 실패 시 0(전결 불가)으로 폴백한다 —
 * fail-closed: 한도가 명시되지 않은 사용자는 금액 결재를 통과시키지 않는다.
 */
@Slf4j
@Component
public class ApprovalAuthorityProvider {

    public BigDecimal getApprovalLimit() {
        Jwt jwt = currentJwt();
        if (jwt == null) {
            return BigDecimal.ZERO;
        }
        Object claim = jwt.getClaim("approval_limit");
        if (claim == null) {
            return BigDecimal.ZERO;
        }
        try {
            if (claim instanceof Number n) {
                return new BigDecimal(n.toString());
            }
            if (claim instanceof String s && !s.isBlank()) {
                return new BigDecimal(s.trim());
            }
        } catch (NumberFormatException e) {
            // 잘못된 클레임 값 — 0으로 폴백하되 경고(Keycloak 매핑 오류 진단용)
            log.warn("알 수 없는 approval_limit 클레임 '{}' — 0(전결 불가)으로 처리", claim);
        }
        return BigDecimal.ZERO;
    }

    private Jwt currentJwt() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        if (auth.getPrincipal() instanceof Jwt jwt) {
            return jwt;
        }
        return null;
    }
}
