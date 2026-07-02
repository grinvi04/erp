package com.erp.finance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.erp.common.AbstractIntegrationTest;
import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.finance.application.ReferenceTypes;
import com.erp.finance.application.dto.DepreciationAccountUpdateRequest;
import com.erp.finance.application.dto.FixedAssetCreateRequest;
import com.erp.finance.application.dto.ImpairmentAccountUpdateRequest;
import com.erp.finance.application.dto.ImpairmentRecognizeResponse;
import com.erp.finance.application.dto.ImpairmentReversalResponse;
import com.erp.finance.application.service.BaseCurrencyService;
import com.erp.finance.application.service.DepreciationPostingService;
import com.erp.finance.application.service.FixedAssetService;
import com.erp.finance.application.service.ImpairmentPostingService;
import com.erp.finance.domain.model.Account;
import com.erp.finance.domain.model.AccountType;
import com.erp.finance.domain.model.DepreciationMethod;
import com.erp.finance.domain.model.FiscalPeriod;
import com.erp.finance.domain.model.FiscalYear;
import com.erp.finance.domain.model.ImpairmentEntryType;
import com.erp.finance.domain.model.JournalEntry;
import com.erp.finance.domain.model.NormalBalance;
import com.erp.finance.domain.repository.AccountRepository;
import com.erp.finance.domain.repository.FiscalPeriodRepository;
import com.erp.finance.domain.repository.FiscalYearRepository;
import com.erp.finance.domain.repository.FixedAssetRepository;
import com.erp.finance.domain.repository.ImpairmentEntryRepository;
import com.erp.finance.domain.repository.JournalEntryRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * 손상차손 인식 → GL 자동 분개(DRAFT) 흐름 검증. 분개: (차)손상차손비 / (대)손상차손누계액 — 균형·손상누계갱신·이력·멱등·계정필수·회계기간·회수가능액
 * 검증·인식 전 상각 catch-up·정액 손상후 재배분(AC-1,5,6,7,8,10,13).
 */
@Transactional
class ImpairmentPostingIntegrationTest extends AbstractIntegrationTest {

  @Autowired private ImpairmentPostingService impairmentPostingService;
  @Autowired private DepreciationPostingService depreciationPostingService;
  @Autowired private FixedAssetService fixedAssetService;
  @Autowired private BaseCurrencyService baseCurrencyService;
  @Autowired private AccountRepository accountRepository;
  @Autowired private FiscalYearRepository fiscalYearRepository;
  @Autowired private FiscalPeriodRepository fiscalPeriodRepository;
  @Autowired private FixedAssetRepository fixedAssetRepository;
  @Autowired private ImpairmentEntryRepository impairmentEntryRepository;
  @Autowired private JournalEntryRepository journalEntryRepository;

  private FiscalYear fiscalYear;
  private Long period1Id;
  private Long assetAccountId;

