package com.erp.common.workflow;

import com.erp.common.AbstractIntegrationTest;
import com.erp.common.workflow.repository.ApprovalRequestRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class ApprovalInboxQueryIntegrationTest extends AbstractIntegrationTest {

    @Autowired private ApprovalRequestRepository approvalRequestRepository;

    private ApprovalRequest pending(String entityType, Long entityId, String requester, String... stepApprovers) {
        ApprovalStep[] steps = new ApprovalStep[stepApprovers.length];
        for (int i = 0; i < stepApprovers.length; i++) {
            steps[i] = ApprovalStep.of(i + 1, "단계 " + (i + 1), stepApprovers[i]);
        }
        return approvalRequestRepository.save(
                ApprovalRequest.create(entityType, entityId, entityType + " 결재", requester,
                        new ArrayList<>(List.of(steps))));
    }

    @Test
    void findPendingForApprover_returnsOnlyRequestsWhereCurrentStepApproverMatches() {
        // A: 1단계, 현재 결재자 = approver-1 → 포함
        pending("LEAVE_REQUEST", 1L, "requester-x", "approver-1");
        // B: 1단계, 현재 결재자 = approver-2 → 제외
        pending("LEAVE_REQUEST", 2L, "requester-x", "approver-2");
        // C: 2단계, 1단계 승인 후 현재 단계(2)=approver-1 → 포함
        ApprovalRequest c = pending("AP_INVOICE", 3L, "requester-y", "approver-9", "approver-1");
        c.approve("approver-9", "1단계 승인");
        approvalRequestRepository.save(c);
        // D: 완료(APPROVED) — approver-1이 단계 결재자지만 PENDING 아님 → 제외
        ApprovalRequest d = pending("LEAVE_REQUEST", 4L, "requester-z", "approver-1");
        d.approve("approver-1", "완료");
        approvalRequestRepository.save(d);

        List<ApprovalRequest> result = approvalRequestRepository.findPendingForApprover("approver-1");

        assertThat(result).extracting(ApprovalRequest::getEntityId)
                .containsExactlyInAnyOrder(1L, 3L);
        assertThat(result).allMatch(ApprovalRequest::isPending);
    }

    @Test
    void findByRequesterIdOrderByRequestedAtDesc_returnsOnlyMyRequests() {
        pending("LEAVE_REQUEST", 10L, "me", "approver-1");
        pending("AP_INVOICE", 11L, "me", "approver-2");
        pending("LEAVE_REQUEST", 12L, "someone-else", "approver-1");

        List<ApprovalRequest> mine = approvalRequestRepository
                .findByRequesterIdOrderByRequestedAtDesc("me");

        assertThat(mine).extracting(ApprovalRequest::getEntityId)
                .containsExactlyInAnyOrder(10L, 11L);
    }
}
