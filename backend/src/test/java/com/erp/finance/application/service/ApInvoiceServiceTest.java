package com.erp.finance.application.service;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.security.CurrentUserProvider;
import com.erp.common.security.Permission;
import com.erp.common.workflow.ApprovalRequest;
import com.erp.common.workflow.repository.ApprovalRequestRepository;
import com.erp.finance.application.dto.ApInvoiceCreateRequest;
import com.erp.finance.application.dto.ApInvoicePayRequest;
import com.erp.finance.application.dto.ApInvoiceResponse;
import com.erp.finance.domain.model.ApInvoice;
import com.erp.finance.domain.model.Vendor;
import com.erp.finance.domain.repository.ApInvoiceRepository;
import com.erp.finance.domain.repository.VendorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ApInvoiceServiceTest {

    @Mock private ApInvoiceRepository apInvoiceRepository;
    @Mock private VendorRepository vendorRepository;
    @Mock private ApprovalRequestRepository approvalRequestRepository;
    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private com.erp.common.security.PermissionChecker permissionChecker;
    @Mock private com.erp.common.security.ApprovalAuthorityProvider approvalAuthorityProvider;
    @Mock private com.erp.common.audit.AuditService auditService;
    @Mock private com.erp.finance.domain.repository.AccountRepository accountRepository;
    @Mock private ApInvoicePostingService apInvoicePostingService;

    @InjectMocks
    private ApInvoiceService apInvoiceService;

    private Vendor buildVendor() {
        return Vendor.of("V001", "공급사", null, null, null, null, 30);
    }

    private ApInvoice buildInvoice() {
        return ApInvoice.create("INV-001", buildVendor(),
            LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31),
            new BigDecimal("100000"), "KRW", null);
    }

    @Test
    void create_valid_returnsApInvoiceResponse() {
        given(apInvoiceRepository.existsByInvoiceNo("INV-001")).willReturn(false);
        given(vendorRepository.findById(1L)).willReturn(Optional.of(buildVendor()));
        ApInvoice invoice = buildInvoice();
        given(apInvoiceRepository.save(any())).willReturn(invoice);

        ApInvoiceResponse result = apInvoiceService.create(
            new ApInvoiceCreateRequest("INV-001", 1L,
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31),
                new BigDecimal("100000"), "KRW", null, null));

        assertThat(result.invoiceNo()).isEqualTo("INV-001");
        assertThat(result.totalAmount()).isEqualByComparingTo("100000");
    }

    @Test
    void create_duplicateInvoiceNo_throwsInvoiceNoDuplicate() {
        given(apInvoiceRepository.existsByInvoiceNo("INV-001")).willReturn(true);

        ErpException ex = assertThrows(ErpException.class, () ->
            apInvoiceService.create(new ApInvoiceCreateRequest("INV-001", 1L,
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31),
                new BigDecimal("100000"), "KRW", null, null)));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVOICE_NO_DUPLICATE);
    }

    @Test
    void submit_draftInvoice_changeStatusToPendingApproval() {
        ApInvoice invoice = buildInvoice();
        given(apInvoiceRepository.findById(1L)).willReturn(Optional.of(invoice));
        given(currentUserProvider.getCurrentUserId()).willReturn("user-1");
        given(approvalRequestRepository.save(any(ApprovalRequest.class)))
            .willAnswer(inv -> inv.getArgument(0));

        ApInvoiceResponse result = apInvoiceService.submit(1L);

        assertThat(result.status().name()).isEqualTo("PENDING_APPROVAL");
    }

    @Test
    void approve_pendingInvoice_changeStatusToApproved() {
        ApInvoice invoice = buildInvoice();
        invoice.submit();
        given(apInvoiceRepository.findById(1L)).willReturn(Optional.of(invoice));
        given(currentUserProvider.getCurrentUserId()).willReturn("user-1");
        // 전결 한도(1백만) ≥ 전표 금액(10만) → 결재 가능
        given(approvalAuthorityProvider.getApprovalLimit()).willReturn(new BigDecimal("1000000"));

        ApInvoiceResponse result = apInvoiceService.approve(1L);

        assertThat(result.status().name()).isEqualTo("APPROVED");
        verify(permissionChecker).require(Permission.FINANCE_INVOICE_APPROVE);
    }

    @Test
    void approve_amountExceedsApprovalLimit_throwsLimitExceeded() {
        // 전결규정: 전결 한도(5만)를 초과하는 전표(10만)는 상위 전결권자만 결재 가능.
        ApInvoice invoice = buildInvoice();
        invoice.submit();
        given(apInvoiceRepository.findById(1L)).willReturn(Optional.of(invoice));
        given(currentUserProvider.getCurrentUserId()).willReturn("user-1");
        given(approvalAuthorityProvider.getApprovalLimit()).willReturn(new BigDecimal("50000"));

        ErpException ex = assertThrows(ErpException.class, () -> apInvoiceService.approve(1L));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.APPROVAL_LIMIT_EXCEEDED);
        assertThat(invoice.getStatus().name()).isEqualTo("PENDING_APPROVAL");
    }

    @Test
    void approve_unauthenticated_throwsApproverNotAuthorized() {
        given(currentUserProvider.getCurrentUserId()).willReturn(null);

        ErpException ex = assertThrows(ErpException.class, () -> apInvoiceService.approve(1L));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.APPROVER_NOT_AUTHORIZED);
    }

    @Test
    void pay_fullPayment_changeStatusToPaid() {
        ApInvoice invoice = buildInvoice();
        invoice.submit();
        invoice.approve();
        given(apInvoiceRepository.findById(1L)).willReturn(Optional.of(invoice));

        ApInvoiceResponse result = apInvoiceService.pay(1L, new ApInvoicePayRequest(new BigDecimal("100000")));

        assertThat(result.status().name()).isEqualTo("PAID");
        assertThat(result.outstandingAmount()).isEqualByComparingTo("0");
    }

    @Test
    void pay_overpayment_throwsInvoiceOverpayment() {
        ApInvoice invoice = buildInvoice();
        invoice.submit();
        invoice.approve();
        given(apInvoiceRepository.findById(1L)).willReturn(Optional.of(invoice));

        ErpException ex = assertThrows(ErpException.class, () ->
            apInvoiceService.pay(1L, new ApInvoicePayRequest(new BigDecimal("200000"))));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVOICE_OVERPAYMENT);
    }

    @Test
    void cancel_paidInvoice_throwsInvoiceAlreadyProcessed() {
        ApInvoice invoice = buildInvoice();
        invoice.submit();
        invoice.approve();
        invoice.pay(new BigDecimal("100000"));
        given(apInvoiceRepository.findById(1L)).willReturn(Optional.of(invoice));

        ErpException ex = assertThrows(ErpException.class, () -> apInvoiceService.cancel(1L));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVOICE_ALREADY_PROCESSED);
    }

    @Test
    void findById_notFound_throwsInvoiceNotFound() {
        given(apInvoiceRepository.findById(99L)).willReturn(Optional.empty());

        ErpException ex = assertThrows(ErpException.class, () -> apInvoiceService.findById(99L));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVOICE_NOT_FOUND);
    }
}
