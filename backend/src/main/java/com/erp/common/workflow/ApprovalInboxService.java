package com.erp.common.workflow;

import com.erp.common.security.CurrentUserProvider;
import com.erp.common.workflow.dto.ApprovalSummaryResponse;
import com.erp.common.workflow.repository.ApprovalRequestRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 통합 결재함 — 여러 모듈의 결재 요청을 한 곳에서 조회한다.
 * 읽기 전용: 실제 승인/반려는 각 도메인(모듈) 서비스가 소유한다(모듈 경계 유지).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApprovalInboxService {

    private final ApprovalRequestRepository approvalRequestRepository;
    private final CurrentUserProvider currentUserProvider;
    // 역할/한도 기반 결재(AP 전표 전결 등)를 결재함에 기여하는 모듈별 포트. 없으면 빈 리스트.
    private final List<PendingApprovalContributor> pendingContributors;

    /**
     * 현재 사용자가 처리해야 할 대기 결재 목록. 사람 단위 결재선(현재 단계 결재자=나)에
     * 더해, 역할·금액 한도 기반 결재(모듈 기여분)를 합친다. (entityType,entityId)로 중복 제거.
     */
    public List<ApprovalSummaryResponse> pendingForCurrentUser() {
        String userId = currentUserProvider.getCurrentUserId();
        if (userId == null) {
            return List.of();
        }
        Map<String, ApprovalSummaryResponse> merged = new LinkedHashMap<>();
        approvalRequestRepository.findPendingForApprover(userId).stream()
                .map(ApprovalSummaryResponse::from)
                .forEach(s -> merged.put(s.entityType() + ":" + s.entityId(), s));
        for (PendingApprovalContributor contributor : pendingContributors) {
            for (ApprovalSummaryResponse s : contributor.pendingForCurrentUser()) {
                merged.putIfAbsent(s.entityType() + ":" + s.entityId(), s);
            }
        }
        return List.copyOf(merged.values());
    }

    /** 현재 사용자가 상신한 결재 요청 목록(상태 추적용). */
    public List<ApprovalSummaryResponse> requestedByCurrentUser() {
        String userId = currentUserProvider.getCurrentUserId();
        if (userId == null) {
            return List.of();
        }
        return approvalRequestRepository.findByRequesterIdOrderByRequestedAtDesc(userId).stream()
                .map(ApprovalSummaryResponse::from).toList();
    }
}
