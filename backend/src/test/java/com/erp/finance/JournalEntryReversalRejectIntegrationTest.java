package com.erp.finance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.erp.common.AbstractIntegrationTest;
import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.security.DataScope;
import com.erp.common.security.UserAccessProfile;
import com.erp.common.security.UserAccessProfileRepository;
import com.erp.common.workflow.ApprovalStatus;
import com.erp.common.workflow.repository.ApprovalRequestRepository;
import com.erp.finance.application.ReferenceTypes;
import com.erp.finance.application.dto.JournalEntryCreateRequest;
import com.erp.finance.application.dto.JournalEntryResponse;
import com.erp.finance.application.dto.JournalLineRequest;
import com.erp.finance.application.dto.JournalLineResponse;
import com.erp.finance.application.service.JournalEntryService;
import com.erp.finance.domain.model.Account;
import com.erp.finance.domain.model.AccountType;
import com.erp.finance.domain.model.FiscalPeriod;
import com.erp.finance.domain.model.FiscalYear;
import com.erp.finance.domain.model.JournalEntryStatus;
import com.erp.finance.domain.model.JournalEntryType;
import com.erp.finance.domain.model.NormalBalance;
import com.erp.finance.domain.repository.AccountRepository;
import com.erp.finance.domain.repository.FiscalPeriodRepository;
import com.erp.finance.domain.repository.FiscalYearRepository;
import com.erp.finance.domain.repository.JournalEntryRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * GL 전표 역분개(reverse)·반려(reject) 검증. 역분개: POSTED 전표를 차/대 뒤집은 새 전표로 상쇄(균형 보존), 원 전표 REVERSED. 반려:
 * PENDING_APPROVAL 전표를 사유와 함께 DRAFT로 되돌리고 ApprovalRequest를 REJECTED로 종료.
 */
@Transactional
class JournalEntryReversalRejectIntegrationTest extends AbstractIntegrationTest {

  @Autowired private JournalEntryService journalEntryService;
  @Autowired private JournalEntryRepository journalEntryRepository;
  @Autowired private AccountRepository accountRepository;
  @Autowired private FiscalYearRepository fiscalYearRepository;
  @Autowired private FiscalPeriodRepository fiscalPeriodRepository;
  @Autowired private UserAccessProfileRepository accessProfileRepository;
  @Autowired private ApprovalRequestRepository approvalRequestRepository;

  private Long periodId;
  private Long debitAccountId;
  private Long creditAccountId;

