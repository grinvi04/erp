package com.erp.crm.application.service;

import com.erp.common.security.CurrentUserProvider;
import com.erp.common.security.DataScope;
import com.erp.common.security.DataScopeProvider;
import com.erp.crm.domain.repository.SalesTeamMemberRepository;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * CRM 영업데이터(owner 기준) 조회의 데이터 스코프 해석 — 모든 CRM 조회 진입점이 공통 사용
 * (auth-standards: 화면별 수작업 필터 금지). ALL=narrowing 없음, SELF=본인 owner, DEPARTMENT=
 * 소속 영업팀(들)의 팀원 owner 합집합(미소속=본인).
 */
@Component
@RequiredArgsConstructor
public class CrmDataScopeResolver {

    private final DataScopeProvider dataScopeProvider;
    private final CurrentUserProvider currentUserProvider;
    private final SalesTeamMemberRepository memberRepository;

    /**
     * owner 스코프. {@code scoped=false}면 필터 없음(ALL). {@code scoped=true}면 {@code ownerIds}에
     * 포함된 owner만. search 쿼리는 {@code (:scoped = false OR x.ownerId IN :ownerIds)}로 적용한다.
     */
    public record OwnerScope(boolean scoped, Set<String> ownerIds) {}

    public OwnerScope ownerScope() {
        DataScope scope = dataScopeProvider.getDataScope();
        return switch (scope) {
            case ALL -> new OwnerScope(false, Set.of());
            case SELF -> {
                String sub = currentUserProvider.getCurrentUserId();
                yield new OwnerScope(true, sub == null ? Set.of() : Set.of(sub));
            }
            case DEPARTMENT -> {
                String sub = currentUserProvider.getCurrentUserId();
                if (sub == null) {
                    yield new OwnerScope(true, Set.of());
                }
                Set<String> ids = new HashSet<>(memberRepository.findTeammateUserIds(sub));
                ids.add(sub); // 팀 미소속이어도 본인 데이터는 보장
                yield new OwnerScope(true, ids);
            }
        };
    }
}
