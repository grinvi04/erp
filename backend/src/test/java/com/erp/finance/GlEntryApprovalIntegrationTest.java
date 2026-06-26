package com.erp.finance;

import com.erp.common.AbstractIntegrationTest;
import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.security.DataScope;
import com.erp.common.security.UserAccessProfile;
import com.erp.common.security.UserAccessProfileRepository;
import com.erp.common.workflow.ApprovalInboxService;
import com.erp.common.workflow.ApprovalStatus;
import com.erp.common.workflow.repository.ApprovalRequestRepository;
import com.erp.finance.application.dto.JournalEntryCreateRequest;
import com.erp.finance.application.dto.JournalEntryResponse;
import com.erp.finance.application.dto.JournalLineRequest;
import com.erp.finance.application.service.JournalEntryService;
import com.erp.finance.domain.model.Account;
import com.erp.finance.domain.model.AccountType;
import com.erp.finance.domain.model.FiscalPeriod;
import com.erp.finance.domain.model.FiscalYear;
import com.erp.finance.domain.model.JournalEntryType;
import com.erp.finance.domain.model.NormalBalance;
import com.erp.finance.domain.repository.AccountRepository;
import com.erp.finance.domain.repository.FiscalPeriodRepository;
import com.erp.finance.domain.repository.FiscalYearRepository;
import com.erp.finance.domain.repository.JournalEntryRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * GL 전표 전기 결재선 전체 흐름 검증: DRAFT→상신(PENDING_APPROVAL)→다른 사용자 승인→POSTED.
 * 직무분리(작성자 본인 결재 차단), 전결권 미보유 차단, 직접 전기 차단, 결재함 노출을 함께 검증한다.
 */
@Transactional
class GlEntryApprovalIntegrationTest extends AbstractIntegrationTest {

    @Autowired private JournalEntryService journalEntryService;
    @Autowired private JournalEntryRepository journalEntryRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private FiscalYearRepository fiscalYearRepository;
    @Autowired private FiscalPeriodRepository fiscalPeriodRepository;
    @Autowired private UserAccessProfileRepository accessProfileRepository;
    @Autowired private ApprovalRequestRepository approvalRequestRepository;
    @Autowired private ApprovalInboxService approvalInboxService;

    private Long periodId;
    private Long debitAccountId;
    private Long creditAccountId;

