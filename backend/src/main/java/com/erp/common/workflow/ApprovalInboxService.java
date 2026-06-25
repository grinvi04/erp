package com.erp.common.workflow;

import com.erp.common.security.CurrentUserProvider;
import com.erp.common.workflow.dto.ApprovalSummaryResponse;
import com.erp.common.workflow.repository.ApprovalRequestRepository;
import java.util.List;
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

    /** 현재 사용자가 처리해야 할(현재 단계 결재자=나) 대기 결재 목록. */
    public List<ApprovalSummaryResponse> pendingForCurrentUser() {
        String userId = currentUserProvider.getCurrentUserId();
        if (userId == null) {
            return List.of();
        }
        return approvalRequestRepository.findPendingForApprover(userId).stream()
                .map(ApprovalSummaryResponse::from).toList();
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
