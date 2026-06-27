package com.erp.finance.application.service;

import com.erp.finance.application.ReferenceTypes;
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
import com.erp.finance.domain.model.Account;
import com.erp.finance.domain.model.ApInvoice;
import com.erp.finance.domain.model.ApInvoiceStatus;
import com.erp.finance.domain.model.Vendor;
import com.erp.finance.domain.repository.AccountRepository;
import com.erp.finance.domain.repository.ApInvoiceRepository;
import com.erp.finance.domain.repository.VendorRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
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
    private final AccountRepository accountRepository;
    private final ApInvoicePostingService apInvoicePostingService;
    private final CurrencyConverter currencyConverter;

    // 전결규정상 결재자는 전결권·한도로 결정되므로 결재선에 특정인을 사전 지정하지 않는다(역할 sentinel).
    private static final String ROLE_BASED_APPROVER = "@role:" + Permission.FINANCE_INVOICE_APPROVE;

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
        addLines(invoice, request);
        // 거래 시점 FX 스냅샷 — 송장일 환율로 기준통화 환산액·환율 저장. 환율 부재 시 미산정(null) 유지(AC-11).
        currencyConverter.tryConvert(invoice.getTotalAmount(), invoice.getCurrency(), invoice.getInvoiceDate())
            .ifPresent(c -> invoice.applyBaseSnapshot(c.baseAmount(), c.rate()));
        return ApInvoiceResponse.from(apInvoiceRepository.save(invoice));
    }

    /** 차변 라인 추가(있으면) — 계정 검증 + 합계가 totalAmount와 일치하는지 확인. */
    private void addLines(ApInvoice invoice, ApInvoiceCreateRequest request) {
        if (request.lines() == null || request.lines().isEmpty()) {
            return;
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (var lineReq : request.lines()) {
            Account account = accountRepository.findById(lineReq.accountId())
                .orElseThrow(() -> new ErpException(ErrorCode.ACCOUNT_NOT_FOUND));
            account.assertPostable();
            invoice.addLine(account, lineReq.amount(), lineReq.description());
            sum = sum.add(lineReq.amount());
        }
        if (sum.compareTo(request.totalAmount()) != 0) {
            throw new ErpException(ErrorCode.INVALID_INPUT);
        }
    }

    @Transactional
    public ApInvoiceResponse submit(Long id) {
        permissionChecker.require(Permission.FINANCE_WRITE);
        String userId = currentUserProvider.getCurrentUserId();
        ApInvoice invoice = getOrThrow(id);
        invoice.submit();
        // 전결규정(역할/한도 기반)이라 특정 결재자를 결재선에 사전 지정하지 않는다 —
        // 결재함 라우팅은 ApInvoiceApprovalInboxContributor가 전결권·한도로 산출한다.
        // 사람 단위 결재함(findPendingForApprover)에 작성자로 잘못 노출되지 않도록 역할 sentinel.
        ApprovalStep step = ApprovalStep.of(1, "AP 전표 승인", ROLE_BASED_APPROVER);
        ApprovalRequest approvalRequest = ApprovalRequest.create(
            ReferenceTypes.AP_INVOICE, invoice.getId(),
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
        // 승인 → GL 자동 분개(DRAFT). 라인·공급업체 외상매입금계정이 있을 때만 생성·연결.
        Long journalEntryId = apInvoicePostingService.postDraft(invoice);
        if (journalEntryId != null) {
            invoice.linkJournalEntry(journalEntryId);
        }
        auditService.record(ReferenceTypes.AP_INVOICE, invoice.getId(),
            AuditLog.AuditAction.APPROVE, null, null);
        log.atInfo().addKeyValue("event", "AP_INVOICE_APPROVED")
            .addKeyValue("apInvoiceId", invoice.getId())
            .addKeyValue("invoiceNo", invoice.getInvoiceNo())
            .addKeyValue("totalAmount", invoice.getTotalAmount())
            .log("AP 인보이스 승인");
        return ApInvoiceResponse.from(invoice);
    }

    @Transactional
    public ApInvoiceResponse pay(Long id, ApInvoicePayRequest request) {
        permissionChecker.require(Permission.FINANCE_INVOICE_PAY);
        ApInvoice invoice = getOrThrow(id);
        // 직무분리(SoD): 본인이 작성한 전표는 지급(현금 유출) 처리할 수 없다.
        String userId = currentUserProvider.getCurrentUserId();
        if (userId == null || userId.equals(invoice.getCreatedBy())) {
            throw new ErpException(ErrorCode.PAYMENT_SELF_FORBIDDEN);
        }
        // 전결규정(위임전결): 지급 금액이 본인 전결 한도를 초과하면 상위 전결권자만 지급 가능.
        if (request.amount().compareTo(approvalAuthorityProvider.getApprovalLimit()) > 0) {
            throw new ErpException(ErrorCode.APPROVAL_LIMIT_EXCEEDED);
        }
        invoice.pay(request.amount());
        // 지급계정 제공 시 지급 분개 자동 생성 (차)외상매입금 /(대)현금·예금.
        if (request.cashAccountId() != null) {
            Account cashAccount = accountRepository.findById(request.cashAccountId())
                .orElseThrow(() -> new ErpException(ErrorCode.ACCOUNT_NOT_FOUND));
            cashAccount.assertPostable();
            java.time.LocalDate paymentDate = request.paymentDate() != null
                ? request.paymentDate() : invoice.getInvoiceDate();
            apInvoicePostingService.postPaymentDraft(invoice, request.amount(), cashAccount, paymentDate);
        }
        log.atInfo().addKeyValue("event", "AP_INVOICE_PAID")
            .addKeyValue("apInvoiceId", invoice.getId())
            .addKeyValue("invoiceNo", invoice.getInvoiceNo())
            .addKeyValue("paidAmount", request.amount())
            .log("AP 인보이스 지급");
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
