package com.erp.finance.application.service;

import com.erp.common.security.ApprovalAuthorityProvider;
import com.erp.common.security.CurrentUserProvider;
import com.erp.common.security.Permission;
import com.erp.common.security.PermissionChecker;
import com.erp.common.workflow.dto.ApprovalSummaryResponse;
import com.erp.finance.domain.model.ApInvoice;
import com.erp.finance.domain.model.ApInvoiceStatus;
import com.erp.finance.domain.repository.ApInvoiceRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ApInvoiceApprovalInboxContributorTest {

    @Mock private ApInvoiceRepository apInvoiceRepository;
    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private PermissionChecker permissionChecker;
    @Mock private ApprovalAuthorityProvider approvalAuthorityProvider;

    @InjectMocks
    private ApInvoiceApprovalInboxContributor contributor;

    @Test
    void noAuthenticatedUser_returnsEmpty_withoutQuerying() {
        given(currentUserProvider.getCurrentUserId()).willReturn(null);

        assertThat(contributor.pendingForCurrentUser()).isEmpty();
        verify(apInvoiceRepository, never()).findPendingApprovableBy(any(), any(), any());
    }

    @Test
    void withoutApprovePermission_returnsEmpty() {
        given(currentUserProvider.getCurrentUserId()).willReturn("approver");
        given(permissionChecker.hasPermission(Permission.FINANCE_INVOICE_APPROVE)).willReturn(false);

        assertThat(contributor.pendingForCurrentUser()).isEmpty();
        verify(apInvoiceRepository, never()).findPendingApprovableBy(any(), any(), any());
    }

    @Test
    void zeroApprovalLimit_returnsEmpty() {
        given(currentUserProvider.getCurrentUserId()).willReturn("approver");
        given(permissionChecker.hasPermission(Permission.FINANCE_INVOICE_APPROVE)).willReturn(true);
        given(approvalAuthorityProvider.getApprovalLimit()).willReturn(BigDecimal.ZERO);

        assertThat(contributor.pendingForCurrentUser()).isEmpty();
        verify(apInvoiceRepository, never()).findPendingApprovableBy(any(), any(), any());
    }

    @Test
    void authorizedApprover_returnsMappedSummaries() {
        given(currentUserProvider.getCurrentUserId()).willReturn("approver");
        given(permissionChecker.hasPermission(Permission.FINANCE_INVOICE_APPROVE)).willReturn(true);
        given(approvalAuthorityProvider.getApprovalLimit()).willReturn(new BigDecimal("2000000"));

        ApInvoice inv = mock(ApInvoice.class);
        given(inv.getId()).willReturn(1L);
        given(inv.getInvoiceNo()).willReturn("INV-1");
        given(inv.getApprovalRequestId()).willReturn(10L);
        given(inv.getCreatedBy()).willReturn("creator");
        given(inv.getCreatedAt()).willReturn(LocalDateTime.of(2026, 1, 1, 9, 0));
        given(apInvoiceRepository.findPendingApprovableBy(
                eq(ApInvoiceStatus.PENDING_APPROVAL), eq("approver"), eq(new BigDecimal("2000000"))))
                .willReturn(List.of(inv));

        List<ApprovalSummaryResponse> result = contributor.pendingForCurrentUser();

        assertThat(result).hasSize(1);
        ApprovalSummaryResponse s = result.get(0);
        assertThat(s.entityType()).isEqualTo("AP_INVOICE");
        assertThat(s.entityId()).isEqualTo(1L);
        assertThat(s.requesterId()).isEqualTo("creator");
        assertThat(s.title()).contains("INV-1");
    }
}