  @BeforeEach
  void setUp() {
    fiscalYear =
        fiscalYearRepository.save(
            FiscalYear.of(2025, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31)));
    period1Id =
        fiscalPeriodRepository
            .save(
                FiscalPeriod.of(fiscalYear, 1, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31)))
            .getId();
    assetAccountId = accountId("20800", "비품", AccountType.ASSET, NormalBalance.DEBIT);
  }

  private Long accountId(String code, String name, AccountType type, NormalBalance nb) {
    return accountRepository.save(Account.of(code, name, type, nb, null, false)).getId();
  }

  private void configureDepreciationAccounts() {
    authenticate("admin", "finance:setting:write");
    Long expense = accountId("81800", "감가상각비", AccountType.EXPENSE, NormalBalance.DEBIT);
    Long accumulated = accountId("20900", "감가상각누계액", AccountType.ASSET, NormalBalance.CREDIT);
    baseCurrencyService.updateDepreciationAccounts(
        new DepreciationAccountUpdateRequest(expense, accumulated, null, null));
  }

  private void configureImpairmentAccounts() {
    authenticate("admin", "finance:setting:write");
    Long loss = accountId("81900", "유형자산손상차손", AccountType.EXPENSE, NormalBalance.DEBIT);
    Long accumulated = accountId("21000", "손상차손누계액", AccountType.ASSET, NormalBalance.CREDIT);
    Long reversal = accountId("91200", "손상차손환입", AccountType.REVENUE, NormalBalance.CREDIT);
    baseCurrencyService.updateImpairmentAccounts(
        new ImpairmentAccountUpdateRequest(loss, accumulated, reversal));
  }

  /** 환입 계정만 제외(손상차손비·누계액은 설정) — 환입 계정 미설정 차단 검증용. */
  private void configureImpairmentAccountsWithoutReversal() {
    authenticate("admin", "finance:setting:write");
    Long loss = accountId("81900", "유형자산손상차손", AccountType.EXPENSE, NormalBalance.DEBIT);
    Long accumulated = accountId("21000", "손상차손누계액", AccountType.ASSET, NormalBalance.CREDIT);
    baseCurrencyService.updateImpairmentAccounts(
        new ImpairmentAccountUpdateRequest(loss, accumulated, null));
  }

  private Long period2Id() {
    return fiscalPeriodRepository
        .save(FiscalPeriod.of(fiscalYear, 2, LocalDate.of(2025, 2, 1), LocalDate.of(2025, 2, 28)))
        .getId();
  }

  private Long registerStraightLine(String code, BigDecimal cost, int lifeMonths) {
    authenticate("creator", "finance:write");
    return fixedAssetService
        .create(
            new FixedAssetCreateRequest(
                code,
                "노트북",
                LocalDate.of(2025, 1, 1),
                cost,
                BigDecimal.ZERO,
                lifeMonths,
                DepreciationMethod.STRAIGHT_LINE,
                null,
                assetAccountId))
        .id();
  }

  @Test
  void recognize_createsBalancedDraftAndUpdatesImpairmentAndHistory() {
    // AC-1·13: 상각 후 장부 1,100,000 → 회수가능액 800,000 손상 → (차)손상차손비 300,000 (대)손상차손누계액 300,000 균형.
    configureDepreciationAccounts();
    configureImpairmentAccounts();
    Long assetId = registerStraightLine("FA-IMP-1", new BigDecimal("1200000"), 12);

    authenticate("creator", "finance:write");
    depreciationPostingService.runForPeriod(period1Id); // 장부 1,100,000

    ImpairmentRecognizeResponse result =
        impairmentPostingService.recognizeImpairment(assetId, period1Id, new BigDecimal("800000"));

    assertThat(result.bookValueBefore()).isEqualByComparingTo("1100000");
    assertThat(result.impairmentLoss()).isEqualByComparingTo("300000");
    assertThat(result.bookValueAfter()).isEqualByComparingTo("800000");

    var asset = fixedAssetRepository.findById(assetId).orElseThrow();
    assertThat(asset.getAccumulatedImpairment()).isEqualByComparingTo("300000");
    assertThat(asset.bookValue()).isEqualByComparingTo("800000");

    var entry =
        impairmentEntryRepository.findByFixedAssetIdOrderByFiscalPeriodIdAsc(assetId).get(0);
    assertThat(entry.getRecoverableAmount()).isEqualByComparingTo("800000");
    assertThat(entry.getBookValueBefore()).isEqualByComparingTo("1100000");
    assertThat(entry.getImpairmentLoss()).isEqualByComparingTo("300000");
    assertThat(entry.getJournalEntryId()).isNotNull();

    JournalEntry je = journalEntryRepository.findById(entry.getJournalEntryId()).orElseThrow();
    assertThat(je.getStatus().name()).isEqualTo("DRAFT");
    assertThat(je.isBalanced()).isTrue();
    assertThat(je.getTotalDebit()).isEqualByComparingTo("300000");
    assertThat(je.getTotalCredit()).isEqualByComparingTo("300000");
    assertThat(je.getReferenceType()).isEqualTo(ReferenceTypes.IMPAIRMENT);
    assertThat(je.getReferenceId()).isEqualTo(assetId);
  }

  @Test
  void recognize_catchesUpDepreciationThenRespreadsStraightLine() {
    // AC-1·4: 상각 미실행 상태로 2월에 인식 → 1·2월 상각 catch-up(200,000)·장부 1,000,000 → 700,000 손상(300,000).
    // 손상 후 정액 월상각 = (700,000)/잔여 10월 = 70,000.
    configureDepreciationAccounts();
    configureImpairmentAccounts();
    Long assetId = registerStraightLine("FA-IMP-2", new BigDecimal("1200000"), 12);
    Long period2 = period2Id();

    authenticate("creator", "finance:write");
    ImpairmentRecognizeResponse result =
        impairmentPostingService.recognizeImpairment(assetId, period2, new BigDecimal("700000"));

    assertThat(result.bookValueBefore()).isEqualByComparingTo("1000000"); // catch-up 200,000
    assertThat(result.impairmentLoss()).isEqualByComparingTo("300000");

    var asset = fixedAssetRepository.findById(assetId).orElseThrow();
    assertThat(asset.getAccumulatedDepreciation()).isEqualByComparingTo("200000");
    assertThat(asset.getAccumulatedImpairment()).isEqualByComparingTo("300000");
    assertThat(asset.bookValue()).isEqualByComparingTo("700000");
    // 잔여 10월 재배분: 700,000 / 10 = 70,000.
    assertThat(asset.monthlyDepreciation()).isEqualByComparingTo("70000");
  }

  @Test
  void recognize_twiceSamePeriod_isRejected() {
    // AC-5: 같은 (자산,기간) 재인식 → 거부.
    configureDepreciationAccounts();
    configureImpairmentAccounts();
    Long assetId = registerStraightLine("FA-IMP-3", new BigDecimal("1200000"), 12);

    authenticate("creator", "finance:write");
    impairmentPostingService.recognizeImpairment(assetId, period1Id, new BigDecimal("800000"));

    assertThatThrownBy(
            () ->
                impairmentPostingService.recognizeImpairment(
                    assetId, period1Id, new BigDecimal("700000")))
        .isInstanceOf(ErpException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.IMPAIRMENT_ALREADY_RECOGNIZED);
  }

  @Test
  void recognize_recoverableNotBelowBookValue_isRejected() {
    // AC-6: 회수가능액 ≥ 장부가액 → 인식할 손상 없음. (catch-up 상각이 손실 판정 전에 실행되므로 감가상각 계정도 설정.)
    configureDepreciationAccounts();
    configureImpairmentAccounts();
    Long assetId = registerStraightLine("FA-IMP-4", new BigDecimal("1200000"), 12);

    authenticate("creator", "finance:write");
    assertThatThrownBy(
            () ->
                impairmentPostingService.recognizeImpairment(
                    assetId, period1Id, new BigDecimal("1300000")))
        .isInstanceOf(ErpException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.IMPAIRMENT_NOT_REQUIRED);
  }

  @Test
  void recognize_negativeRecoverable_isRejected() {
    // AC-7: 회수가능액 < 0 → 400.
    configureImpairmentAccounts();
    Long assetId = registerStraightLine("FA-IMP-5", new BigDecimal("1200000"), 12);

    authenticate("creator", "finance:write");
    assertThatThrownBy(
            () ->
                impairmentPostingService.recognizeImpairment(
                    assetId, period1Id, new BigDecimal("-1")))
        .isInstanceOf(ErpException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.INVALID_INPUT);
  }

  @Test
  void recognize_withoutImpairmentAccounts_isBlocked() {
    // AC-8: 손상차손비/누계액 계정 미설정 → 차단(빈 분개 금지).
    Long assetId = registerStraightLine("FA-IMP-6", new BigDecimal("1200000"), 12);

    authenticate("creator", "finance:write");
    assertThatThrownBy(
            () ->
                impairmentPostingService.recognizeImpairment(
                    assetId, period1Id, new BigDecimal("800000")))
        .isInstanceOf(ErpException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.IMPAIRMENT_ACCOUNT_NOT_CONFIGURED);
  }

  @Test
  void recognize_periodBeforeAcquisition_isRejected() {
    // AC-7 경계: 취득일(3월) 이전 기간(1월)에 손상 인식 → 거부(취득 전 기간 귀속 방지).
    configureImpairmentAccounts();
    authenticate("creator", "finance:write");
    Long assetId =
        fixedAssetService
            .create(
                new FixedAssetCreateRequest(
                    "FA-IMP-PRE",
                    "3월취득자산",
                    LocalDate.of(2025, 3, 1),
                    new BigDecimal("1200000"),
                    BigDecimal.ZERO,
                    12,
                    DepreciationMethod.STRAIGHT_LINE,
                    null,
                    assetAccountId))
            .id();

    assertThatThrownBy(
            () ->
                impairmentPostingService.recognizeImpairment(
                    assetId, period1Id, new BigDecimal("800000")))
        .isInstanceOf(ErpException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.INVALID_INPUT);
  }

  @Test
  void recognize_closedPeriod_isRejected() {
    // AC-10: 마감된 회계기간 → 손상 거부.
    configureImpairmentAccounts();
    Long assetId = registerStraightLine("FA-IMP-7", new BigDecimal("1200000"), 12);
    fiscalPeriodRepository.findById(period1Id).orElseThrow().close();

    authenticate("creator", "finance:write");
    assertThatThrownBy(
            () ->
                impairmentPostingService.recognizeImpairment(
                    assetId, period1Id, new BigDecimal("800000")))
        .isInstanceOf(ErpException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.FISCAL_PERIOD_CLOSED);
  }

  // ── 손상차손 환입(reversal) ─────────────────────────────────────────

  /** 1월 상각·손상 인식 후 2월 환입 셋업 — 환입 대상 자산 반환. recoverable로 손상 강도 조절. */
  private Long impairThenPeriod2(String code, BigDecimal impairRecoverable) {
    configureDepreciationAccounts();
    configureImpairmentAccounts();
    Long assetId = registerStraightLine(code, new BigDecimal("1200000"), 60);
    authenticate("creator", "finance:write");
    impairmentPostingService.recognizeImpairment(assetId, period1Id, impairRecoverable);
    period2Id();
    return assetId;
  }

  @Test
  void reverse_createsBalancedDraftAndReducesImpairment() {
    // AC-1·15: 1월 상각·손상(회수가능 60만) 후 2월 회수가능 90만 환입 → 장부 90만(한도 116만 미만)·균형·손상누계 차감.
    Long assetId = impairThenPeriod2("FA-REV-1", new BigDecimal("600000"));
    BigDecimal impairmentAfter =
        fixedAssetRepository.findById(assetId).orElseThrow().getAccumulatedImpairment();
    Long period2 =
        fiscalPeriodRepository.findAll().stream().mapToLong(p -> p.getId()).max().getAsLong();

    ImpairmentReversalResponse result =
        impairmentPostingService.reverseImpairment(assetId, period2, new BigDecimal("900000"));

    assertThat(result.bookValueAfter()).isEqualByComparingTo("900000"); // 회수가능액(한도 미만)까지 환입
    assertThat(result.reversalAmount())
        .isEqualByComparingTo(result.bookValueAfter().subtract(result.bookValueBefore()));
    assertThat(result.reversalAmount().signum()).isPositive();

    var asset = fixedAssetRepository.findById(assetId).orElseThrow();
    assertThat(asset.bookValue()).isEqualByComparingTo("900000");
    assertThat(asset.getAccumulatedImpairment())
        .isEqualByComparingTo(impairmentAfter.subtract(result.reversalAmount()));

    var entry =
        impairmentEntryRepository.findByFixedAssetIdOrderByFiscalPeriodIdAsc(assetId).stream()
            .filter(e -> e.getEntryType() == ImpairmentEntryType.REVERSAL)
            .findFirst()
            .orElseThrow();
    JournalEntry je = journalEntryRepository.findById(entry.getJournalEntryId()).orElseThrow();
    assertThat(je.getStatus().name()).isEqualTo("DRAFT");
    assertThat(je.isBalanced()).isTrue();
    assertThat(je.getTotalDebit()).isEqualByComparingTo(result.reversalAmount());
    assertThat(je.getTotalCredit()).isEqualByComparingTo(result.reversalAmount());
    assertThat(je.getReferenceType()).isEqualTo(ReferenceTypes.IMPAIRMENT_REVERSAL);
    assertThat(je.getReferenceId()).isEqualTo(assetId);
  }

  @Test
  void reverse_cappedAtCeiling() {
    // AC-3: 큰 손상(회수가능 10만) 후 2월 회수가능 500만(한도 초과) 환입 → 장부가액은 한도(116만)까지만.
    Long assetId = impairThenPeriod2("FA-REV-2", new BigDecimal("100000"));
    Long period2 =
        fiscalPeriodRepository.findAll().stream().mapToLong(p -> p.getId()).max().getAsLong();

    ImpairmentReversalResponse result =
        impairmentPostingService.reverseImpairment(assetId, period2, new BigDecimal("5000000"));

    // 한도 = 1,200,000 − min(2×20,000, …) = 1,160,000. 회수가능 500만이지만 한도까지만.
    assertThat(result.bookValueAfter()).isEqualByComparingTo("1160000");
  }

  @Test
  void reverse_recoverableBelowBookValue_isRejected() {
    // AC-6: 회수가능액이 현재 장부가액 이하 → 환입할 손상 없음.
    Long assetId = impairThenPeriod2("FA-REV-3", new BigDecimal("600000"));
    Long period2 =
        fiscalPeriodRepository.findAll().stream().mapToLong(p -> p.getId()).max().getAsLong();

    assertThatThrownBy(
            () ->
                impairmentPostingService.reverseImpairment(
                    assetId, period2, new BigDecimal("100000")))
        .isInstanceOf(ErpException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.IMPAIRMENT_REVERSAL_NOT_REQUIRED);
  }

  @Test
  void reverse_noImpairment_isRejected() {
    // AC-6: 손상 이력이 없는 자산 → 환입 불가(환입액 0).
    configureDepreciationAccounts();
    configureImpairmentAccounts();
    Long assetId = registerStraightLine("FA-REV-4", new BigDecimal("1200000"), 60);

    authenticate("creator", "finance:write");
    assertThatThrownBy(
            () ->
                impairmentPostingService.reverseImpairment(
                    assetId, period1Id, new BigDecimal("5000000")))
        .isInstanceOf(ErpException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.IMPAIRMENT_REVERSAL_NOT_REQUIRED);
  }

  @Test
  void reverse_withoutReversalAccount_isBlocked() {
    // AC-8: 손상차손환입 계정 미설정 → 차단.
    configureDepreciationAccounts();
    configureImpairmentAccountsWithoutReversal();
    Long assetId = registerStraightLine("FA-REV-5", new BigDecimal("1200000"), 60);
    authenticate("creator", "finance:write");
    impairmentPostingService.recognizeImpairment(assetId, period1Id, new BigDecimal("600000"));
    Long period2 = period2Id();

    assertThatThrownBy(
            () ->
                impairmentPostingService.reverseImpairment(
                    assetId, period2, new BigDecimal("900000")))
        .isInstanceOf(ErpException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.IMPAIRMENT_REVERSAL_ACCOUNT_NOT_CONFIGURED);
  }

  @Test
  void reverse_negativeRecoverable_isRejected() {
    // AC-9: 회수가능액 < 0 → 400.
    configureImpairmentAccounts();
    Long assetId = registerStraightLine("FA-REV-6", new BigDecimal("1200000"), 60);
    authenticate("creator", "finance:write");
    assertThatThrownBy(
            () ->
                impairmentPostingService.reverseImpairment(
                    assetId, period1Id, new BigDecimal("-1")))
        .isInstanceOf(ErpException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.INVALID_INPUT);
  }

  @Test
  void reverse_twiceSamePeriod_isRejected() {
    // AC-7: 같은 (자산,기간,REVERSAL) 재환입 → 거부.
    Long assetId = impairThenPeriod2("FA-REV-7", new BigDecimal("100000"));
    Long period2 =
        fiscalPeriodRepository.findAll().stream().mapToLong(p -> p.getId()).max().getAsLong();
    impairmentPostingService.reverseImpairment(assetId, period2, new BigDecimal("900000"));

    assertThatThrownBy(
            () ->
                impairmentPostingService.reverseImpairment(
                    assetId, period2, new BigDecimal("950000")))
        .isInstanceOf(ErpException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.IMPAIRMENT_REVERSAL_ALREADY_RECOGNIZED);
  }

  @Test
  void reverse_samePeriodAsImpairment_isRejected() {
    // AC: 같은 기간 손상 인식 후 동일 기간 환입 → 거부(IAS 36: 환입은 이전 기간 손상 대상).
    configureDepreciationAccounts();
    configureImpairmentAccounts();
    Long assetId = registerStraightLine("FA-REV-SAME", new BigDecimal("1200000"), 60);
    authenticate("creator", "finance:write");
    impairmentPostingService.recognizeImpairment(assetId, period1Id, new BigDecimal("600000"));

    assertThatThrownBy(
            () ->
                impairmentPostingService.reverseImpairment(
                    assetId, period1Id, new BigDecimal("900000")))
        .isInstanceOf(ErpException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.IMPAIRMENT_REVERSAL_SAME_PERIOD_AS_IMPAIRMENT);
  }

  // ── 음성 인가(권한 미보유 거부) ──────────────────────────────────────

  @Test
  void recognize_withoutWritePermission_isForbidden() {
    // AC-13: finance:write 미보유 → 손상 인식 거부.
    authenticate("viewer", "finance:read");
    assertThatThrownBy(
            () ->
                impairmentPostingService.recognizeImpairment(
                    1L, period1Id, new BigDecimal("800000")))
        .isInstanceOf(ErpException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.FORBIDDEN);
  }

  @Test
  void reverse_withoutWritePermission_isForbidden() {
    // AC-13: finance:write 미보유 → 환입 거부.
    authenticate("viewer", "finance:read");
    assertThatThrownBy(
            () ->
                impairmentPostingService.reverseImpairment(1L, period1Id, new BigDecimal("900000")))
        .isInstanceOf(ErpException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.FORBIDDEN);
  }

  @Test
  void updateImpairmentAccounts_withoutSettingWrite_isForbidden() {
    // AC-13: finance:setting:write 미보유 → 손상 계정 설정 거부.
    authenticate("viewer", "finance:read");
    assertThatThrownBy(
            () ->
                baseCurrencyService.updateImpairmentAccounts(
                    new ImpairmentAccountUpdateRequest(1L, 2L, 3L)))
        .isInstanceOf(ErpException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.FORBIDDEN);
  }
}