  @BeforeEach
  void setUp() {
    FiscalYear fy =
        fiscalYearRepository.save(
            FiscalYear.of(2025, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31)));
    periodId =
        fiscalPeriodRepository
            .save(FiscalPeriod.of(fy, 1, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31)))
            .getId();
    debitAccountId =
        accountRepository
            .save(
                Account.of("51100", "소모품비", AccountType.EXPENSE, NormalBalance.DEBIT, null, false))
            .getId();
    creditAccountId =
        accountRepository
            .save(
                Account.of(
                    "25100", "외상매입금", AccountType.LIABILITY, NormalBalance.CREDIT, null, false))
            .getId();
  }

  private void authenticate(String sub, BigDecimal approvalLimit, String... authorities) {
    authenticate(sub, authorities);
    accessProfileRepository
        .findByTenantIdAndUserId(TEST_TENANT_ID, sub)
        .map(
            p -> {
              p.update(DataScope.ALL, null, approvalLimit);
              return p;
            })
        .orElseGet(
            () ->
                accessProfileRepository.save(
                    UserAccessProfile.of(TEST_TENANT_ID, sub, DataScope.ALL, null, approvalLimit)));
  }

  private JournalEntryResponse createDraftAsCreator() {
    authenticate("creator", BigDecimal.ZERO, "finance:write");
    return journalEntryService.create(
        new JournalEntryCreateRequest(
            LocalDate.of(2025, 1, 10),
            periodId,
            "수동 전표",
            JournalEntryType.MANUAL,
            "KRW",
            List.of(
                new JournalLineRequest(
                    debitAccountId, new BigDecimal("100000"), BigDecimal.ZERO, "차변", null),
                new JournalLineRequest(
                    creditAccountId, BigDecimal.ZERO, new BigDecimal("100000"), "대변", null))));
  }

  private JournalEntryResponse createAndPost() {
    var created = createDraftAsCreator();
    journalEntryService.submitForApproval(created.id());
    authenticate("approver", new BigDecimal("1000000"), "finance:gl:approve");
    return journalEntryService.approve(created.id());
  }

  // 역분개: POSTED 전표 → 차/대 뒤집은 새 전표 생성·균형, 원 전표 REVERSED, 새 전표 POSTED·참조 링크
  @Test
  void reversePostedEntry_createsBalancedSwappedEntryAndMarksOriginalReversed() {
    var posted = createAndPost();

    authenticate("approver", new BigDecimal("1000000"), "finance:read", "finance:gl:approve");
    var reversal = journalEntryService.reverse(posted.id());

    // 새 역분개 전표는 전기(POSTED)·균형, 원 전표와 다른 전표
    assertThat(reversal.id()).isNotEqualTo(posted.id());
    assertThat(reversal.status()).isEqualTo(JournalEntryStatus.POSTED);
    assertThat(reversal.totalDebit()).isEqualByComparingTo(posted.totalDebit());
    assertThat(reversal.totalCredit()).isEqualByComparingTo(posted.totalCredit());
    assertThat(reversal.referenceType()).isEqualTo(ReferenceTypes.GL_REVERSAL);
    assertThat(reversal.referenceId()).isEqualTo(posted.id());

    // 원 전표는 REVERSED
    var original = journalEntryRepository.findById(posted.id()).orElseThrow();
    assertThat(original.getStatus()).isEqualTo(JournalEntryStatus.REVERSED);

    // 라인 차/대가 정확히 뒤집힘
    List<JournalLineResponse> originalLines = journalEntryService.findLines(posted.id());
    List<JournalLineResponse> reversalLines = journalEntryService.findLines(reversal.id());
    assertThat(reversalLines).hasSameSizeAs(originalLines);
    for (JournalLineResponse ol : originalLines) {
      var match =
          reversalLines.stream().filter(rl -> rl.accountId().equals(ol.accountId())).findFirst();
      assertThat(match).isPresent();
      assertThat(match.get().debitAmount()).isEqualByComparingTo(ol.creditAmount());
      assertThat(match.get().creditAmount()).isEqualByComparingTo(ol.debitAmount());
    }
  }

  // 역분개 대상은 POSTED만 — DRAFT 전표는 역분개 불가
  @Test
  void reverseDraftEntry_throwsNotPosted() {
    var created = createDraftAsCreator();

    authenticate("approver", new BigDecimal("1000000"), "finance:gl:approve");
    ErpException ex =
        assertThrows(ErpException.class, () -> journalEntryService.reverse(created.id()));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.JOURNAL_ENTRY_NOT_POSTED);
    assertThat(journalEntryRepository.findById(created.id()).orElseThrow().getStatus())
        .isEqualTo(JournalEntryStatus.DRAFT);
  }

  // 역분개는 전기 권한(finance:gl:approve) 필요 — 미보유 시 차단
  @Test
  void reverseWithoutGlApprove_throwsForbidden() {
    var posted = createAndPost();

    authenticate("clerk", BigDecimal.ZERO, "finance:write");
    ErpException ex =
        assertThrows(ErpException.class, () -> journalEntryService.reverse(posted.id()));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
    assertThat(journalEntryRepository.findById(posted.id()).orElseThrow().getStatus())
        .isEqualTo(JournalEntryStatus.POSTED);
  }

  // 반려: PENDING_APPROVAL 전표 → DRAFT, ApprovalRequest REJECTED
  @Test
  void rejectPendingEntry_returnsToDraftAndRejectsRequest() {
    var created = createDraftAsCreator();
    journalEntryService.submitForApproval(created.id());

    authenticate("approver", new BigDecimal("1000000"), "finance:gl:approve");
    var rejected = journalEntryService.reject(created.id(), "증빙 불충분");

    assertThat(rejected.status()).isEqualTo(JournalEntryStatus.DRAFT);
    var je = journalEntryRepository.findById(created.id()).orElseThrow();
    assertThat(je.getStatus()).isEqualTo(JournalEntryStatus.DRAFT);
    assertThat(
            approvalRequestRepository.findById(je.getApprovalRequestId()).orElseThrow().getStatus())
        .isEqualTo(ApprovalStatus.REJECTED);
  }

  // 반려는 전기 권한(finance:gl:approve) 필요 — 미보유 시 차단
  @Test
  void rejectWithoutGlApprove_throwsForbidden() {
    var created = createDraftAsCreator();
    journalEntryService.submitForApproval(created.id());

    authenticate("clerk", BigDecimal.ZERO, "finance:write");
    ErpException ex =
        assertThrows(ErpException.class, () -> journalEntryService.reject(created.id(), "사유"));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
  }
}
