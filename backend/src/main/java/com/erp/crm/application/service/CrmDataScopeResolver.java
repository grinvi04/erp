package com.erp.crm.application.service;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
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

    /**
     * 단건 조회(detail) 스코프 검증 — 목록(search)만 좁히면 id 순회로 우회 가능(IDOR)하므로,
     * 상세 조회도 owner가 스코프 밖이면 존재를 드러내지 않도록 RESOURCE_NOT_FOUND로 거부한다.
     * 쓰기(update/delete)는 정책상 스코프 미적용(기능 권한이 통제) — 읽기에만 사용.
     */
    public void requireOwnerAccess(String ownerId) {
        OwnerScope s = ownerScope();
        if (s.scoped() && !s.ownerIds().contains(ownerId)) {
            throw new ErpException(ErrorCode.RESOURCE_NOT_FOUND);
        }
    }
}
