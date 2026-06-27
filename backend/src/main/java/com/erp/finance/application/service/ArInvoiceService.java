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
import com.erp.finance.application.dto.ArInvoiceCreateRequest;
import com.erp.finance.application.dto.ArInvoicePayRequest;
import com.erp.finance.application.dto.ArInvoiceResponse;
import com.erp.finance.domain.model.Account;
import com.erp.finance.domain.model.ArInvoice;
import com.erp.finance.domain.model.ArInvoiceStatus;
import com.erp.finance.domain.model.Customer;
import com.erp.finance.domain.repository.AccountRepository;
import com.erp.finance.domain.repository.ArInvoiceRepository;
import com.erp.finance.domain.repository.CustomerRepository;
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
public class ArInvoiceService {

    private final ArInvoiceRepository arInvoiceRepository;
    private final CustomerRepository customerRepository;
    private final ApprovalRequestRepository approvalRequestRepository;
    private final CurrentUserProvider currentUserProvider;
    private final PermissionChecker permissionChecker;
    private final ApprovalAuthorityProvider approvalAuthorityProvider;
    private final AuditService auditService;
    private final AccountRepository accountRepository;
    private final ArInvoicePostingService arInvoicePostingService;
    private final CurrencyConverter currencyConverter;

    // 전결규정상 결재자는 전결권·한도로 결정되므로 결재선에 특정인을 사전 지정하지 않는다(역할 sentinel).
    private static final String ROLE_BASED_APPROVER = "@role:" + Permission.FINANCE_INVOICE_APPROVE;

    public ArInvoiceResponse findById(Long id) {
        permissionChecker.require(Permission.FINANCE_READ);
        return ArInvoiceResponse.from(getOrThrow(id));
    }

    public PageResponse<ArInvoiceResponse> findAll(ArInvoiceStatus status, Pageable pageable) {
        permissionChecker.require(Permission.FINANCE_READ);
        if (status != null) {
            return PageResponse.from(arInvoiceRepository.findByStatus(status, pageable).map(ArInvoiceResponse::from));
        }
        return PageResponse.from(arInvoiceRepository.findAll(pageable).map(ArInvoiceResponse::from));
    }

    public PageResponse<ArInvoiceResponse> findByCustomer(Long customerId, Pageable pageable) {
        permissionChecker.require(Permission.FINANCE_READ);
        return PageResponse.from(arInvoiceRepository.findByCustomerId(customerId, pageable).map(ArInvoiceResponse::from));
    }

    @Transactional
    public ArInvoiceResponse create(ArInvoiceCreateRequest request) {
        permissionChecker.require(Permission.FINANCE_WRITE);
        if (arInvoiceRepository.existsByInvoiceNo(request.invoiceNo())) {
            throw new ErpException(ErrorCode.INVOICE_NO_DUPLICATE);
        }
        Customer customer = customerRepository.findById(request.customerId())
            .orElseThrow(() -> new ErpException(ErrorCode.CUSTOMER_NOT_FOUND));
        ArInvoice invoice = ArInvoice.create(request.invoiceNo(), customer, request.invoiceDate(),
            request.dueDate(), request.totalAmount(), request.currency(), request.note());
        addLines(invoice, request);
        // 거래 시점 FX 스냅샷 — 송장일 환율로 기준통화 환산액·환율 저장. 환율 부재 시 미산정(null) 유지(AC-11).
        currencyConverter.tryConvert(invoice.getTotalAmount(), invoice.getCurrency(), invoice.getInvoiceDate())
            .ifPresent(c -> invoice.applyBaseSnapshot(c.baseAmount(), c.rate()));
        return ArInvoiceResponse.from(arInvoiceRepository.save(invoice));
    }

