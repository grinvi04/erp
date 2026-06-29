package com.erp.finance;

import static org.assertj.core.api.Assertions.assertThat;

import com.erp.common.AbstractIntegrationTest;
import com.erp.finance.application.ReferenceTypes;
import com.erp.finance.application.dto.DepreciationAccountUpdateRequest;
import com.erp.finance.application.dto.FixedAssetCreateRequest;
import com.erp.finance.application.dto.FixedAssetDisposeRequest;
import com.erp.finance.application.service.BaseCurrencyService;
import com.erp.finance.application.service.DepreciationPostingService;
import com.erp.finance.application.service.FixedAssetService;
import com.erp.finance.domain.model.Account;
import com.erp.finance.domain.model.AccountType;
import com.erp.finance.domain.model.DepreciationEntry;
import com.erp.finance.domain.model.DepreciationMethod;
import com.erp.finance.domain.model.FiscalPeriod;
import com.erp.finance.domain.model.FiscalYear;
import com.erp.finance.domain.model.NormalBalance;
import com.erp.finance.domain.repository.AccountRepository;
import com.erp.finance.domain.repository.DepreciationEntryRepository;
import com.erp.finance.domain.repository.FiscalPeriodRepository;
import com.erp.finance.domain.repository.FiscalYearRepository;
import com.erp.finance.domain.repository.FixedAssetRepository;
import com.erp.finance.domain.repository.JournalEntryRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * 처분 시 catch-up 상각 — 처분일까지 상각 반영(처분월 미상각: 취득월 포함·처분월 제외). 미처리 직전 기간을 자동 상각해 장부가액을 현행화한 뒤 처분손익을
 * 계산하는지 검증.
 */
@Transactional
class FixedAssetDisposalCatchUpIntegrationTest extends AbstractIntegrationTest {

  @Autowired private FixedAssetService fixedAssetService;
  @Autowired private DepreciationPostingService depreciationPostingService;
  @Autowired private BaseCurrencyService baseCurrencyService;
  @Autowired private AccountRepository accountRepository;
  @Autowired private FiscalYearRepository fiscalYearRepository;
  @Autowired private FiscalPeriodRepository fiscalPeriodRepository;
  @Autowired private FixedAssetRepository fixedAssetRepository;
  @Autowired private DepreciationEntryRepository depreciationEntryRepository;
  @Autowired private JournalEntryRepository journalEntryRepository;

  private Long janId;
  private Long febId;
  private Long marId;
  private Long assetAccountId;
  private Long cashAccountId;

