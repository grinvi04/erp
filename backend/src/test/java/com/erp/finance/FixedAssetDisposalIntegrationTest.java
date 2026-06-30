package com.erp.finance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.erp.common.AbstractIntegrationTest;
import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.finance.application.ReferenceTypes;
import com.erp.finance.application.dto.DepreciationAccountUpdateRequest;
import com.erp.finance.application.dto.FixedAssetCreateRequest;
import com.erp.finance.application.dto.FixedAssetDisposeRequest;
import com.erp.finance.application.dto.ImpairmentAccountUpdateRequest;
import com.erp.finance.application.service.BaseCurrencyService;
import com.erp.finance.application.service.DepreciationPostingService;
import com.erp.finance.application.service.FixedAssetService;
import com.erp.finance.application.service.ImpairmentPostingService;
import com.erp.finance.domain.model.Account;
import com.erp.finance.domain.model.AccountType;
import com.erp.finance.domain.model.DepreciationMethod;
import com.erp.finance.domain.model.FiscalPeriod;
import com.erp.finance.domain.model.FiscalYear;
import com.erp.finance.domain.model.JournalEntry;
import com.erp.finance.domain.model.NormalBalance;
import com.erp.finance.domain.repository.AccountRepository;
import com.erp.finance.domain.repository.FiscalPeriodRepository;
import com.erp.finance.domain.repository.FiscalYearRepository;
import com.erp.finance.domain.repository.JournalEntryRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * 고정자산 처분(매각·폐기) → 처분손익 GL 자동 분개·상태 전이 검증. 분개: (차)감가상각누계액·현금·처분손실 / (대)자산(취득원가)·처분이익 —
 * 균형·상태=처분·계정필수(AC-7,8,12).
 */
@Transactional
class FixedAssetDisposalIntegrationTest extends AbstractIntegrationTest {

  @Autowired private FixedAssetService fixedAssetService;
  @Autowired private DepreciationPostingService depreciationPostingService;
  @Autowired private ImpairmentPostingService impairmentPostingService;
  @Autowired private BaseCurrencyService baseCurrencyService;
  @Autowired private AccountRepository accountRepository;
  @Autowired private FiscalYearRepository fiscalYearRepository;
  @Autowired private FiscalPeriodRepository fiscalPeriodRepository;
  @Autowired private JournalEntryRepository journalEntryRepository;

  private Long periodId;
  private Long assetAccountId;
  private Long cashAccountId;

