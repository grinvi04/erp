package com.erp.finance.application.service;

import com.erp.common.audit.AuditLog;
import com.erp.common.audit.AuditService;
import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.response.PageResponse;
import com.erp.common.security.ApprovalAuthorityProvider;
import com.erp.common.security.CurrentUserProvider;
import com.erp.common.security.Permission;
import com.erp.common.security.PermissionChecker;
import com.erp.common.workflow.ApprovalRequest;
import com.erp.common.workflow.ApprovalStep;
import com.erp.common.workflow.repository.ApprovalRequestRepository;
import com.erp.finance.application.dto.ApInvoiceCreateRequest;
import com.erp.finance.application.dto.ApInvoicePayRequest;
import com.erp.finance.application.dto.ApInvoiceResponse;
import com.erp.finance.domain.model.ApInvoice;
import com.erp.finance.domain.model.ApInvoiceStatus;
import com.erp.finance.domain.model.Vendor;
import com.erp.finance.domain.repository.ApInvoiceRepository;
import com.erp.finance.domain.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApInvoiceService {

    private final ApInvoiceRepository apInvoiceRepository;
    private final VendorRepository vendorRepository;
    private final ApprovalRequestRepository approvalRequestRepository;
    private final CurrentUserProvider currentUserProvider;
    private final PermissionChecker permissionChecker;
    private final ApprovalAuthorityProvider approvalAuthorityProvider;
    private final AuditService auditService;

    public ApInvoiceResponse findById(Long id) {
        permissionChecker.require(Permission.FINANCE_READ);
        return ApInvoiceResponse.from(getOrThrow(id));
    }

    public PageResponse<ApInvoiceResponse> findAll(ApInvoiceStatus status, Pageable pageable) {
        permissionChecker.require(Permission.FINANCE_READ);
        if (status != null) {
            return PageResponse.from(apInvoiceRepository.findByStatus(status, pageable).map(ApInvoiceResponse::from));
        }
        return PageResponse.from(apInvoiceRepository.findAll(pageable).map(ApInvoiceResponse::from));
    }

    public PageResponse<ApInvoiceResponse> findByVendor(Long vendorId, Pageable pageable) {
        permissionChecker.require(Permission.FINANCE_READ);
        return PageResponse.from(apInvoiceRepository.findByVendorId(vendorId, pageable).map(ApInvoiceResponse::from));
    }

    @Transactional
    public ApInvoiceResponse create(ApInvoiceCreateRequest request) {
        permissionChecker.require(Permission.FINANCE_WRITE);
        if (apInvoiceRepository.existsByInvoiceNo(request.invoiceNo())) {
            throw new ErpException(ErrorCode.INVOICE_NO_DUPLICATE);
        }
        Vendor vendor = vendorRepository.findById(request.vendorId())
            .orElseThrow(() -> new ErpException(ErrorCode.VENDOR_NOT_FOUND));
        ApInvoice invoice = ApInvoice.create(request.invoiceNo(), vendor, request.invoiceDate(),
            request.dueDate(), request.totalAmount(), request.currency(), request.note());
        return ApInvoiceResponse.from(apInvoiceRepository.save(invoice));
    }

    @Transactional
    public ApInvoiceResponse submit(Long id) {
        permissionChecker.require(Permission.FINANCE_WRITE);
        String userId = currentUserProvider.getCurrentUserId();
        ApInvoice invoice = getOrThrow(id);
        invoice.submit();
        ApprovalStep step = ApprovalStep.of(1, "AP 전표 승인", userId);
        ApprovalRequest approvalRequest = ApprovalRequest.create(
            "AP_INVOICE", invoice.getId(),
            "AP 전표 승인: " + invoice.getInvoiceNo(),
            userId, new ArrayList<>(List.of(step))
        );
        ApprovalRequest saved = approvalRequestRepository.save(approvalRequest);
        invoice.linkApprovalRequest(saved.getId());
        return ApInvoiceResponse.from(invoice);
    }

    @Transactional
    public ApInvoiceResponse approve(Long id) {
        // 전결권(결재 권한) 보유자만 결재할 수 있다 — 전표 작성권(finance:write)과 분리.
        permissionChecker.require(Permission.FINANCE_INVOICE_APPROVE);
        String userId = currentUserProvider.getCurrentUserId();
        if (userId == null) {
            throw new ErpException(ErrorCode.APPROVER_NOT_AUTHORIZED);
        }
        ApInvoice invoice = getOrThrow(id);
        // 직무분리(직무 분리): 본인이 작성한 전표는 결재할 수 없다.
        if (userId.equals(invoice.getCreatedBy())) {
            throw new ErpException(ErrorCode.APPROVER_NOT_AUTHORIZED);
        }
        // 전결규정(위임전결): 본인의 전결 한도를 초과하는 금액은 상위 전결권자만 결재 가능.
        if (invoice.getTotalAmount().compareTo(approvalAuthorityProvider.getApprovalLimit()) > 0) {
            throw new ErpException(ErrorCode.APPROVAL_LIMIT_EXCEEDED);
        }
        invoice.approve();
        if (invoice.getApprovalRequestId() != null) {
            ApprovalRequest approvalRequest = approvalRequestRepository
                .findById(invoice.getApprovalRequestId())
                .orElseThrow(() -> new ErpException(ErrorCode.APPROVAL_NOT_FOUND));
            approvalRequest.approve(userId, null);
        }
        auditService.record("AP_INVOICE", invoice.getId(),
            AuditLog.AuditAction.APPROVE, null, null);
        return ApInvoiceResponse.from(invoice);
    }

    @Transactional
    public ApInvoiceResponse pay(Long id, ApInvoicePayRequest request) {
        permissionChecker.require(Permission.FINANCE_WRITE);
        ApInvoice invoice = getOrThrow(id);
        invoice.pay(request.amount());
        return ApInvoiceResponse.from(invoice);
    }

    @Transactional
    public ApInvoiceResponse cancel(Long id) {
        permissionChecker.require(Permission.FINANCE_WRITE);
        ApInvoice invoice = getOrThrow(id);
        invoice.cancel();
        return ApInvoiceResponse.from(invoice);
    }

    private ApInvoice getOrThrow(Long id) {
        return apInvoiceRepository.findById(id)
            .orElseThrow(() -> new ErpException(ErrorCode.INVOICE_NOT_FOUND));
    }
}
