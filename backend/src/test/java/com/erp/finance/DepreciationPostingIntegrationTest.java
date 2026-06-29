package com.erp.finance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.erp.common.AbstractIntegrationTest;
import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.finance.application.ReferenceTypes;
import com.erp.finance.application.dto.DepreciationAccountUpdateRequest;
import com.erp.finance.application.dto.DepreciationRunResponse;
import com.erp.finance.application.dto.FixedAssetCreateRequest;
import com.erp.finance.application.service.BaseCurrencyService;
import com.erp.finance.application.service.DepreciationPostingService;
import com.erp.finance.application.service.FixedAssetService;
import com.erp.finance.domain.model.Account;
import com.erp.finance.domain.model.AccountType;
import com.erp.finance.domain.model.DepreciationMethod;
import com.erp.finance.domain.model.FiscalPeriod;
import com.erp.finance.domain.model.FiscalYear;
import com.erp.finance.domain.model.JournalEntry;
import com.erp.finance.domain.model.NormalBalance;
import com.erp.finance.domain.repository.AccountRepository;
import com.erp.finance.domain.repository.DepreciationEntryRepository;
import com.erp.finance.domain.repository.FiscalPeriodRepository;
import com.erp.finance.domain.repository.FiscalYearRepository;
import com.erp.finance.domain.repository.FixedAssetRepository;
import com.erp.finance.domain.repository.JournalEntryRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * 월별 감가상각 처리 → GL 자동 분개(DRAFT) 흐름 검증. 분개: (차)감가상각비 / (대)감가상각누계액 —
 * 균형·누계갱신·이력·멱등·계정필수·회계기간(AC-4,5,8,10,12).
 */
@Transactional
class DepreciationPostingIntegrationTest extends AbstractIntegrationTest {

  @Autowired private DepreciationPostingService depreciationPostingService;
  @Autowired private FixedAssetService fixedAssetService;
  @Autowired private BaseCurrencyService baseCurrencyService;
  @Autowired private AccountRepository accountRepository;
  @Autowired private FiscalYearRepository fiscalYearRepository;
  @Autowired private FiscalPeriodRepository fiscalPeriodRepository;
  @Autowired private FixedAssetRepository fixedAssetRepository;
  @Autowired private DepreciationEntryRepository depreciationEntryRepository;
  @Autowired private JournalEntryRepository journalEntryRepository;

  private Long periodId;
  private Long assetAccountId;

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

