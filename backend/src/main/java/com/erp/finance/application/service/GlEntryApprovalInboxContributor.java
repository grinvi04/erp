package com.erp.finance.application.service;

import com.erp.finance.application.ReferenceTypes;
import com.erp.common.security.ApprovalAuthorityProvider;
import com.erp.common.security.CurrentUserProvider;
import com.erp.common.security.Permission;
import com.erp.common.security.PermissionChecker;
import com.erp.common.workflow.ApprovalStatus;
import com.erp.common.workflow.PendingApprovalContributor;
import com.erp.common.workflow.dto.ApprovalSummaryResponse;
import com.erp.finance.domain.model.JournalEntry;
import com.erp.finance.domain.model.JournalEntryStatus;
import com.erp.finance.domain.repository.JournalEntryRepository;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * GL 전표 전기 결재를 통합 결재함에 기여한다. 전결권(finance:gl:approve) 보유자에게
 * 자기 전결 한도 내·본인 작성 아닌 대기(PENDING_APPROVAL) 전표를 보여준다 —
 * {@link JournalEntryService#approve}의 결재 권한 기준과 동일하게 산출한다.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GlEntryApprovalInboxContributor implements PendingApprovalContributor {

    private final JournalEntryRepository journalEntryRepository;
    private final CurrentUserProvider currentUserProvider;
    private final PermissionChecker permissionChecker;
    private final ApprovalAuthorityProvider approvalAuthorityProvider;

    @Override
    public List<ApprovalSummaryResponse> pendingForCurrentUser() {
        String userId = currentUserProvider.getCurrentUserId();
        if (userId == null || !permissionChecker.hasPermission(Permission.FINANCE_GL_APPROVE)) {
            return List.of();
        }
        BigDecimal limit = approvalAuthorityProvider.getApprovalLimit();
        if (limit.signum() <= 0) {
            return List.of();
        }
        return journalEntryRepository
                .findPendingApprovableBy(JournalEntryStatus.PENDING_APPROVAL, userId, limit)
                .stream()
                .map(this::toSummary)
                .toList();
    }

    private ApprovalSummaryResponse toSummary(JournalEntry je) {
        return new ApprovalSummaryResponse(
                je.getApprovalRequestId(), ReferenceTypes.GL_ENTRY, je.getId(),
                "GL 전표 전기 승인: " + je.getEntryNo(),
                ApprovalStatus.PENDING, je.getCreatedBy(),
                1, 1, "GL 전표 전기 승인", null,
                je.getCreatedAt(), null);
    }
}