    @BeforeEach
    void setUp() {
        FiscalYear fy = fiscalYearRepository.save(FiscalYear.of(2025,
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31)));
        periodId = fiscalPeriodRepository.save(FiscalPeriod.of(fy, 1,
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31))).getId();
        debitAccountId = accountRepository.save(Account.of("51100", "소모품비",
                AccountType.EXPENSE, NormalBalance.DEBIT, null, false)).getId();
        creditAccountId = accountRepository.save(Account.of("25100", "외상매입금",
                AccountType.LIABILITY, NormalBalance.CREDIT, null, false)).getId();
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(String sub, BigDecimal approvalLimit, String... authorities) {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject(sub)
                .claim("sub", sub).claim("tenant_id", TEST_TENANT_ID).build();
        List<GrantedAuthority> auths = java.util.Arrays.stream(authorities)
                .map(a -> (GrantedAuthority) new SimpleGrantedAuthority(a)).toList();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, auths));
        accessProfileRepository.findByTenantIdAndUserId(TEST_TENANT_ID, sub)
                .map(p -> { p.update(DataScope.ALL, null, approvalLimit); return p; })
                .orElseGet(() -> accessProfileRepository.save(
                        UserAccessProfile.of(TEST_TENANT_ID, sub, DataScope.ALL, null, approvalLimit)));
    }

    private JournalEntryResponse createDraftAsCreator() {
        authenticate("creator", BigDecimal.ZERO, "finance:write");
        return journalEntryService.create(new JournalEntryCreateRequest(
                LocalDate.of(2025, 1, 10), periodId, "수동 전표", JournalEntryType.MANUAL, "KRW",
                List.of(
                        new JournalLineRequest(debitAccountId, new BigDecimal("100000"), BigDecimal.ZERO, "차변", null),
                        new JournalLineRequest(creditAccountId, BigDecimal.ZERO, new BigDecimal("100000"), "대변", null))));
    }

    // AC-1 + AC-3: 상신 → 다른 사용자 승인 → POSTED, ApprovalRequest APPROVED
    @Test
    void submitThenApproveByOther_postsEntryAndApprovesRequest() {
        var created = createDraftAsCreator();
        var submitted = journalEntryService.submitForApproval(created.id());
        assertThat(submitted.status().name()).isEqualTo("PENDING_APPROVAL");

        authenticate("approver", new BigDecimal("1000000"), "finance:gl:approve");
        var approved = journalEntryService.approve(created.id());

        assertThat(approved.status().name()).isEqualTo("POSTED");
        var je = journalEntryRepository.findById(created.id()).orElseThrow();
        assertThat(je.getPostedBy()).isEqualTo("approver");
        assertThat(approvalRequestRepository.findById(je.getApprovalRequestId()).orElseThrow().getStatus())
                .isEqualTo(ApprovalStatus.APPROVED);
    }

    // AC-4: 작성자 본인 결재 차단(직무분리)
    @Test
    void approveBySelfCreator_throwsApproverNotAuthorized() {
        var created = createDraftAsCreator();
        journalEntryService.submitForApproval(created.id());

        // 작성자(creator)에게 전결권을 줘도 본인 작성 전표는 결재 불가
        authenticate("creator", new BigDecimal("1000000"), "finance:gl:approve");
        ErpException ex = assertThrows(ErpException.class, () -> journalEntryService.approve(created.id()));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.APPROVER_NOT_AUTHORIZED);
    }

    // AC-7: 전결권(finance:gl:approve) 미보유 시 결재 차단
    @Test
    void approveWithoutPermission_throwsForbidden() {
        var created = createDraftAsCreator();
        journalEntryService.submitForApproval(created.id());

        authenticate("approver", new BigDecimal("1000000"), "finance:read");
        ErpException ex = assertThrows(ErpException.class, () -> journalEntryService.approve(created.id()));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
    }

    // AC-6: 직접 전기 차단 — 상신하지 않은 DRAFT는 승인(전기) 경로로도 전기 불가
    @Test
    void approveDraftNotSubmitted_throwsNotPendingApproval() {
        var created = createDraftAsCreator();

        authenticate("approver", new BigDecimal("1000000"), "finance:gl:approve");
        ErpException ex = assertThrows(ErpException.class, () -> journalEntryService.approve(created.id()));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.JOURNAL_ENTRY_NOT_PENDING_APPROVAL);
        assertThat(journalEntryRepository.findById(created.id()).orElseThrow().getStatus().name())
                .isEqualTo("DRAFT");
    }

    // AC-8: PENDING_APPROVAL 전표가 전결권 보유·한도 내·작성자≠본인 사용자의 결재함에 노출
    @Test
    void pendingEntry_appearsInApproverInbox() {
        var created = createDraftAsCreator();
        journalEntryService.submitForApproval(created.id());

        authenticate("approver", new BigDecimal("1000000"), "finance:gl:approve");
        var inbox = approvalInboxService.pendingForCurrentUser();

        assertThat(inbox).anyMatch(s -> "GL_ENTRY".equals(s.entityType())
                && s.entityId().equals(created.id()));
    }

    // AC-8(경계): 전결 한도 미달 사용자에게는 결재함에 노출되지 않음
    @Test
    void pendingEntry_notInInboxWhenBelowApprovalLimit() {
        var created = createDraftAsCreator();
        journalEntryService.submitForApproval(created.id());

        authenticate("approver", new BigDecimal("500"), "finance:gl:approve");
        var inbox = approvalInboxService.pendingForCurrentUser();

        assertThat(inbox).noneMatch(s -> s.entityId().equals(created.id()) && "GL_ENTRY".equals(s.entityType()));
    }
}
