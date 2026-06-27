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
import com.erp.finance.application.dto.JournalEntryCreateRequest;
import com.erp.finance.application.dto.JournalEntryResponse;
import com.erp.finance.application.dto.JournalLineRequest;
import com.erp.finance.application.dto.JournalLineResponse;
import com.erp.finance.domain.model.Account;
import com.erp.finance.domain.model.FiscalPeriod;
import com.erp.finance.domain.model.JournalEntry;
import com.erp.finance.domain.model.JournalLine;
import com.erp.finance.domain.repository.AccountRepository;
import com.erp.finance.domain.repository.FiscalPeriodRepository;
import com.erp.finance.domain.repository.JournalEntryRepository;
import com.erp.finance.domain.repository.JournalLineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JournalEntryService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int ENTRY_NO_MAX_RETRIES = 10;
    private static final int ENTRY_NO_SUFFIX_MIN = 10000;
    private static final int ENTRY_NO_SUFFIX_MAX = 99999;

    private final JournalEntryRepository journalEntryRepository;
    private final JournalLineRepository journalLineRepository;
    private final FiscalPeriodRepository fiscalPeriodRepository;
    private final AccountRepository accountRepository;
    private final CurrentUserProvider currentUserProvider;
    private final PermissionChecker permissionChecker;
    private final ApprovalRequestRepository approvalRequestRepository;
    private final ApprovalAuthorityProvider approvalAuthorityProvider;
    private final AuditService auditService;
    private final CurrencyConverter currencyConverter;

    // 전결규정상 결재자는 전결권·한도로 결정되므로 결재선에 특정인을 사전 지정하지 않는다(역할 sentinel).
    private static final String ROLE_BASED_APPROVER = "@role:" + Permission.FINANCE_GL_APPROVE;

    public JournalEntryResponse findById(Long id) {
        permissionChecker.require(Permission.FINANCE_READ);
        return JournalEntryResponse.from(getOrThrow(id));
    }

    public PageResponse<JournalEntryResponse> findByFiscalPeriod(Long fiscalPeriodId, Pageable pageable) {
        permissionChecker.require(Permission.FINANCE_READ);
        return PageResponse.from(
            journalEntryRepository.findByFiscalPeriodId(fiscalPeriodId, pageable)
                .map(JournalEntryResponse::from));
    }

    public List<JournalLineResponse> findLines(Long journalEntryId) {
        permissionChecker.require(Permission.FINANCE_READ);
        getOrThrow(journalEntryId);
        return journalLineRepository.findByJournalEntryIdOrderByLineNoAsc(journalEntryId).stream()
            .map(JournalLineResponse::from)
            .toList();
    }

    @Transactional
    public JournalEntryResponse create(JournalEntryCreateRequest request) {
        permissionChecker.require(Permission.FINANCE_WRITE);
        return createInternal(request);
    }

    /**
     * 권한 게이트 없는 DRAFT 분개 생성 — finance 모듈 내부의 시스템 자동 전기(AP 전표 승인 등)용.
     * 결재 승인이 분개 생성을 인가하므로 결재자에게 finance:write를 요구하지 않는다(package-private).
     */
    @Transactional
    JournalEntryResponse createInternal(JournalEntryCreateRequest request) {
        FiscalPeriod period = fiscalPeriodRepository.findById(request.fiscalPeriodId())
            .orElseThrow(() -> new ErpException(ErrorCode.FISCAL_PERIOD_NOT_FOUND));

        if (!period.isOpen()) {
            throw new ErpException(ErrorCode.FISCAL_PERIOD_CLOSED);
        }
        if (request.entryDate().isBefore(period.getStartDate()) || request.entryDate().isAfter(period.getEndDate())) {
            throw new ErpException(ErrorCode.JOURNAL_ENTRY_DATE_OUT_OF_RANGE);
        }

        String entryNo = generateEntryNo(request.entryDate());
        JournalEntry entry = JournalEntry.create(entryNo, request.entryDate(), period,
            request.description(), request.entryType(), request.currency());

        int lineNo = 1;
        for (JournalLineRequest lineReq : request.lines()) {
            Account account = accountRepository.findById(lineReq.accountId())
                .orElseThrow(() -> new ErpException(ErrorCode.ACCOUNT_NOT_FOUND));
            account.assertPostable();
            JournalLine line = JournalLine.of(entry, lineNo++, account,
                lineReq.debitAmount(), lineReq.creditAmount(), lineReq.description(), lineReq.departmentId());
            entry.addLine(line);
        }

        if (!entry.isBalanced()) {
            throw new ErpException(ErrorCode.JOURNAL_ENTRY_NOT_BALANCED);
        }

        // 거래 시점 FX 스냅샷 — 전표일 환율로 차변합계를 기준통화 환산해 저장. 환율 부재 시 미산정(null)(AC-11).
        currencyConverter.tryConvert(entry.getTotalDebit(), entry.getCurrency(), entry.getEntryDate())
            .ifPresent(c -> entry.applyBaseSnapshot(c.baseAmount(), c.rate()));

        JournalEntry saved = journalEntryRepository.save(entry);
        log.atInfo().addKeyValue("event", "JOURNAL_ENTRY_CREATED")
            .addKeyValue("journalEntryId", saved.getId())
            .addKeyValue("entryNo", saved.getEntryNo())
            .addKeyValue("totalDebit", saved.getTotalDebit())
            .log("전표 생성");
        return JournalEntryResponse.from(saved);
    }

    /**
     * 결재 상신: DRAFT → PENDING_APPROVAL + ApprovalRequest(GL_ENTRY) 생성·링크.
     * 차대변 균형·회계기간 검증은 도메인({@link JournalEntry#submitForApproval})에서 수행.
     * 결재함 라우팅은 {@link GlEntryApprovalInboxContributor}가 전결권·한도로 산출한다(역할 sentinel).
     */
    @Transactional
    public JournalEntryResponse submitForApproval(Long id) {
        permissionChecker.require(Permission.FINANCE_WRITE);
        String userId = currentUserProvider.getCurrentUserId();
        JournalEntry entry = getOrThrow(id);
        entry.submitForApproval();
        ApprovalStep step = ApprovalStep.of(1, "GL 전표 전기 승인", ROLE_BASED_APPROVER);
        ApprovalRequest approvalRequest = ApprovalRequest.create(
            ReferenceTypes.GL_ENTRY, entry.getId(),
            "GL 전표 전기 승인: " + entry.getEntryNo(),
            userId, new ArrayList<>(List.of(step)));
        ApprovalRequest saved = approvalRequestRepository.save(approvalRequest);
        entry.linkApprovalRequest(saved.getId());
        log.atInfo().addKeyValue("event", "JOURNAL_ENTRY_SUBMITTED")
            .addKeyValue("journalEntryId", entry.getId())
            .addKeyValue("entryNo", entry.getEntryNo())
            .log("전표 결재 상신");
        return JournalEntryResponse.from(entry);
    }

    /**
     * 전기 결재: 전결권(finance:gl:approve) 보유자만, 작성자≠결재자·차변≤전결한도 충족 시
     * ApprovalRequest 승인 후 PENDING_APPROVAL → POSTED 전기. 작성권(finance:write)과 분리.
     */
    @Transactional
    public JournalEntryResponse approve(Long id) {
        permissionChecker.require(Permission.FINANCE_GL_APPROVE);
        String userId = currentUserProvider.getCurrentUserId();
        if (userId == null) {
            throw new ErpException(ErrorCode.APPROVER_NOT_AUTHORIZED);
        }
        JournalEntry entry = getOrThrow(id);
        // 직무분리: 본인이 작성한 전표는 전기 결재할 수 없다.
        if (userId.equals(entry.getCreatedBy())) {
            throw new ErpException(ErrorCode.APPROVER_NOT_AUTHORIZED);
        }
        // 전결규정(위임전결): 차변 합계가 본인 전결 한도를 초과하면 상위 전결권자만 결재 가능.
        if (entry.getTotalDebit().compareTo(approvalAuthorityProvider.getApprovalLimit()) > 0) {
            throw new ErpException(ErrorCode.APPROVAL_LIMIT_EXCEEDED);
        }
        if (entry.getApprovalRequestId() != null) {
            ApprovalRequest approvalRequest = approvalRequestRepository
                .findById(entry.getApprovalRequestId())
                .orElseThrow(() -> new ErpException(ErrorCode.APPROVAL_NOT_FOUND));
            approvalRequest.approve(userId, null);
        }
        entry.post(userId);
        auditService.record(ReferenceTypes.GL_ENTRY, entry.getId(), AuditLog.AuditAction.APPROVE, null, null);
        log.atInfo().addKeyValue("event", "JOURNAL_ENTRY_POSTED")
            .addKeyValue("journalEntryId", entry.getId())
            .addKeyValue("entryNo", entry.getEntryNo())
            .addKeyValue("totalDebit", entry.getTotalDebit())
            .log("전표 승인·전기 완료");
        return JournalEntryResponse.from(entry);
    }

    /**
     * 전기 결재 반려: 전결권(finance:gl:approve) 보유자만, 작성자≠결재자 충족 시 반려 사유와 함께
     * ApprovalRequest를 REJECTED로 종료하고 전표를 PENDING_APPROVAL → DRAFT로 되돌린다(수정·재상신 가능).
     */
    @Transactional
    public JournalEntryResponse reject(Long id, String comment) {
        permissionChecker.require(Permission.FINANCE_GL_APPROVE);
        String userId = currentUserProvider.getCurrentUserId();
        if (userId == null) {
            throw new ErpException(ErrorCode.APPROVER_NOT_AUTHORIZED);
        }
        if (comment == null || comment.isBlank()) {
            throw new ErpException(ErrorCode.INVALID_INPUT);
        }
        JournalEntry entry = getOrThrow(id);
        // 직무분리: 본인이 작성한 전표는 반려(결재 행위)할 수 없다 — 철회(withdraw)로 처리한다.
        if (userId.equals(entry.getCreatedBy())) {
            throw new ErpException(ErrorCode.APPROVER_NOT_AUTHORIZED);
        }
        entry.returnToDraft();
        if (entry.getApprovalRequestId() != null) {
            ApprovalRequest approvalRequest = approvalRequestRepository
                .findById(entry.getApprovalRequestId())
                .orElseThrow(() -> new ErpException(ErrorCode.APPROVAL_NOT_FOUND));
            approvalRequest.reject(userId, comment);
        }
        auditService.record(ReferenceTypes.GL_ENTRY, entry.getId(), AuditLog.AuditAction.REJECT, null, null);
        log.atInfo().addKeyValue("event", "JOURNAL_ENTRY_REJECTED")
            .addKeyValue("journalEntryId", entry.getId())
            .addKeyValue("entryNo", entry.getEntryNo())
            .log("전표 결재 반려");
        return JournalEntryResponse.from(entry);
    }

    /**
     * 전기 결재 철회: 상신자 본인(작성권 finance:write)만, ApprovalRequest를 CANCELLED로 종료하고
     * 전표를 PENDING_APPROVAL → DRAFT로 되돌린다(수정·재상신 가능). 타인은 철회할 수 없다.
     */
    @Transactional
    public JournalEntryResponse withdraw(Long id) {
        permissionChecker.require(Permission.FINANCE_WRITE);
        String userId = currentUserProvider.getCurrentUserId();
        JournalEntry entry = getOrThrow(id);
        // 상신자 본인만 철회 가능.
        if (userId == null || !userId.equals(entry.getCreatedBy())) {
            throw new ErpException(ErrorCode.FORBIDDEN);
        }
        entry.returnToDraft();
        if (entry.getApprovalRequestId() != null) {
            ApprovalRequest approvalRequest = approvalRequestRepository
                .findById(entry.getApprovalRequestId())
                .orElseThrow(() -> new ErpException(ErrorCode.APPROVAL_NOT_FOUND));
            approvalRequest.cancel(userId, null);
        }
        auditService.record(ReferenceTypes.GL_ENTRY, entry.getId(), AuditLog.AuditAction.WITHDRAW, null, null);
        log.atInfo().addKeyValue("event", "JOURNAL_ENTRY_WITHDRAWN")
            .addKeyValue("journalEntryId", entry.getId())
            .addKeyValue("entryNo", entry.getEntryNo())
            .log("전표 결재 철회");
        return JournalEntryResponse.from(entry);
    }

    private JournalEntry getOrThrow(Long id) {
        return journalEntryRepository.findById(id)
            .orElseThrow(() -> new ErpException(ErrorCode.JOURNAL_ENTRY_NOT_FOUND));
    }

    private String generateEntryNo(LocalDate date) {
        String base = "JE-" + date.format(DATE_FMT) + "-";
        for (int i = 0; i < ENTRY_NO_MAX_RETRIES; i++) {
            int suffix = ThreadLocalRandom.current().nextInt(ENTRY_NO_SUFFIX_MIN, ENTRY_NO_SUFFIX_MAX + 1);
            String entryNo = base + suffix;
            if (!journalEntryRepository.existsByEntryNo(entryNo)) {
                return entryNo;
            }
        }
        throw new ErpException(ErrorCode.INTERNAL_ERROR);
    }
}