    /** 대변 라인 추가(있으면) — 계정 검증 + 합계가 totalAmount와 일치하는지 확인. */
    private void addLines(ArInvoice invoice, ArInvoiceCreateRequest request) {
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
    public ArInvoiceResponse submit(Long id) {
        permissionChecker.require(Permission.FINANCE_WRITE);
        String userId = currentUserProvider.getCurrentUserId();
        ArInvoice invoice = getOrThrow(id);
        invoice.submit();
        ApprovalStep step = ApprovalStep.of(1, "AR 전표 승인", ROLE_BASED_APPROVER);
        ApprovalRequest approvalRequest = ApprovalRequest.create(
            "AR_INVOICE", invoice.getId(),
            "AR 전표 승인: " + invoice.getInvoiceNo(),
            userId, new ArrayList<>(List.of(step))
        );
        ApprovalRequest saved = approvalRequestRepository.save(approvalRequest);
        invoice.linkApprovalRequest(saved.getId());
        return ArInvoiceResponse.from(invoice);
    }

    @Transactional
    public ArInvoiceResponse approve(Long id) {
        // 전결권(결재 권한) 보유자만 결재할 수 있다 — 전표 작성권(finance:write)과 분리.
        permissionChecker.require(Permission.FINANCE_INVOICE_APPROVE);
        String userId = currentUserProvider.getCurrentUserId();
        if (userId == null) {
            throw new ErpException(ErrorCode.APPROVER_NOT_AUTHORIZED);
        }
        ArInvoice invoice = getOrThrow(id);
        // 직무분리: 본인이 작성한 전표는 결재할 수 없다.
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
        // 승인 → GL 자동 분개(DRAFT). 라인·고객 외상매출금계정이 있을 때만 생성·연결.
        Long journalEntryId = arInvoicePostingService.postDraft(invoice);
        if (journalEntryId != null) {
            invoice.linkJournalEntry(journalEntryId);
        }
        auditService.record("AR_INVOICE", invoice.getId(),
            AuditLog.AuditAction.APPROVE, null, null);
        log.atInfo().addKeyValue("event", "AR_INVOICE_APPROVED")
            .addKeyValue("arInvoiceId", invoice.getId())
            .addKeyValue("invoiceNo", invoice.getInvoiceNo())
            .addKeyValue("totalAmount", invoice.getTotalAmount())
            .log("AR 인보이스 승인");
        return ArInvoiceResponse.from(invoice);
    }

    @Transactional
    public ArInvoiceResponse pay(Long id, ArInvoicePayRequest request) {
        permissionChecker.require(Permission.FINANCE_INVOICE_PAY);
        ArInvoice invoice = getOrThrow(id);
        // 직무분리(SoD): 본인이 작성한 전표는 수금(현금 유입) 처리할 수 없다.
        // 수금은 현금 유입이므로 지출권한(전결한도)은 무의미 — 작성자 차단만 적용한다.
        String userId = currentUserProvider.getCurrentUserId();
        if (userId == null || userId.equals(invoice.getCreatedBy())) {
            throw new ErpException(ErrorCode.PAYMENT_SELF_FORBIDDEN);
        }
        invoice.pay(request.amount());
        // 수금계정 제공 시 수금 분개 자동 생성 (차)현금·예금 /(대)외상매출금.
        if (request.cashAccountId() != null) {
            Account cashAccount = accountRepository.findById(request.cashAccountId())
                .orElseThrow(() -> new ErpException(ErrorCode.ACCOUNT_NOT_FOUND));
            cashAccount.assertPostable();
            java.time.LocalDate paymentDate = request.paymentDate() != null
                ? request.paymentDate() : invoice.getInvoiceDate();
            arInvoicePostingService.postPaymentDraft(invoice, request.amount(), cashAccount, paymentDate);
        }
        log.atInfo().addKeyValue("event", "AR_INVOICE_COLLECTED")
            .addKeyValue("arInvoiceId", invoice.getId())
            .addKeyValue("invoiceNo", invoice.getInvoiceNo())
            .addKeyValue("collectedAmount", request.amount())
            .log("AR 인보이스 수금");
        return ArInvoiceResponse.from(invoice);
    }

    @Transactional
    public ArInvoiceResponse cancel(Long id) {
        permissionChecker.require(Permission.FINANCE_WRITE);
        ArInvoice invoice = getOrThrow(id);
        invoice.cancel();
        return ArInvoiceResponse.from(invoice);
    }

    private ArInvoice getOrThrow(Long id) {
        return arInvoiceRepository.findById(id)
            .orElseThrow(() -> new ErpException(ErrorCode.INVOICE_NOT_FOUND));
    }
}