  private Long registerAsset(String code, BigDecimal cost, int lifeMonths) {
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
  void run_activeAsset_createsBalancedDraftAndUpdatesAccumulatedAndHistory() {
    // AC-4·12: 1,200,000 / 12개월 = 100,000 월상각 → (차)감가상각비 100,000 (대)누계액 100,000 균형.
    configureDepreciationAccounts();
    Long assetId = registerAsset("FA-DEP-1", new BigDecimal("1200000"), 12);

    authenticate("creator", "finance:write");
    DepreciationRunResponse result = depreciationPostingService.runForPeriod(periodId);

    assertThat(result.processedCount()).isEqualTo(1);
    assertThat(result.totalAmount()).isEqualByComparingTo("100000");

    // 누계상각·장부가액 갱신
    var asset = fixedAssetRepository.findById(assetId).orElseThrow();
    assertThat(asset.getAccumulatedDepreciation()).isEqualByComparingTo("100000");
    assertThat(asset.bookValue()).isEqualByComparingTo("1100000");

    // 상각 이력 + 분개 역참조
    var entry =
        depreciationEntryRepository.findByFixedAssetIdOrderByFiscalPeriodIdAsc(assetId).get(0);
    assertThat(entry.getAmount()).isEqualByComparingTo("100000");
    assertThat(entry.getJournalEntryId()).isNotNull();

    JournalEntry je = journalEntryRepository.findById(entry.getJournalEntryId()).orElseThrow();
    assertThat(je.getStatus().name()).isEqualTo("DRAFT");
    assertThat(je.isBalanced()).isTrue();
    assertThat(je.getTotalDebit()).isEqualByComparingTo("100000");
    assertThat(je.getTotalCredit()).isEqualByComparingTo("100000");
    assertThat(je.getReferenceType()).isEqualTo(ReferenceTypes.DEPRECIATION);
    assertThat(je.getReferenceId()).isEqualTo(assetId);
  }

  @Test
  void run_twiceSamePeriod_isIdempotent() {
    // AC-5: 같은 (자산,기간) 재처리 → 중복 상각·분개 없음.
    configureDepreciationAccounts();
    Long assetId = registerAsset("FA-DEP-2", new BigDecimal("1200000"), 12);

    authenticate("creator", "finance:write");
    depreciationPostingService.runForPeriod(periodId);
    DepreciationRunResponse second = depreciationPostingService.runForPeriod(periodId);

    assertThat(second.processedCount()).isZero();
    assertThat(second.skippedCount()).isEqualTo(1);
    assertThat(fixedAssetRepository.findById(assetId).orElseThrow().getAccumulatedDepreciation())
        .isEqualByComparingTo("100000");
    assertThat(depreciationEntryRepository.findByFixedAssetIdOrderByFiscalPeriodIdAsc(assetId))
        .hasSize(1);
  }

  @Test
  void run_withoutAccountsConfigured_isBlocked() {
    // AC-8: 감가상각비/누계액 계정 미설정 → 차단(빈 분개 금지).
    registerAsset("FA-DEP-3", new BigDecimal("1200000"), 12);

    authenticate("creator", "finance:write");
    assertThatThrownBy(() -> depreciationPostingService.runForPeriod(periodId))
        .isInstanceOf(ErpException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.DEPRECIATION_ACCOUNT_NOT_CONFIGURED);
  }

  @Test
  void run_closedPeriod_isRejected() {
    // AC-10: 마감된 회계기간 → 상각 거부.
    configureDepreciationAccounts();
    registerAsset("FA-DEP-4", new BigDecimal("1200000"), 12);
    fiscalPeriodRepository.findById(periodId).orElseThrow().close();

    authenticate("creator", "finance:write");
    assertThatThrownBy(() -> depreciationPostingService.runForPeriod(periodId))
        .isInstanceOf(ErpException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.FISCAL_PERIOD_CLOSED);
  }

  @Test
  void run_fullyDepreciatedAsset_isSkipped() {
    // AC-6: 잔존가치 도달(내용연수 1개월·1회 상각 후) → 추가 상각 0, 건너뜀.
    configureDepreciationAccounts();
    Long assetId = registerAsset("FA-DEP-5", new BigDecimal("1200000"), 1);

    authenticate("creator", "finance:write");
    depreciationPostingService.runForPeriod(periodId); // 1회로 전액 상각

    // 2기간 생성 후 재처리 → 상각액 0이라 건너뜀
    FiscalYear fy = fiscalYearRepository.findAll().get(0);
    Long period2 =
        fiscalPeriodRepository
            .save(FiscalPeriod.of(fy, 2, LocalDate.of(2025, 2, 1), LocalDate.of(2025, 2, 28)))
            .getId();
    DepreciationRunResponse r2 = depreciationPostingService.runForPeriod(period2);

    assertThat(r2.processedCount()).isZero();
    assertThat(fixedAssetRepository.findById(assetId).orElseThrow().getAccumulatedDepreciation())
        .isEqualByComparingTo("1200000");
  }

  @Test
  void run_assetAcquiredAfterPeriod_isSkipped() {
    // 취득월부터 상각: 취득일(3월)이 대상 기간(1월) 이후면 미취득 → 건너뜀(취득 전 상각 금지).
    configureDepreciationAccounts();
    authenticate("creator", "finance:write");
    Long assetId =
        fixedAssetService
            .create(
                new FixedAssetCreateRequest(
                    "FA-DEP-FUTURE",
                    "3월취득자산",
                    LocalDate.of(2025, 3, 1),
                    new BigDecimal("1200000"),
                    BigDecimal.ZERO,
                    12,
                    DepreciationMethod.STRAIGHT_LINE,
                    null,
                    assetAccountId))
            .id();

    DepreciationRunResponse result = depreciationPostingService.runForPeriod(periodId); // 1월 기간

    assertThat(result.processedCount()).isZero();
    assertThat(result.skippedCount()).isEqualTo(1);
    assertThat(fixedAssetRepository.findById(assetId).orElseThrow().getAccumulatedDepreciation())
        .isEqualByComparingTo("0");
  }
}