  @BeforeEach
  void setUp() {
    FiscalYear fy =
        fiscalYearRepository.save(
            FiscalYear.of(2025, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31)));
    janId = savePeriod(fy, 1, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31));
    febId = savePeriod(fy, 2, LocalDate.of(2025, 2, 1), LocalDate.of(2025, 2, 28));
    marId = savePeriod(fy, 3, LocalDate.of(2025, 3, 1), LocalDate.of(2025, 3, 31));
    assetAccountId = accountId("20800", "비품", AccountType.ASSET, NormalBalance.DEBIT);
    cashAccountId = accountId("10100", "현금", AccountType.ASSET, NormalBalance.DEBIT);
    configureAccounts();
  }

  private Long savePeriod(FiscalYear fy, int n, LocalDate s, LocalDate e) {
    return fiscalPeriodRepository.save(FiscalPeriod.of(fy, n, s, e)).getId();
  }

  private Long accountId(String code, String name, AccountType type, NormalBalance nb) {
    return accountRepository.save(Account.of(code, name, type, nb, null, false)).getId();
  }

  private void configureAccounts() {
    authenticate("admin", "finance:setting:write");
    Long expense = accountId("81800", "감가상각비", AccountType.EXPENSE, NormalBalance.DEBIT);
    Long accumulated = accountId("20900", "감가상각누계액", AccountType.ASSET, NormalBalance.CREDIT);
    Long gain = accountId("91100", "유형자산처분이익", AccountType.REVENUE, NormalBalance.CREDIT);
    Long loss = accountId("95100", "유형자산처분손실", AccountType.EXPENSE, NormalBalance.DEBIT);
    baseCurrencyService.updateDepreciationAccounts(
        new DepreciationAccountUpdateRequest(expense, accumulated, gain, loss));
  }

  /** 취득 1,200,000·12개월 정액(100,000/월) 자산을 1월 취득으로 등록. */
  private Long registerJanAsset(String code) {
    authenticate("creator", "finance:write");
    return fixedAssetService
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
  }

  @Test
  void dispose_inMarch_catchesUpJanFebExcludingDisposalMonth() {
    // 상각 미실행 자산을 3월에 처분 → catch-up이 1·2월만 상각(3월=처분월 제외).
    // 누계 200,000·장부 1,000,000 → 1,050,000 매각 시 처분이익 50,000.
    Long assetId = registerJanAsset("FA-CU-1");

    authenticate("creator", "finance:write");
    var result =
        fixedAssetService.dispose(
            assetId,
            new FixedAssetDisposeRequest(
                LocalDate.of(2025, 3, 15), new BigDecimal("1050000"), cashAccountId));

    assertThat(result.status().name()).isEqualTo("DISPOSED");

    var asset = fixedAssetRepository.findById(assetId).orElseThrow();
    assertThat(asset.getAccumulatedDepreciation()).isEqualByComparingTo("200000");
    assertThat(asset.bookValue()).isEqualByComparingTo("1000000");

    // 상각 이력: 1·2월만(처분월 3월 제외)
    List<DepreciationEntry> entries =
        depreciationEntryRepository.findByFixedAssetIdOrderByFiscalPeriodIdAsc(assetId);
    assertThat(entries)
        .extracting(DepreciationEntry::getFiscalPeriodId)
        .containsExactly(janId, febId);
    assertThat(entries).extracting(DepreciationEntry::getFiscalPeriodId).doesNotContain(marId);

    // 처분 분개: (차)누계액 200,000·현금 1,050,000 (대)자산 1,200,000·처분이익 50,000 = 1,250,000 균형
    var je =
        journalEntryRepository
            .findByReferenceTypeAndReferenceId(ReferenceTypes.ASSET_DISPOSAL, assetId)
            .orElseThrow();
    assertThat(je.isBalanced()).isTrue();
    assertThat(je.getTotalDebit()).isEqualByComparingTo("1250000");
    assertThat(je.getTotalCredit()).isEqualByComparingTo("1250000");
  }

  @Test
  void dispose_partiallyDepreciated_catchesUpRemainingOnly() {
    // 1월만 이미 상각된 자산을 3월 처분 → catch-up이 2월만 추가(멱등: 1월 중복 없음). 누계 200,000.
    Long assetId = registerJanAsset("FA-CU-2");
    authenticate("creator", "finance:write");
    depreciationPostingService.runForPeriod(janId); // 1월 선상각

    fixedAssetService.dispose(
        assetId,
        new FixedAssetDisposeRequest(
            LocalDate.of(2025, 3, 20), new BigDecimal("1050000"), cashAccountId));

    var asset = fixedAssetRepository.findById(assetId).orElseThrow();
    assertThat(asset.getAccumulatedDepreciation()).isEqualByComparingTo("200000");
    assertThat(depreciationEntryRepository.findByFixedAssetIdOrderByFiscalPeriodIdAsc(assetId))
        .extracting(DepreciationEntry::getFiscalPeriodId)
        .containsExactly(janId, febId);
  }

  @Test
  void dispose_inAcquisitionMonth_noDepreciation() {
    // 취득월(1월)에 처분 → 처분월 미상각 + 직전 기간 없음 → 상각 0, 장부=취득원가.
    Long assetId = registerJanAsset("FA-CU-3");
    authenticate("creator", "finance:write");
    fixedAssetService.dispose(
        assetId,
        new FixedAssetDisposeRequest(
            LocalDate.of(2025, 1, 20), new BigDecimal("1200000"), cashAccountId));

    var asset = fixedAssetRepository.findById(assetId).orElseThrow();
    assertThat(asset.getAccumulatedDepreciation()).isEqualByComparingTo("0");
    assertThat(depreciationEntryRepository.findByFixedAssetIdOrderByFiscalPeriodIdAsc(assetId))
        .isEmpty();
  }
}
