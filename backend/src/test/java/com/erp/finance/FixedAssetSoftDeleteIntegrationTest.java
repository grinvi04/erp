package com.erp.finance;

import static org.assertj.core.api.Assertions.assertThat;

import com.erp.common.AbstractIntegrationTest;
import com.erp.finance.domain.model.Account;
import com.erp.finance.domain.model.AccountType;
import com.erp.finance.domain.model.DepreciationEntry;
import com.erp.finance.domain.model.DepreciationMethod;
import com.erp.finance.domain.model.FiscalPeriod;
import com.erp.finance.domain.model.FiscalYear;
import com.erp.finance.domain.model.FixedAsset;
import com.erp.finance.domain.model.NormalBalance;
import com.erp.finance.domain.repository.AccountRepository;
import com.erp.finance.domain.repository.DepreciationEntryRepository;
import com.erp.finance.domain.repository.FiscalPeriodRepository;
import com.erp.finance.domain.repository.FiscalYearRepository;
import com.erp.finance.domain.repository.FixedAssetRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

/**
 * 소프트삭제(deleted_at) 행이 목록·조회에서 제외되는지 — 신규 엔티티별 회귀
 * 가드(db-standards: @SQLRestriction은 @MappedSuperclass에서 상속되지 않아 엔티티마다 검증 필수).
 * FixedAsset·DepreciationEntry 각각 확인.
 */
@Transactional
class FixedAssetSoftDeleteIntegrationTest extends AbstractIntegrationTest {

  @Autowired private FixedAssetRepository fixedAssetRepository;
  @Autowired private DepreciationEntryRepository depreciationEntryRepository;
  @Autowired private AccountRepository accountRepository;
  @Autowired private FiscalYearRepository fiscalYearRepository;
  @Autowired private FiscalPeriodRepository fiscalPeriodRepository;
  @PersistenceContext private EntityManager entityManager;

  @BeforeEach
  void setUp() {
    authenticate("finance-admin", "finance:read", "finance:write");
  }

  @Test
  void softDeletedFixedAsset_excludedFromList() {
    Account assetAccount =
        accountRepository.save(
            Account.of("20800", "비품", AccountType.ASSET, NormalBalance.DEBIT, null, false));
    FixedAsset asset =
        fixedAssetRepository.save(
            FixedAsset.register(
                "FA-SD",
                "노트북",
                LocalDate.of(2025, 1, 1),
                new BigDecimal("1200000"),
                BigDecimal.ZERO,
                12,
                DepreciationMethod.STRAIGHT_LINE,
                null,
                assetAccount));

    asset.softDelete();
    fixedAssetRepository.saveAndFlush(asset);
    entityManager.clear();

    assertThat(fixedAssetRepository.findByOrderByIdDesc(PageRequest.of(0, 20)).getContent())
        .as("소프트삭제된 고정자산은 목록에서 제외되어야 한다")
        .extracting(FixedAsset::getId)
        .doesNotContain(asset.getId());
    assertThat(fixedAssetRepository.findById(asset.getId())).isEmpty();
  }

  @Test
  void softDeletedDepreciationEntry_excludedFromHistory() {
    Account assetAccount =
        accountRepository.save(
            Account.of("20801", "비품2", AccountType.ASSET, NormalBalance.DEBIT, null, false));
    FixedAsset asset =
        fixedAssetRepository.save(
            FixedAsset.register(
                "FA-SD-2",
                "노트북2",
                LocalDate.of(2025, 1, 1),
                new BigDecimal("1200000"),
                BigDecimal.ZERO,
                12,
                DepreciationMethod.STRAIGHT_LINE,
                null,
                assetAccount));
    FiscalYear fy =
        fiscalYearRepository.save(
            FiscalYear.of(2025, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31)));
    FiscalPeriod period =
        fiscalPeriodRepository.save(
            FiscalPeriod.of(fy, 1, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31)));
    DepreciationEntry entry =
        depreciationEntryRepository.save(
            DepreciationEntry.of(asset.getId(), period.getId(), new BigDecimal("100000"), null));

    entry.softDelete();
    depreciationEntryRepository.saveAndFlush(entry);
    entityManager.clear();

    assertThat(
            depreciationEntryRepository.findByFixedAssetIdOrderByFiscalPeriodIdAsc(asset.getId()))
        .as("소프트삭제된 상각 이력은 조회에서 제외되어야 한다")
        .isEmpty();
  }
}
