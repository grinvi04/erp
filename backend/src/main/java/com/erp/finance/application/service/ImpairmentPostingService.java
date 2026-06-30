package com.erp.finance.application.service;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.security.Permission;
import com.erp.common.security.PermissionChecker;
import com.erp.finance.application.ReferenceTypes;
import com.erp.finance.application.dto.ImpairmentRecognizeResponse;
import com.erp.finance.application.dto.ImpairmentReversalResponse;
import com.erp.finance.application.dto.JournalEntryCreateRequest;
import com.erp.finance.application.dto.JournalLineRequest;
import com.erp.finance.application.service.BaseCurrencyService.ImpairmentAccounts;
import com.erp.finance.domain.model.FiscalPeriod;
import com.erp.finance.domain.model.FixedAsset;
import com.erp.finance.domain.model.ImpairmentEntry;
import com.erp.finance.domain.model.ImpairmentEntryType;
import com.erp.finance.domain.model.JournalEntryType;
import com.erp.finance.domain.repository.FiscalPeriodRepository;
import com.erp.finance.domain.repository.FixedAssetRepository;
import com.erp.finance.domain.repository.ImpairmentEntryRepository;
import com.erp.finance.domain.repository.JournalEntryRepository;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 손상차손 인식 → GL 자동 분개(DRAFT). 회수가능액이 장부가액보다 작을 때 차액을:
 *
 * <pre>(차) 손상차손비 [비용]   (대) 손상차손누계액 [자산 차감] = 손상차손액</pre>
 *
 * 으로 분개하고 자산 손상차손누계액을 갱신하며 손상 이력(ImpairmentEntry)을 남긴다. 측정 전에 인식기간까지 감가상각을 catch-up해 장부가액을
 * 현행화한다(K-IFRS). (자산,기간) UNIQUE로 같은 기간 중복 인식을 막는다(멱등). 전기(POST)는 회계담당이 별도 수행한다(DRAFT만).
 * 손상차손비·손상차손누계액 계정이 설정되지 않았으면 차단한다.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ImpairmentPostingService {

  private final JournalEntryService journalEntryService;
  private final FiscalPeriodRepository fiscalPeriodRepository;
  private final FixedAssetRepository fixedAssetRepository;
  private final ImpairmentEntryRepository impairmentEntryRepository;
  private final JournalEntryRepository journalEntryRepository;
  private final BaseCurrencyService baseCurrencyService;
  private final DepreciationPostingService depreciationPostingService;
  private final PermissionChecker permissionChecker;

  /**
   * 자산 손상차손 인식 — 인식기간까지 상각 catch-up 후 손상차손액(장부가액−회수가능액)을 분개한다. 가동 자산·OPEN 기간·손상 계정 설정·회수가능액 0
   * 이상·미인식 (자산,기간)·회수가능액 &lt; 장부가액이 전제. 정액은 손상 후 잔여내용연수로 월상각액을 재배분한다.
   */
  @Transactional
  public ImpairmentRecognizeResponse recognizeImpairment(
      Long assetId, Long fiscalPeriodId, BigDecimal recoverableAmount) {
    permissionChecker.require(Permission.FINANCE_WRITE);
    if (recoverableAmount == null || recoverableAmount.signum() < 0) {
      throw new ErpException(ErrorCode.INVALID_INPUT, "회수가능액은 0 이상이어야 합니다");
    }
    FixedAsset asset =
        fixedAssetRepository
            .findById(assetId)
            .orElseThrow(() -> new ErpException(ErrorCode.RESOURCE_NOT_FOUND, "고정자산을 찾을 수 없습니다"));
    if (!asset.isActive()) {
      throw new ErpException(ErrorCode.FIXED_ASSET_ALREADY_DISPOSED);
    }
    FiscalPeriod period =
        fiscalPeriodRepository
            .findById(fiscalPeriodId)
            .orElseThrow(() -> new ErpException(ErrorCode.FISCAL_PERIOD_NOT_FOUND));
    if (!period.isOpen()) {
      throw new ErpException(ErrorCode.FISCAL_PERIOD_CLOSED);
    }
    if (period.getEndDate().isBefore(asset.getAcquisitionDate())) {
      throw new ErpException(ErrorCode.INVALID_INPUT, "취득일 이전 회계기간에는 손상을 인식할 수 없습니다");
    }
    if (impairmentEntryRepository.existsByFixedAssetIdAndFiscalPeriodIdAndEntryType(
        assetId, fiscalPeriodId, ImpairmentEntryType.IMPAIRMENT)) {
      throw new ErpException(ErrorCode.IMPAIRMENT_ALREADY_RECOGNIZED);
    }
    ImpairmentAccounts accounts = baseCurrencyService.currentImpairmentAccounts();
    if (accounts.lossAccount() == null || accounts.accumulatedAccount() == null) {
      throw new ErpException(ErrorCode.IMPAIRMENT_ACCOUNT_NOT_CONFIGURED);
    }

    // 측정 전 인식기간까지 상각을 현행화(K-IFRS — 손상은 기간 상각 반영 후 기말 장부가액으로 판정).
    depreciationPostingService.catchUpThroughPeriod(asset, period);

    BigDecimal bookValueBefore = asset.bookValue();
    BigDecimal loss = bookValueBefore.subtract(recoverableAmount);
    if (loss.signum() <= 0) {
      throw new ErpException(ErrorCode.IMPAIRMENT_NOT_REQUIRED);
    }

    int remainingMonths = asset.getUsefulLifeMonths() - monthsElapsed(asset, period);
    Long journalEntryId = postImpairment(asset, period, accounts, loss);
    asset.applyImpairment(loss, remainingMonths);
    ImpairmentEntry entry =
        impairmentEntryRepository.save(
            ImpairmentEntry.of(
                assetId, fiscalPeriodId, recoverableAmount, bookValueBefore, loss, journalEntryId));

    log.atInfo()
        .addKeyValue("event", "IMPAIRMENT_RECOGNIZED")
        .addKeyValue("assetId", assetId)
        .addKeyValue("fiscalPeriodId", fiscalPeriodId)
        .addKeyValue("impairmentLoss", loss)
        .log("손상차손 인식");
    return ImpairmentRecognizeResponse.of(entry, asset.bookValue());
  }

  /**
   * 자산 손상차손 환입 — 인식기간까지 상각 catch-up 후 한도(손상 없었을 경우 장부금액) 내에서 환입한다. 환입액 = min(min(회수가능액, 한도) − 장부가액,
   * 손상누계액). 가동 자산·OPEN 기간·환입 계정 설정·회수가능액 0 이상·미환입 (자산,기간)·환입액 &gt; 0이 전제. 정액은 환입 후 잔여내용연수로 월상각액을
   * 재배분한다.
   */
  @Transactional
  public ImpairmentReversalResponse reverseImpairment(
      Long assetId, Long fiscalPeriodId, BigDecimal recoverableAmount) {
    permissionChecker.require(Permission.FINANCE_WRITE);
    if (recoverableAmount == null || recoverableAmount.signum() < 0) {
      throw new ErpException(ErrorCode.INVALID_INPUT, "회수가능액은 0 이상이어야 합니다");
    }
    FixedAsset asset =
        fixedAssetRepository
            .findById(assetId)
            .orElseThrow(() -> new ErpException(ErrorCode.RESOURCE_NOT_FOUND, "고정자산을 찾을 수 없습니다"));
    if (!asset.isActive()) {
      throw new ErpException(ErrorCode.FIXED_ASSET_ALREADY_DISPOSED);
    }
    FiscalPeriod period =
        fiscalPeriodRepository
            .findById(fiscalPeriodId)
            .orElseThrow(() -> new ErpException(ErrorCode.FISCAL_PERIOD_NOT_FOUND));
    if (!period.isOpen()) {
      throw new ErpException(ErrorCode.FISCAL_PERIOD_CLOSED);
    }
    if (period.getEndDate().isBefore(asset.getAcquisitionDate())) {
      throw new ErpException(ErrorCode.INVALID_INPUT, "취득일 이전 회계기간에는 환입을 인식할 수 없습니다");
    }
    if (impairmentEntryRepository.existsByFixedAssetIdAndFiscalPeriodIdAndEntryType(
        assetId, fiscalPeriodId, ImpairmentEntryType.REVERSAL)) {
      throw new ErpException(ErrorCode.IMPAIRMENT_REVERSAL_ALREADY_RECOGNIZED);
    }
    ImpairmentAccounts accounts = baseCurrencyService.currentImpairmentAccounts();
    if (accounts.accumulatedAccount() == null || accounts.reversalAccount() == null) {
      throw new ErpException(ErrorCode.IMPAIRMENT_REVERSAL_ACCOUNT_NOT_CONFIGURED);
    }

    // 측정 전 인식기간까지 상각을 현행화(환입은 기간 상각 반영 후 기말 장부가액으로 판정).
    depreciationPostingService.catchUpThroughPeriod(asset, period);

    int monthsElapsed = monthsElapsed(asset, period);
    BigDecimal ceiling = asset.noImpairmentCarryingAmount(monthsElapsed);
    BigDecimal bookValueBefore = asset.bookValue();
    // 한도·회수가능액·손상누계액 3중 상한 — 환입 후 장부가액이 한도·회수가능액을 넘지 않고, 인식한 손상보다 많이 환입하지 않는다.
    BigDecimal reversal =
        recoverableAmount
            .min(ceiling)
            .subtract(bookValueBefore)
            .min(asset.getAccumulatedImpairment());
    if (reversal.signum() <= 0) {
      throw new ErpException(ErrorCode.IMPAIRMENT_REVERSAL_NOT_REQUIRED);
    }

    int remainingMonths = asset.getUsefulLifeMonths() - monthsElapsed;
    Long journalEntryId = postReversal(asset, period, accounts, reversal);
    asset.applyReversal(reversal, remainingMonths);
    ImpairmentEntry entry =
        impairmentEntryRepository.save(
            ImpairmentEntry.reversal(
                assetId,
                fiscalPeriodId,
                recoverableAmount,
                bookValueBefore,
                reversal,
                journalEntryId));

    log.atInfo()
        .addKeyValue("event", "IMPAIRMENT_REVERSED")
        .addKeyValue("assetId", assetId)
        .addKeyValue("fiscalPeriodId", fiscalPeriodId)
        .addKeyValue("reversalAmount", reversal)
        .log("손상차손 환입");
    return ImpairmentReversalResponse.of(entry, asset.bookValue());
  }

  /** 취득월부터 인식기간까지의 경과 개월수(취득월·인식월 포함, 달력 기준). 마감으로 건너뛴 기간이 있어도 캘린더 경과로 산정한다. */
  private static int monthsElapsed(FixedAsset asset, FiscalPeriod period) {
    return (int)
            ChronoUnit.MONTHS.between(
                YearMonth.from(asset.getAcquisitionDate()), YearMonth.from(period.getEndDate()))
        + 1;
  }

  private Long postImpairment(
      FixedAsset asset, FiscalPeriod period, ImpairmentAccounts accounts, BigDecimal amount) {
    List<JournalLineRequest> lines =
        List.of(
            new JournalLineRequest(
                accounts.lossAccount().getId(),
                amount,
                BigDecimal.ZERO,
                "손상차손: " + asset.getCode(),
                null),
            new JournalLineRequest(
                accounts.accumulatedAccount().getId(),
                BigDecimal.ZERO,
                amount,
                "손상차손누계액: " + asset.getCode(),
                null));
    JournalEntryCreateRequest request =
        new JournalEntryCreateRequest(
            period.getEndDate(),
            period.getId(),
            "손상차손 " + asset.getCode(),
            JournalEntryType.ADJUSTMENT,
            baseCurrencyService.currentBaseCurrencyCode(),
            lines);
    Long journalEntryId = journalEntryService.createInternal(request).id();
    journalEntryRepository
        .findById(journalEntryId)
        .ifPresent(je -> je.linkReference(ReferenceTypes.IMPAIRMENT, asset.getId()));
    return journalEntryId;
  }

  private Long postReversal(
      FixedAsset asset, FiscalPeriod period, ImpairmentAccounts accounts, BigDecimal amount) {
    List<JournalLineRequest> lines =
        List.of(
            new JournalLineRequest(
                accounts.accumulatedAccount().getId(),
                amount,
                BigDecimal.ZERO,
                "손상차손누계액 환입: " + asset.getCode(),
                null),
            new JournalLineRequest(
                accounts.reversalAccount().getId(),
                BigDecimal.ZERO,
                amount,
                "손상차손환입: " + asset.getCode(),
                null));
    JournalEntryCreateRequest request =
        new JournalEntryCreateRequest(
            period.getEndDate(),
            period.getId(),
            "손상차손 환입 " + asset.getCode(),
            JournalEntryType.ADJUSTMENT,
            baseCurrencyService.currentBaseCurrencyCode(),
            lines);
    Long journalEntryId = journalEntryService.createInternal(request).id();
    journalEntryRepository
        .findById(journalEntryId)
        .ifPresent(je -> je.linkReference(ReferenceTypes.IMPAIRMENT_REVERSAL, asset.getId()));
    return journalEntryId;
  }
}
