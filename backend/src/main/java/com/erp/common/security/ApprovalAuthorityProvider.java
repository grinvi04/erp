package com.erp.common.security;

import java.math.BigDecimal;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 현재 사용자의 전결 한도(approval limit)를 DB(접근 프로파일)에서 읽는다(auth-standards: DB 관리).
 *
 * <p>한국 기업 전결규정(위임전결)은 금액 한도로 결재권을 위임한다 — 일정 금액 이하는 하위
 * 전결권자가 최종 결재한다. 한도는 그 사용자가 결재(전결)할 수 있는 최대 금액(원)이다.
 * 미설정·비인증 시 0(전결 불가) — fail-closed.
 */
@Component
@RequiredArgsConstructor
public class ApprovalAuthorityProvider {

    private final CurrentUserProvider currentUserProvider;
    private final AuthorizationResolver authorizationResolver;

    public BigDecimal getApprovalLimit() {
        return authorizationResolver.accessProfile(
                currentUserProvider.getCurrentTenantId(), currentUserProvider.getCurrentUserId())
            .map(UserAccessProfile::getApprovalLimit)
            .filter(Objects::nonNull)
            .orElse(BigDecimal.ZERO);
    }
}
