package com.erp.finance.application.service;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.security.CurrentUserProvider;
import com.erp.common.security.Permission;
import com.erp.common.workflow.ApprovalRequest;
import com.erp.common.workflow.repository.ApprovalRequestRepository;
import com.erp.finance.application.dto.ArInvoiceCreateRequest;
import com.erp.finance.application.dto.ArInvoicePayRequest;
import com.erp.finance.application.dto.ArInvoiceResponse;
import com.erp.finance.domain.model.ArInvoice;
import com.erp.finance.domain.model.Customer;
import com.erp.finance.domain.repository.ArInvoiceRepository;
import com.erp.finance.domain.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ArInvoiceServiceTest {

    @Mock private ArInvoiceRepository arInvoiceRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private ApprovalRequestRepository approvalRequestRepository;
    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private com.erp.common.security.PermissionChecker permissionChecker;
    @Mock private com.erp.common.security.ApprovalAuthorityProvider approvalAuthorityProvider;
    @Mock private com.erp.common.audit.AuditService auditService;
    @Mock private com.erp.finance.domain.repository.AccountRepository accountRepository;
    @Mock private ArInvoicePostingService arInvoicePostingService;

    @InjectMocks
    private ArInvoiceService arInvoiceService;

    private Customer buildCustomer() {
        return Customer.of("C001", "고객사", null, null, null, null, 30);
    }

    private ArInvoice buildInvoice() {
        return ArInvoice.create("AR-001", buildCustomer(),
            LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31),
            new BigDecimal("100000"), "KRW", null);
    }

    @Test
    void create_valid_returnsArInvoiceResponse() {
        given(arInvoiceRepository.existsByInvoiceNo("AR-001")).willReturn(false);
        given(customerRepository.findById(1L)).willReturn(Optional.of(buildCustomer()));
        ArInvoice invoice = buildInvoice();
        given(arInvoiceRepository.save(any())).willReturn(invoice);

        ArInvoiceResponse result = arInvoiceService.create(
            new ArInvoiceCreateRequest("AR-001", 1L,
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31),
                new BigDecimal("100000"), "KRW", null, null));

        assertThat(result.invoiceNo()).isEqualTo("AR-001");
        assertThat(result.totalAmount()).isEqualByComparingTo("100000");
    }

    @Test
    void create_duplicateInvoiceNo_throwsInvoiceNoDuplicate() {
        given(arInvoiceRepository.existsByInvoiceNo("AR-001")).willReturn(true);

        ErpException ex = assertThrows(ErpException.class, () ->
            arInvoiceService.create(new ArInvoiceCreateRequest("AR-001", 1L,
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31),
                new BigDecimal("100000"), "KRW", null, null)));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVOICE_NO_DUPLICATE);
    }

    @Test
    void submit_draftInvoice_changeStatusToPendingApproval() {
        ArInvoice invoice = buildInvoice();
        given(arInvoiceRepository.findById(1L)).willReturn(Optional.of(invoice));
        given(currentUserProvider.getCurrentUserId()).willReturn("user-1");
        given(approvalRequestRepository.save(any(ApprovalRequest.class)))
            .willAnswer(inv -> inv.getArgument(0));

        ArInvoiceResponse result = arInvoiceService.submit(1L);

        assertThat(result.status().name()).isEqualTo("PENDING_APPROVAL");
    }

    @Test
    void approve_pendingInvoice_changeStatusToApproved() {
        ArInvoice invoice = buildInvoice();
        invoice.submit();
        given(arInvoiceRepository.findById(1L)).willReturn(Optional.of(invoice));
        given(currentUserProvider.getCurrentUserId()).willReturn("user-1");
        // 전결 한도(1백만) ≥ 전표 금액(10만) → 결재 가능
        given(approvalAuthorityProvider.getApprovalLimit()).willReturn(new BigDecimal("1000000"));

        ArInvoiceResponse result = arInvoiceService.approve(1L);

        assertThat(result.status().name()).isEqualTo("APPROVED");
        verify(permissionChecker).require(Permission.FINANCE_INVOICE_APPROVE);
    }

    @Test
    void approve_amountExceedsApprovalLimit_throwsLimitExceeded() {
        // 전결규정: 전결 한도(5만)를 초과하는 전표(10만)는 상위 전결권자만 결재 가능.
        ArInvoice invoice = buildInvoice();
        invoice.submit();
        given(arInvoiceRepository.findById(1L)).willReturn(Optional.of(invoice));
        given(currentUserProvider.getCurrentUserId()).willReturn("user-1");
        given(approvalAuthorityProvider.getApprovalLimit()).willReturn(new BigDecimal("50000"));

        ErpException ex = assertThrows(ErpException.class, () -> arInvoiceService.approve(1L));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.APPROVAL_LIMIT_EXCEEDED);
        assertThat(invoice.getStatus().name()).isEqualTo("PENDING_APPROVAL");
    }

    @Test
    void approve_unauthenticated_throwsApproverNotAuthorized() {
        given(currentUserProvider.getCurrentUserId()).willReturn(null);

        ErpException ex = assertThrows(ErpException.class, () -> arInvoiceService.approve(1L));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.APPROVER_NOT_AUTHORIZED);
    }

    @Test
    void pay_fullPayment_changeStatusToPaid() {
        // 직무분리: 수금자(receiver-1)는 작성자(createdBy=null)와 다른 사용자
        ArInvoice invoice = buildInvoice();
        invoice.submit();
        invoice.approve();
        given(arInvoiceRepository.findById(1L)).willReturn(Optional.of(invoice));
        given(currentUserProvider.getCurrentUserId()).willReturn("receiver-1");

        ArInvoiceResponse result = arInvoiceService.pay(1L, new ArInvoicePayRequest(new BigDecimal("100000"), null, null));

        assertThat(result.status().name()).isEqualTo("PAID");
        assertThat(result.outstandingAmount()).isEqualByComparingTo("0");
    }

    @Test
    void pay_overpayment_throwsInvoiceOverpayment() {
        ArInvoice invoice = buildInvoice();
        invoice.submit();
        invoice.approve();
        given(arInvoiceRepository.findById(1L)).willReturn(Optional.of(invoice));
        given(currentUserProvider.getCurrentUserId()).willReturn("receiver-1");

        ErpException ex = assertThrows(ErpException.class, () ->
            arInvoiceService.pay(1L, new ArInvoicePayRequest(new BigDecimal("200000"), null, null)));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVOICE_OVERPAYMENT);
    }

    @Test
    void pay_bySelfCreator_throwsPaymentSelfForbidden() {
        // 직무분리(SoD): 본인이 작성한 전표는 수금(현금 유입) 처리 불가
        ArInvoice invoice = buildInvoice();
        invoice.submit();
        invoice.approve();
        ReflectionTestUtils.setField(invoice, "createdBy", "user-1");
        given(arInvoiceRepository.findById(1L)).willReturn(Optional.of(invoice));
        given(currentUserProvider.getCurrentUserId()).willReturn("user-1");

        ErpException ex = assertThrows(ErpException.class, () ->
            arInvoiceService.pay(1L, new ArInvoicePayRequest(new BigDecimal("100000"), null, null)));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_SELF_FORBIDDEN);
        assertThat(invoice.getStatus().name()).isEqualTo("APPROVED");
    }

    @Test
    void cancel_paidInvoice_throwsInvoiceAlreadyProcessed() {
        ArInvoice invoice = buildInvoice();
        invoice.submit();
        invoice.approve();
        invoice.pay(new BigDecimal("100000"));
        given(arInvoiceRepository.findById(1L)).willReturn(Optional.of(invoice));

        ErpException ex = assertThrows(ErpException.class, () -> arInvoiceService.cancel(1L));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVOICE_ALREADY_PROCESSED);
    }

    @Test
    void findById_notFound_throwsInvoiceNotFound() {
        given(arInvoiceRepository.findById(99L)).willReturn(Optional.empty());

        ErpException ex = assertThrows(ErpException.class, () -> arInvoiceService.findById(99L));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVOICE_NOT_FOUND);
    }
}
