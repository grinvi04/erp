package com.erp.finance.application.service;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.security.Permission;
import com.erp.common.security.PermissionChecker;
import com.erp.finance.application.ReferenceTypes;
import com.erp.finance.application.dto.DepreciationRunResponse;
import com.erp.finance.application.dto.JournalEntryCreateRequest;
import com.erp.finance.application.dto.JournalLineRequest;
import com.erp.finance.application.service.BaseCurrencyService.DepreciationAccounts;
import com.erp.finance.domain.model.DepreciationEntry;
import com.erp.finance.domain.model.FiscalPeriod;
import com.erp.finance.domain.model.FixedAsset;
import com.erp.finance.domain.model.FixedAssetStatus;
import com.erp.finance.domain.model.JournalEntryType;
import com.erp.finance.domain.repository.DepreciationEntryRepository;
import com.erp.finance.domain.repository.FiscalPeriodRepository;
import com.erp.finance.domain.repository.FixedAssetRepository;
import com.erp.finance.domain.repository.JournalEntryRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 월별 감가상각 처리 → GL 자동 분개(DRAFT). 가동 자산별 당월 상각액으로:
 *
 * <pre>(차) 감가상각비 [비용]   (대) 감가상각누계액 [자산 차감] = 당월 상각액</pre>
 *
 * 누계상각액·장부가액을 갱신하고 상각 이력(DepreciationEntry)을 남긴다. (자산,기간) UNIQUE로 같은 기간 재처리는 건너뛴다(멱등). 전기(POST)는
 * 회계담당이 별도 수행한다(여기선 DRAFT만). 감가상각비·누계액 계정이 설정되지 않았으면 빈 분개를 만들지 않고 차단한다.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DepreciationPostingService {

  private final JournalEntryService journalEntryService;
  private final FiscalPeriodRepository fiscalPeriodRepository;
  private final FixedAssetRepository fixedAssetRepository;
  private final DepreciationEntryRepository depreciationEntryRepository;
  private final JournalEntryRepository journalEntryRepository;
  private final BaseCurrencyService baseCurrencyService;
  private final PermissionChecker permissionChecker;

  /**
   * 대상 회계기간의 가동 자산 전체에 당월 상각을 처리한다. 회계기간이 OPEN이 아니거나 상각 계정이 미설정이면 차단한다. 이미 처리한 (자산,기간)·상각액 0(잔존
   * 도달·내용연수 종료)은 건너뛴다.
   */
  @Transactional
  public DepreciationRunResponse runForPeriod(Long fiscalPeriodId) {
    permissionChecker.require(Permission.FINANCE_WRITE);
    FiscalPeriod period =
        fiscalPeriodRepository
            .findById(fiscalPeriodId)
            .orElseThrow(() -> new ErpException(ErrorCode.FISCAL_PERIOD_NOT_FOUND));
    if (!period.isOpen()) {
      throw new ErpException(ErrorCode.FISCAL_PERIOD_CLOSED);
    }
    DepreciationAccounts accounts = baseCurrencyService.currentDepreciationAccounts();
    if (accounts.expenseAccount() == null || accounts.accumulatedAccount() == null) {
      throw new ErpException(ErrorCode.DEPRECIATION_ACCOUNT_NOT_CONFIGURED);
    }

    List<FixedAsset> assets = fixedAssetRepository.findByStatus(FixedAssetStatus.ACTIVE);
    int processed = 0;
    int skipped = 0;
    BigDecimal total = BigDecimal.ZERO;
    for (FixedAsset asset : assets) {
      // 취득월부터 상각 — 취득일이 해당 회계기간 종료일 이후면 아직 미취득이므로 건너뛴다(취득 전 상각 방지).
      if (asset.getAcquisitionDate().isAfter(period.getEndDate())) {
        skipped++;
        continue;
      }
      if (depreciationEntryRepository.existsByFixedAssetIdAndFiscalPeriodId(
          asset.getId(), fiscalPeriodId)) {
        skipped++;
        continue;
      }
      BigDecimal amount = asset.monthlyDepreciation();
      if (amount.signum() <= 0) {
        skipped++;
        continue;
      }
      Long journalEntryId = postDepreciation(asset, period, accounts, amount);
      asset.applyDepreciation(amount);
      depreciationEntryRepository.save(
          DepreciationEntry.of(asset.getId(), fiscalPeriodId, amount, journalEntryId));
      processed++;
      total = total.add(amount);
    }
    log.atInfo()
        .addKeyValue("event", "DEPRECIATION_RUN")
        .addKeyValue("fiscalPeriodId", fiscalPeriodId)
        .addKeyValue("processed", processed)
        .addKeyValue("skipped", skipped)
        .log("월 감가상각 처리");
    return new DepreciationRunResponse(fiscalPeriodId, processed, skipped, total);
  }

  /**
   * 처분 직전까지 catch-up 상각 — 처분월 미상각(취득월 포함·처분월 제외) 정책. 처분월 시작일 이전에 끝난 OPEN 회계기간 중 미처리분에 당월 상각을 순차 전기해
   * 장부가액을 처분 시점까지 현행화한다. 처분(dispose)에서 호출되며 권한은 호출처(FINANCE_WRITE)가 소유한다. 전기할 기간이 있으면 상각 계정 설정이
   * 필요하다(마감 기간은 전기 불가로 건너뛴다).
   */
  @Transactional
  public void catchUpBeforeDisposal(FixedAsset asset, LocalDate disposalDate) {
    FiscalPeriod disposalPeriod =
        fiscalPeriodRepository
            .findByStartDateLessThanEqualAndEndDateGreaterThanEqual(disposalDate, disposalDate)
            .orElseThrow(() -> new ErpException(ErrorCode.FISCAL_PERIOD_NOT_FOUND));
    List<FiscalPeriod> priorPeriods =
        fiscalPeriodRepository.findByEndDateLessThanOrderByStartDateAsc(
            disposalPeriod.getStartDate());
    DepreciationAccounts accounts = null;
    for (FiscalPeriod p : priorPeriods) {
      if (!p.isOpen() || asset.getAcquisitionDate().isAfter(p.getEndDate())) {
        continue; // 마감 기간 전기 불가 / 미취득
      }
      if (depreciationEntryRepository.existsByFixedAssetIdAndFiscalPeriodId(
          asset.getId(), p.getId())) {
        continue; // 이미 처리
      }
      BigDecimal amount = asset.monthlyDepreciation();
      if (amount.signum() <= 0) {
        continue; // 상각 완료(잔존 도달)
      }
      if (accounts == null) {
        accounts = baseCurrencyService.currentDepreciationAccounts();
        if (accounts.expenseAccount() == null || accounts.accumulatedAccount() == null) {
          throw new ErpException(ErrorCode.DEPRECIATION_ACCOUNT_NOT_CONFIGURED);
        }
      }
      Long journalEntryId = postDepreciation(asset, p, accounts, amount);
      asset.applyDepreciation(amount);
      depreciationEntryRepository.save(
          DepreciationEntry.of(asset.getId(), p.getId(), amount, journalEntryId));
    }
  }

  private Long postDepreciation(
      FixedAsset asset, FiscalPeriod period, DepreciationAccounts accounts, BigDecimal amount) {
    List<JournalLineRequest> lines =
        List.of(
            new JournalLineRequest(
                accounts.expenseAccount().getId(),
                amount,
                BigDecimal.ZERO,
                "감가상각비: " + asset.getCode(),
                null),
            new JournalLineRequest(
                accounts.accumulatedAccount().getId(),
                BigDecimal.ZERO,
                amount,
                "감가상각누계액: " + asset.getCode(),
                null));
    JournalEntryCreateRequest request =
        new JournalEntryCreateRequest(
            period.getEndDate(),
            period.getId(),
            "감가상각 " + asset.getCode(),
            JournalEntryType.ADJUSTMENT,
            baseCurrencyService.currentBaseCurrencyCode(),
            lines);
    Long journalEntryId = journalEntryService.createInternal(request).id();
    journalEntryRepository
        .findById(journalEntryId)
        .ifPresent(je -> je.linkReference(ReferenceTypes.DEPRECIATION, asset.getId()));
    return journalEntryId;
  }
}
