package com.erp.finance.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.erp.common.currency.CurrencyConversionPort.Conversion;
import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.security.CurrentUserProvider;
import com.erp.finance.application.ReferenceTypes;
import com.erp.finance.application.dto.JournalEntryCreateRequest;
import com.erp.finance.application.dto.JournalEntryResponse;
import com.erp.finance.application.dto.JournalLineRequest;
import com.erp.finance.domain.model.Account;
import com.erp.finance.domain.model.AccountType;
import com.erp.finance.domain.model.FiscalPeriod;
import com.erp.finance.domain.model.FiscalYear;
import com.erp.finance.domain.model.JournalEntry;
import com.erp.finance.domain.model.JournalEntryType;
import com.erp.finance.domain.model.NormalBalance;
import com.erp.finance.domain.repository.AccountRepository;
import com.erp.finance.domain.repository.FiscalPeriodRepository;
import com.erp.finance.domain.repository.JournalEntryRepository;
import com.erp.finance.domain.repository.JournalLineRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JournalEntryServiceTest {

  @Mock private JournalEntryRepository journalEntryRepository;
  @Mock private JournalLineRepository journalLineRepository;
  @Mock private FiscalPeriodRepository fiscalPeriodRepository;
  @Mock private AccountRepository accountRepository;
  @Mock private CurrentUserProvider currentUserProvider;
  @Mock private com.erp.common.security.PermissionChecker permissionChecker;

  @Mock
  private com.erp.common.workflow.repository.ApprovalRequestRepository approvalRequestRepository;

  @Mock private com.erp.common.security.ApprovalAuthorityProvider approvalAuthorityProvider;
  @Mock private com.erp.common.audit.AuditService auditService;
  @Mock private CurrencyConverter currencyConverter;

  @InjectMocks private JournalEntryService journalEntryService;

  private JournalEntry buildBalancedDraft(FiscalPeriod period, Account account) {
    JournalEntry entry =
        JournalEntry.create(
            "JE-20250101-00001",
            LocalDate.of(2025, 1, 1),
            period,
            "설명",
            JournalEntryType.MANUAL,
            "KRW");
    entry.addLine(
        com.erp.finance.domain.model.JournalLine.of(
            entry, 1, account, new BigDecimal("1000"), BigDecimal.ZERO, null, null));
    entry.addLine(
        com.erp.finance.domain.model.JournalLine.of(
            entry, 2, account, BigDecimal.ZERO, new BigDecimal("1000"), null, null));
    return entry;
  }

  private FiscalYear buildFiscalYear() {
    return FiscalYear.of(2025, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));
  }

  private FiscalPeriod buildOpenPeriod() {
    return FiscalPeriod.of(
        buildFiscalYear(), 1, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31));
  }

  private Account buildAccount(String code) {
    return Account.of(code, "테스트계정", AccountType.ASSET, NormalBalance.DEBIT, null, false);
  }

  @Test
  void create_balancedEntry_returnsJournalEntryResponse() {
    FiscalPeriod period = buildOpenPeriod();
    Account account = buildAccount("1100");

    given(fiscalPeriodRepository.findById(1L)).willReturn(Optional.of(period));
    given(accountRepository.findById(anyLong())).willReturn(Optional.of(account));
    given(journalEntryRepository.existsByEntryNo(anyString())).willReturn(false);

    JournalEntry saved =
        JournalEntry.create(
            "JE-20250101-00001",
            LocalDate.of(2025, 1, 1),
            period,
            "테스트전표",
            JournalEntryType.MANUAL,
            "KRW");
    saved.addLine(
        com.erp.finance.domain.model.JournalLine.of(
            saved, 1, account, new BigDecimal("1000"), BigDecimal.ZERO, null, null));
    saved.addLine(
        com.erp.finance.domain.model.JournalLine.of(
            saved, 2, account, BigDecimal.ZERO, new BigDecimal("1000"), null, null));

    given(journalEntryRepository.save(any())).willReturn(saved);
    given(currencyConverter.tryConvert(any(), any(), any())).willReturn(Optional.empty());

    JournalEntryCreateRequest request =
        new JournalEntryCreateRequest(
            LocalDate.of(2025, 1, 15),
            1L,
            "테스트전표",
            JournalEntryType.MANUAL,
            "KRW",
            List.of(
                new JournalLineRequest(1L, new BigDecimal("1000"), BigDecimal.ZERO, null, null),
                new JournalLineRequest(1L, BigDecimal.ZERO, new BigDecimal("1000"), null, null)));

    JournalEntryResponse result = journalEntryService.create(request);

    assertThat(result.entryType()).isEqualTo(JournalEntryType.MANUAL);
  }

  @Test
  void create_foreignCurrencyWithRate_storesBaseSnapshotOnTotalDebit() {
    // AC-8: 생성 시 전표일 환율로 차변합계(1000)를 환산해 base_amount·exchange_rate 저장 (× 1300).
    FiscalPeriod period = buildOpenPeriod();
    Account account = buildAccount("1100");
    given(fiscalPeriodRepository.findById(1L)).willReturn(Optional.of(period));
    given(accountRepository.findById(anyLong())).willReturn(Optional.of(account));
    given(journalEntryRepository.existsByEntryNo(anyString())).willReturn(false);
    given(currencyConverter.tryConvert(any(), any(), any()))
        .willReturn(
            Optional.of(
                new Conversion(new BigDecimal("1300000.00"), new BigDecimal("1300.00000000"))));
    ArgumentCaptor<JournalEntry> captor = ArgumentCaptor.forClass(JournalEntry.class);
    given(journalEntryRepository.save(captor.capture())).willAnswer(inv -> inv.getArgument(0));

    journalEntryService.create(
        new JournalEntryCreateRequest(
            LocalDate.of(2025, 1, 15),
            1L,
            "USD 전표",
            JournalEntryType.MANUAL,
            "USD",
            List.of(
                new JournalLineRequest(1L, new BigDecimal("1000"), BigDecimal.ZERO, null, null),
                new JournalLineRequest(1L, BigDecimal.ZERO, new BigDecimal("1000"), null, null))));

    JournalEntry saved = captor.getValue();
    assertThat(saved.getBaseAmount()).isEqualByComparingTo("1300000.00");
    assertThat(saved.getExchangeRate()).isEqualByComparingTo("1300.00000000");
  }

  @Test
  void create_noRate_leavesBaseSnapshotNull() {
    // AC-11: 환율 부재 통화는 정상 생성하되 base_amount·exchange_rate를 null(미산정)로 남긴다(거부 안 함).
    FiscalPeriod period = buildOpenPeriod();
    Account account = buildAccount("1100");
    given(fiscalPeriodRepository.findById(1L)).willReturn(Optional.of(period));
    given(accountRepository.findById(anyLong())).willReturn(Optional.of(account));
    given(journalEntryRepository.existsByEntryNo(anyString())).willReturn(false);
    given(currencyConverter.tryConvert(any(), any(), any())).willReturn(Optional.empty());
    ArgumentCaptor<JournalEntry> captor = ArgumentCaptor.forClass(JournalEntry.class);
    given(journalEntryRepository.save(captor.capture())).willAnswer(inv -> inv.getArgument(0));

    journalEntryService.create(
        new JournalEntryCreateRequest(
            LocalDate.of(2025, 1, 15),
            1L,
            "JPY 전표",
            JournalEntryType.MANUAL,
            "JPY",
            List.of(
                new JournalLineRequest(1L, new BigDecimal("1000"), BigDecimal.ZERO, null, null),
                new JournalLineRequest(1L, BigDecimal.ZERO, new BigDecimal("1000"), null, null))));

    JournalEntry saved = captor.getValue();
    assertThat(saved.getBaseAmount()).isNull();
    assertThat(saved.getExchangeRate()).isNull();
  }

  @Test
  void create_closedPeriod_throwsFiscalPeriodClosed() {
    FiscalPeriod period = buildOpenPeriod();
    period.close();
    given(fiscalPeriodRepository.findById(1L)).willReturn(Optional.of(period));

    ErpException ex =
        assertThrows(
            ErpException.class,
            () ->
                journalEntryService.create(
                    new JournalEntryCreateRequest(
                        LocalDate.of(2025, 1, 1),
                        1L,
                        "설명",
                        JournalEntryType.MANUAL,
                        null,
                        List.of(
                            new JournalLineRequest(
                                1L, new BigDecimal("1000"), BigDecimal.ZERO, null, null)))));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FISCAL_PERIOD_CLOSED);
  }

  @Test
  void create_entryDateOutOfRange_throwsJournalEntryDateOutOfRange() {
    FiscalPeriod period = buildOpenPeriod(); // Jan 2025 period
    given(fiscalPeriodRepository.findById(1L)).willReturn(Optional.of(period));

    ErpException ex =
        assertThrows(
            ErpException.class,
            () ->
                journalEntryService.create(
                    new JournalEntryCreateRequest(
                        LocalDate.of(2025, 2, 1), // February — outside January period
                        1L,
                        "설명",
                        JournalEntryType.MANUAL,
                        null,
                        List.of(
                            new JournalLineRequest(
                                1L, new BigDecimal("1000"), BigDecimal.ZERO, null, null)))));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.JOURNAL_ENTRY_DATE_OUT_OF_RANGE);
  }

  @Test
  void create_lineWithBothDebitAndCredit_throwsJournalLineAmountsInvalid() {
    FiscalPeriod period = buildOpenPeriod();
    Account account = buildAccount("1100");
    given(fiscalPeriodRepository.findById(1L)).willReturn(Optional.of(period));
    given(accountRepository.findById(anyLong())).willReturn(Optional.of(account));
    given(journalEntryRepository.existsByEntryNo(anyString())).willReturn(false);

    ErpException ex =
        assertThrows(
            ErpException.class,
            () ->
                journalEntryService.create(
                    new JournalEntryCreateRequest(
                        LocalDate.of(2025, 1, 15),
                        1L,
                        "잘못된 전표",
                        JournalEntryType.MANUAL,
                        null,
                        List.of(
                            new JournalLineRequest(
                                1L, new BigDecimal("500"), new BigDecimal("500"), null, null)))));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.JOURNAL_LINE_AMOUNTS_INVALID);
  }

  // AC-1: DRAFT 전표 상신 → PENDING_APPROVAL + ApprovalRequest(GL_ENTRY) 생성·링크
  @Test
  void submitForApproval_draftEntry_transitionsToPendingApprovalAndCreatesApprovalRequest() {
    FiscalPeriod period = buildOpenPeriod();
    JournalEntry entry = buildBalancedDraft(period, buildAccount("1100"));

    given(journalEntryRepository.findById(1L)).willReturn(Optional.of(entry));
    given(currentUserProvider.getCurrentUserId()).willReturn("creator");
    com.erp.common.workflow.ApprovalRequest saved =
        com.erp.common.workflow.ApprovalRequest.create(
            ReferenceTypes.GL_ENTRY,
            1L,
            "t",
            "creator",
            new java.util.ArrayList<>(
                List.of(
                    com.erp.common.workflow.ApprovalStep.of(
                        1, "GL 전표 전기 승인", "@role:finance:gl:approve"))));
    given(approvalRequestRepository.save(any())).willReturn(saved);

    JournalEntryResponse result = journalEntryService.submitForApproval(1L);

    assertThat(result.status().name()).isEqualTo("PENDING_APPROVAL");
    verify(approvalRequestRepository).save(any());
  }

  // AC-2: 불균형 전표 상신 거부 (상태 불변)
  @Test
  void submitForApproval_unbalancedEntry_throwsJournalEntryNotBalanced() {
    FiscalPeriod period = buildOpenPeriod();
    Account account = buildAccount("1100");
    JournalEntry entry =
        JournalEntry.create(
            "JE-20250101-00001",
            LocalDate.of(2025, 1, 1),
            period,
            "설명",
            JournalEntryType.MANUAL,
            "KRW");
    entry.addLine(
        com.erp.finance.domain.model.JournalLine.of(
            entry, 1, account, new BigDecimal("1000"), BigDecimal.ZERO, null, null));
    given(journalEntryRepository.findById(1L)).willReturn(Optional.of(entry));

    ErpException ex =
        assertThrows(ErpException.class, () -> journalEntryService.submitForApproval(1L));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.JOURNAL_ENTRY_NOT_BALANCED);
    assertThat(entry.getStatus().name()).isEqualTo("DRAFT");
  }

  // AC-2: 마감된 회계기간 상신 거부
  @Test
  void submitForApproval_closedPeriod_throwsFiscalPeriodClosed() {
    FiscalPeriod period = buildOpenPeriod();
    JournalEntry entry = buildBalancedDraft(period, buildAccount("1100"));
    period.close();
    given(journalEntryRepository.findById(1L)).willReturn(Optional.of(entry));

    ErpException ex =
        assertThrows(ErpException.class, () -> journalEntryService.submitForApproval(1L));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FISCAL_PERIOD_CLOSED);
  }

  // AC-3: PENDING_APPROVAL 전표 승인 → POSTED
  @Test
  void approve_pendingEntry_transitionsToPosted() {
    FiscalPeriod period = buildOpenPeriod();
    JournalEntry entry = buildBalancedDraft(period, buildAccount("1100"));
    entry.submitForApproval();
    given(journalEntryRepository.findById(1L)).willReturn(Optional.of(entry));
    given(currentUserProvider.getCurrentUserId()).willReturn("approver");
    given(approvalAuthorityProvider.getApprovalLimit()).willReturn(new BigDecimal("1000000"));

    JournalEntryResponse result = journalEntryService.approve(1L);

    assertThat(result.status().name()).isEqualTo("POSTED");
    verify(permissionChecker).require(com.erp.common.security.Permission.FINANCE_GL_APPROVE);
  }

  // AC-5: 차변 합계가 전결 한도 초과 시 거부
  @Test
  void approve_amountExceedsApprovalLimit_throwsLimitExceeded() {
    FiscalPeriod period = buildOpenPeriod();
    JournalEntry entry = buildBalancedDraft(period, buildAccount("1100"));
    entry.submitForApproval();
    given(journalEntryRepository.findById(1L)).willReturn(Optional.of(entry));
    given(currentUserProvider.getCurrentUserId()).willReturn("approver");
    given(approvalAuthorityProvider.getApprovalLimit()).willReturn(new BigDecimal("500"));

    ErpException ex = assertThrows(ErpException.class, () -> journalEntryService.approve(1L));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.APPROVAL_LIMIT_EXCEEDED);
    assertThat(entry.getStatus().name()).isEqualTo("PENDING_APPROVAL");
  }

  // AC-6: 직접 전기 차단 — DRAFT(미상신) 전표 approve 시도는 도메인 가드로 거부
  @Test
  void approve_draftEntryNotSubmitted_throwsNotPendingApproval() {
    FiscalPeriod period = buildOpenPeriod();
    JournalEntry entry = buildBalancedDraft(period, buildAccount("1100"));
    given(journalEntryRepository.findById(1L)).willReturn(Optional.of(entry));
    given(currentUserProvider.getCurrentUserId()).willReturn("approver");
    given(approvalAuthorityProvider.getApprovalLimit()).willReturn(new BigDecimal("1000000"));

    ErpException ex = assertThrows(ErpException.class, () -> journalEntryService.approve(1L));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.JOURNAL_ENTRY_NOT_PENDING_APPROVAL);
  }

  // --- 반려(reject) ---

  private com.erp.common.workflow.ApprovalRequest buildApprovalRequest() {
    return com.erp.common.workflow.ApprovalRequest.create(
        ReferenceTypes.GL_ENTRY,
        1L,
        "t",
        "creator",
        new java.util.ArrayList<>(
            List.of(
                com.erp.common.workflow.ApprovalStep.of(
                    1, "GL 전표 전기 승인", "@role:finance:gl:approve"))));
  }

  // 결재자 반려 → DRAFT 복귀 + ApprovalRequest REJECTED(사유 저장)
  @Test
  void reject_pendingEntry_returnsToDraftAndMarksApprovalRejected() {
    FiscalPeriod period = buildOpenPeriod();
    JournalEntry entry = buildBalancedDraft(period, buildAccount("1100"));
    org.springframework.test.util.ReflectionTestUtils.setField(entry, "createdBy", "creator");
    entry.submitForApproval();
    entry.linkApprovalRequest(10L);
    com.erp.common.workflow.ApprovalRequest req = buildApprovalRequest();
    given(journalEntryRepository.findById(1L)).willReturn(Optional.of(entry));
    given(currentUserProvider.getCurrentUserId()).willReturn("approver");
    given(approvalRequestRepository.findById(10L)).willReturn(Optional.of(req));

    JournalEntryResponse result = journalEntryService.reject(1L, "증빙 누락");

    assertThat(result.status().name()).isEqualTo("DRAFT");
    assertThat(req.getStatus()).isEqualTo(com.erp.common.workflow.ApprovalStatus.REJECTED);
    assertThat(req.getSteps().get(0).getComment()).isEqualTo("증빙 누락");
    verify(permissionChecker).require(com.erp.common.security.Permission.FINANCE_GL_APPROVE);
  }

  // 작성자 본인 반려 차단
  @Test
  void reject_approverIsCreator_throwsApproverNotAuthorized() {
    FiscalPeriod period = buildOpenPeriod();
    JournalEntry entry = buildBalancedDraft(period, buildAccount("1100"));
    org.springframework.test.util.ReflectionTestUtils.setField(entry, "createdBy", "creator");
    entry.submitForApproval();
    given(journalEntryRepository.findById(1L)).willReturn(Optional.of(entry));
    given(currentUserProvider.getCurrentUserId()).willReturn("creator");

    ErpException ex = assertThrows(ErpException.class, () -> journalEntryService.reject(1L, "사유"));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.APPROVER_NOT_AUTHORIZED);
    assertThat(entry.getStatus().name()).isEqualTo("PENDING_APPROVAL");
  }

  // 권한 없음 → 403 FORBIDDEN
  @Test
  void reject_noApprovePermission_throwsForbidden() {
    org.mockito.BDDMockito.willThrow(new ErpException(ErrorCode.FORBIDDEN))
        .given(permissionChecker)
        .require(com.erp.common.security.Permission.FINANCE_GL_APPROVE);

    ErpException ex = assertThrows(ErpException.class, () -> journalEntryService.reject(1L, "사유"));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
  }

  // PENDING_APPROVAL 아닌 상태(DRAFT 미상신) 반려 거부
  @Test
  void reject_notPendingApproval_throwsNotPendingApproval() {
    FiscalPeriod period = buildOpenPeriod();
    JournalEntry entry = buildBalancedDraft(period, buildAccount("1100"));
    org.springframework.test.util.ReflectionTestUtils.setField(entry, "createdBy", "creator");
    given(journalEntryRepository.findById(1L)).willReturn(Optional.of(entry));
    given(currentUserProvider.getCurrentUserId()).willReturn("approver");

    ErpException ex = assertThrows(ErpException.class, () -> journalEntryService.reject(1L, "사유"));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.JOURNAL_ENTRY_NOT_PENDING_APPROVAL);
  }

  // 반려 사유 필수
  @Test
  void reject_blankComment_throwsInvalidInput() {
    given(currentUserProvider.getCurrentUserId()).willReturn("approver");

    ErpException ex = assertThrows(ErpException.class, () -> journalEntryService.reject(1L, "  "));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT);
  }

  // --- 철회(withdraw) ---

  // 상신자 본인 철회 → DRAFT + ApprovalRequest CANCELLED
  @Test
  void withdraw_bySubmitter_returnsToDraftAndCancelsApproval() {
    FiscalPeriod period = buildOpenPeriod();
    JournalEntry entry = buildBalancedDraft(period, buildAccount("1100"));
    org.springframework.test.util.ReflectionTestUtils.setField(entry, "createdBy", "creator");
    entry.submitForApproval();
    entry.linkApprovalRequest(10L);
    com.erp.common.workflow.ApprovalRequest req = buildApprovalRequest();
    given(journalEntryRepository.findById(1L)).willReturn(Optional.of(entry));
    given(currentUserProvider.getCurrentUserId()).willReturn("creator");
    given(approvalRequestRepository.findById(10L)).willReturn(Optional.of(req));

    JournalEntryResponse result = journalEntryService.withdraw(1L);

    assertThat(result.status().name()).isEqualTo("DRAFT");
    assertThat(req.getStatus()).isEqualTo(com.erp.common.workflow.ApprovalStatus.CANCELLED);
    verify(permissionChecker).require(com.erp.common.security.Permission.FINANCE_WRITE);
    verify(auditService)
        .record(
            ReferenceTypes.GL_ENTRY,
            entry.getId(),
            com.erp.common.audit.AuditLog.AuditAction.WITHDRAW,
            null,
            null);
  }

  // 타인 철회 차단
  @Test
  void withdraw_byNonSubmitter_throwsForbidden() {
    FiscalPeriod period = buildOpenPeriod();
    JournalEntry entry = buildBalancedDraft(period, buildAccount("1100"));
    org.springframework.test.util.ReflectionTestUtils.setField(entry, "createdBy", "creator");
    entry.submitForApproval();
    given(journalEntryRepository.findById(1L)).willReturn(Optional.of(entry));
    given(currentUserProvider.getCurrentUserId()).willReturn("someone-else");

    ErpException ex = assertThrows(ErpException.class, () -> journalEntryService.withdraw(1L));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
    assertThat(entry.getStatus().name()).isEqualTo("PENDING_APPROVAL");
  }

  // PENDING_APPROVAL 아닌 상태 철회 거부
  @Test
  void withdraw_notPendingApproval_throwsNotPendingApproval() {
    FiscalPeriod period = buildOpenPeriod();
    JournalEntry entry = buildBalancedDraft(period, buildAccount("1100"));
    org.springframework.test.util.ReflectionTestUtils.setField(entry, "createdBy", "creator");
    given(journalEntryRepository.findById(1L)).willReturn(Optional.of(entry));
    given(currentUserProvider.getCurrentUserId()).willReturn("creator");

    ErpException ex = assertThrows(ErpException.class, () -> journalEntryService.withdraw(1L));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.JOURNAL_ENTRY_NOT_PENDING_APPROVAL);
  }

  // 되돌린 DRAFT 재상신 가능
  @Test
  void withdrawnEntry_canBeResubmitted() {
    FiscalPeriod period = buildOpenPeriod();
    JournalEntry entry = buildBalancedDraft(period, buildAccount("1100"));
    org.springframework.test.util.ReflectionTestUtils.setField(entry, "createdBy", "creator");
    entry.submitForApproval();
    entry.linkApprovalRequest(10L);
    com.erp.common.workflow.ApprovalRequest req = buildApprovalRequest();
    given(journalEntryRepository.findById(1L)).willReturn(Optional.of(entry));
    given(currentUserProvider.getCurrentUserId()).willReturn("creator");
    given(approvalRequestRepository.findById(10L)).willReturn(Optional.of(req));

    journalEntryService.withdraw(1L);
    assertThat(entry.getStatus().name()).isEqualTo("DRAFT");

    given(approvalRequestRepository.save(any())).willReturn(buildApprovalRequest());
    JournalEntryResponse resubmitted = journalEntryService.submitForApproval(1L);

    assertThat(resubmitted.status().name()).isEqualTo("PENDING_APPROVAL");
  }

  @Test
  void findById_notFound_throwsJournalEntryNotFound() {
    given(journalEntryRepository.findById(99L)).willReturn(Optional.empty());

    ErpException ex = assertThrows(ErpException.class, () -> journalEntryService.findById(99L));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.JOURNAL_ENTRY_NOT_FOUND);
  }
}