  @BeforeEach
  void setUp() {
    FiscalYear fy =
        fiscalYearRepository.save(
            FiscalYear.of(2025, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31)));
    periodId =
        fiscalPeriodRepository
            .save(FiscalPeriod.of(fy, 1, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31)))
            .getId();
    assetAccountId = accountId("20800", "비품", AccountType.ASSET, NormalBalance.DEBIT);
    cashAccountId = accountId("10100", "현금", AccountType.ASSET, NormalBalance.DEBIT);
  }

  private Long accountId(String code, String name, AccountType type, NormalBalance nb) {
    return accountRepository.save(Account.of(code, name, type, nb, null, false)).getId();
  }

  /** 감가상각비·누계액 계정만 설정(처분 손익 계정 제외). */
  private void configureDepreciationOnly() {
    authenticate("admin", "finance:setting:write");
    Long expense = accountId("81800", "감가상각비", AccountType.EXPENSE, NormalBalance.DEBIT);
    Long accumulated = accountId("20900", "감가상각누계액", AccountType.ASSET, NormalBalance.CREDIT);
    baseCurrencyService.updateDepreciationAccounts(
        new DepreciationAccountUpdateRequest(expense, accumulated, null, null));
  }

  /** 감가상각·처분손익 계정 전체 설정. */
  private void configureAllAccounts() {
    authenticate("admin", "finance:setting:write");
    Long expense = accountId("81800", "감가상각비", AccountType.EXPENSE, NormalBalance.DEBIT);
    Long accumulated = accountId("20900", "감가상각누계액", AccountType.ASSET, NormalBalance.CREDIT);
    Long gain = accountId("91100", "유형자산처분이익", AccountType.REVENUE, NormalBalance.CREDIT);
    Long loss = accountId("95100", "유형자산처분손실", AccountType.EXPENSE, NormalBalance.DEBIT);
    baseCurrencyService.updateDepreciationAccounts(
        new DepreciationAccountUpdateRequest(expense, accumulated, gain, loss));
  }

  /** 취득 1,200,000·12개월 자산 등록 후 1회 상각 → 누계 100,000·장부 1,100,000. */
  private Long registerAndDepreciateOnce(String code) {
    authenticate("creator", "finance:write");
    Long assetId =
        fixedAssetService
            .create(
                new FixedAssetCreateRequest(
                    code,
                    "노트북",
                    LocalDate.of(2025, 1, 1),
                    new BigDecimal("1200000"),
                    BigDecimal.ZERO,
                    12,
                    DepreciationMethod.STRAIGHT_LINE,
                    null,
                    assetAccountId))
            .id();
    depreciationPostingService.runForPeriod(periodId);
    return assetId;
  }

  @Test
  void dispose_saleAboveBookValue_postsGainAndDisposesAsset() {
    // AC-7·12: 장부 1,100,000 자산을 1,150,000에 매각 → 처분이익 50,000.
    // (차)누계액 100,000·현금 1,150,000 (대)자산 1,200,000·처분이익 50,000 = 1,250,000 균형.
    configureAllAccounts();
    Long assetId = registerAndDepreciateOnce("FA-DIS-1");

    authenticate("creator", "finance:write");
    var result =
        fixedAssetService.dispose(
            assetId,
            new FixedAssetDisposeRequest(
                LocalDate.of(2025, 1, 20), new BigDecimal("1150000"), cashAccountId));

    assertThat(result.status().name()).isEqualTo("DISPOSED");

    JournalEntry je =
        journalEntryRepository
            .findByReferenceTypeAndReferenceId(ReferenceTypes.ASSET_DISPOSAL, assetId)
            .orElseThrow();
    assertThat(je.getStatus().name()).isEqualTo("DRAFT");
    assertThat(je.isBalanced()).isTrue();
    assertThat(je.getTotalDebit()).isEqualByComparingTo("1250000");
    assertThat(je.getTotalCredit()).isEqualByComparingTo("1250000");
  }

  @Test
  void dispose_scrapWithNoProceeds_postsLossEqualToBookValue() {
    // AC-7: 장부 1,100,000 자산을 폐기(대가 0) → 처분손실 1,100,000.
    // (차)누계액 100,000·처분손실 1,100,000 (대)자산 1,200,000 = 1,200,000 균형.
    configureAllAccounts();
    Long assetId = registerAndDepreciateOnce("FA-DIS-2");

    authenticate("creator", "finance:write");
    fixedAssetService.dispose(
        assetId, new FixedAssetDisposeRequest(LocalDate.of(2025, 1, 20), BigDecimal.ZERO, null));

    JournalEntry je =
        journalEntryRepository
            .findByReferenceTypeAndReferenceId(ReferenceTypes.ASSET_DISPOSAL, assetId)
            .orElseThrow();
    assertThat(je.isBalanced()).isTrue();
    assertThat(je.getTotalDebit()).isEqualByComparingTo("1200000");
    assertThat(je.getTotalCredit()).isEqualByComparingTo("1200000");
  }

  @Test
  void dispose_withoutDisposalGainLossAccounts_isBlocked() {
    // AC-8: 처분이익 계정 미설정 상태에서 매각이익 발생 → 차단(빈 분개 금지).
    configureDepreciationOnly();
    Long assetId = registerAndDepreciateOnce("FA-DIS-3");

    authenticate("creator", "finance:write");
    assertThatThrownBy(
            () ->
                fixedAssetService.dispose(
                    assetId,
                    new FixedAssetDisposeRequest(
                        LocalDate.of(2025, 1, 20), new BigDecimal("1150000"), cashAccountId)))
        .isInstanceOf(ErpException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.DISPOSAL_ACCOUNT_NOT_CONFIGURED);
  }

  @Test
  void dispose_impairedAsset_clearsImpairmentAccumulatedAndBalances() {
    // AC-9: 상각 100,000(장부 1,100,000) → 회수가능액 700,000 손상(400,000)·장부 700,000 → 750,000 매각.
    // (차)감가상각누계액 100,000·손상차손누계액 400,000·현금 750,000 (대)자산 1,200,000·처분이익 50,000 = 1,250,000 균형.
    configureAllAccounts();
    authenticate("admin", "finance:setting:write");
    Long impairmentLoss = accountId("81900", "유형자산손상차손", AccountType.EXPENSE, NormalBalance.DEBIT);
    Long impairmentAccumulated =
        accountId("21000", "손상차손누계액", AccountType.ASSET, NormalBalance.CREDIT);
    baseCurrencyService.updateImpairmentAccounts(
        new ImpairmentAccountUpdateRequest(impairmentLoss, impairmentAccumulated, null));

    Long assetId = registerAndDepreciateOnce("FA-DIS-IMP");
    authenticate("creator", "finance:write");
    impairmentPostingService.recognizeImpairment(assetId, periodId, new BigDecimal("700000"));

    fixedAssetService.dispose(
        assetId,
        new FixedAssetDisposeRequest(
            LocalDate.of(2025, 1, 20), new BigDecimal("750000"), cashAccountId));

    JournalEntry je =
        journalEntryRepository
            .findByReferenceTypeAndReferenceId(ReferenceTypes.ASSET_DISPOSAL, assetId)
            .orElseThrow();
    assertThat(je.isBalanced()).isTrue();
    assertThat(je.getTotalDebit()).isEqualByComparingTo("1250000");
    assertThat(je.getTotalCredit()).isEqualByComparingTo("1250000");
  }

  @Test
  void dispose_alreadyDisposed_throws() {
    // 이미 처분된 자산 재처분 → F045.
    configureAllAccounts();
    Long assetId = registerAndDepreciateOnce("FA-DIS-4");
    authenticate("creator", "finance:write");
    fixedAssetService.dispose(
        assetId,
        new FixedAssetDisposeRequest(
            LocalDate.of(2025, 1, 20), new BigDecimal("1150000"), cashAccountId));

    assertThatThrownBy(
            () ->
                fixedAssetService.dispose(
                    assetId,
                    new FixedAssetDisposeRequest(LocalDate.of(2025, 1, 21), BigDecimal.ZERO, null)))
        .isInstanceOf(ErpException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.FIXED_ASSET_ALREADY_DISPOSED);
  }
}
