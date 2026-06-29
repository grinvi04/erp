package com.erp.finance.application.service;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.response.PageResponse;
import com.erp.common.security.Permission;
import com.erp.common.security.PermissionChecker;
import com.erp.finance.application.ReferenceTypes;
import com.erp.finance.application.dto.DepreciationEntryResponse;
import com.erp.finance.application.dto.FixedAssetCreateRequest;
import com.erp.finance.application.dto.FixedAssetDisposeRequest;
import com.erp.finance.application.dto.FixedAssetResponse;
import com.erp.finance.application.dto.JournalEntryCreateRequest;
import com.erp.finance.application.dto.JournalLineRequest;
import com.erp.finance.application.service.BaseCurrencyService.DepreciationAccounts;
import com.erp.finance.application.service.BaseCurrencyService.ImpairmentAccounts;
import com.erp.finance.domain.model.Account;
import com.erp.finance.domain.model.FiscalPeriod;
import com.erp.finance.domain.model.FixedAsset;
import com.erp.finance.domain.model.JournalEntryType;
import com.erp.finance.domain.repository.AccountRepository;
import com.erp.finance.domain.repository.DepreciationEntryRepository;
import com.erp.finance.domain.repository.FiscalPeriodRepository;
import com.erp.finance.domain.repository.FixedAssetRepository;
import com.erp.finance.domain.repository.JournalEntryRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 고정자산 대장 — 등록·조회·처분. 월별 상각은 별도 서비스(DepreciationPostingService). */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FixedAssetService {

  private final FixedAssetRepository fixedAssetRepository;
  private final DepreciationEntryRepository depreciationEntryRepository;
  private final AccountRepository accountRepository;
  private final JournalEntryService journalEntryService;
  private final JournalEntryRepository journalEntryRepository;
  private final FiscalPeriodRepository fiscalPeriodRepository;
  private final BaseCurrencyService baseCurrencyService;
  private final DepreciationPostingService depreciationPostingService;
  private final PermissionChecker permissionChecker;

  public PageResponse<FixedAssetResponse> findAll(Pageable pageable) {
    permissionChecker.require(Permission.FINANCE_READ);
    return PageResponse.from(
        fixedAssetRepository.findByOrderByIdDesc(pageable).map(FixedAssetResponse::from));
  }

  public FixedAssetResponse findById(Long id) {
    permissionChecker.require(Permission.FINANCE_READ);
    return FixedAssetResponse.from(getOrThrow(id));
  }

  /** 자산의 감가상각 이력(기간 오름차순) — 상세 화면 표시용. */
  public List<DepreciationEntryResponse> findHistory(Long id) {
    permissionChecker.require(Permission.FINANCE_READ);
    getOrThrow(id);
    return depreciationEntryRepository.findByFixedAssetIdOrderByFiscalPeriodIdAsc(id).stream()
        .map(DepreciationEntryResponse::from)
        .toList();
  }

  @Transactional
  public FixedAssetResponse create(FixedAssetCreateRequest request) {
    permissionChecker.require(Permission.FINANCE_WRITE);
    if (fixedAssetRepository.existsByCode(request.code())) {
      throw new ErpException(ErrorCode.DUPLICATE_CODE, "이미 존재하는 자산 코드: " + request.code());
    }
    Account assetAccount =
        accountRepository
            .findById(request.assetAccountId())
            .orElseThrow(() -> new ErpException(ErrorCode.ACCOUNT_NOT_FOUND));
    FixedAsset asset =
        FixedAsset.register(
            request.code(),
            request.name(),
            request.acquisitionDate(),
            request.acquisitionCost(),
            request.residualValue(),
            request.usefulLifeMonths(),
            request.method(),
            request.decliningAnnualRate(),
            assetAccount);
    return FixedAssetResponse.from(fixedAssetRepository.save(asset));
  }

  /**
   * 자산 처분(매각·폐기) → 처분손익 GL 자동 분개(DRAFT)·상태 전이. 분개:
   *
   * <pre>(차) 감가상각누계액·현금[대가]·처분손실   (대) 자산[취득원가]·처분이익</pre>
   *
   * 처분이익=대가−장부가액(양수). 라인이 필요한 계정(누계액·처분손익)이 미설정이면 빈 분개를 만들지 않고 차단한다. 이미 처분된 자산은 거부한다.
   */
  @Transactional
  public FixedAssetResponse dispose(Long id, FixedAssetDisposeRequest request) {
    permissionChecker.require(Permission.FINANCE_WRITE);
    FixedAsset asset = getOrThrow(id);
    if (!asset.isActive()) {
      throw new ErpException(ErrorCode.FIXED_ASSET_ALREADY_DISPOSED);
    }
    FiscalPeriod period =
        fiscalPeriodRepository
            .findByStartDateLessThanEqualAndEndDateGreaterThanEqual(
                request.disposalDate(), request.disposalDate())
            .orElseThrow(() -> new ErpException(ErrorCode.FISCAL_PERIOD_NOT_FOUND));

    // 처분일까지 상각 반영(처분월 미상각) — 미처리 직전 기간을 catch-up해 장부가액을 현행화한 뒤 처분손익 계산.
    depreciationPostingService.catchUpBeforeDisposal(asset, request.disposalDate());

    postDisposal(asset, period, request);
    asset.dispose();
    return FixedAssetResponse.from(asset);
  }

  private void postDisposal(
      FixedAsset asset, FiscalPeriod period, FixedAssetDisposeRequest request) {
    DepreciationAccounts accounts = baseCurrencyService.currentDepreciationAccounts();
    BigDecimal accumulated = asset.getAccumulatedDepreciation();
    BigDecimal cost = asset.getAcquisitionCost();
    BigDecimal proceeds = request.proceeds();
    BigDecimal gainLoss = proceeds.subtract(asset.bookValue()); // >0 처분이익, <0 처분손실

    List<JournalLineRequest> lines = new ArrayList<>();
    // 차변: 감가상각누계액 제거
    if (accumulated.signum() > 0) {
      requireAccount(accounts.accumulatedAccount(), ErrorCode.DISPOSAL_ACCOUNT_NOT_CONFIGURED);
      lines.add(
          new JournalLineRequest(
              accounts.accumulatedAccount().getId(),
              accumulated,
              BigDecimal.ZERO,
              "감가상각누계액 제거: " + asset.getCode(),
              null));
    }
    // 차변: 손상차손누계액 제거(손상된 자산) — 자산을 취득원가로 제거하려면 손상누계 차감분도 함께 청산해야 정합.
    BigDecimal accumulatedImpairment = asset.getAccumulatedImpairment();
    if (accumulatedImpairment.signum() > 0) {
      ImpairmentAccounts impairmentAccounts = baseCurrencyService.currentImpairmentAccounts();
      requireAccount(
          impairmentAccounts.accumulatedAccount(), ErrorCode.IMPAIRMENT_ACCOUNT_NOT_CONFIGURED);
      lines.add(
          new JournalLineRequest(
              impairmentAccounts.accumulatedAccount().getId(),
              accumulatedImpairment,
              BigDecimal.ZERO,
              "손상차손누계액 제거: " + asset.getCode(),
              null));
    }
    // 차변: 처분 대가(현금·미수금)
    if (proceeds.signum() > 0) {
      if (request.proceedsAccountId() == null) {
        throw new ErpException(ErrorCode.INVALID_INPUT, "처분 대가 수령 계정이 필요합니다");
      }
      Account proceedsAccount =
          accountRepository
              .findById(request.proceedsAccountId())
              .orElseThrow(() -> new ErpException(ErrorCode.ACCOUNT_NOT_FOUND));
      lines.add(
          new JournalLineRequest(
              proceedsAccount.getId(),
              proceeds,
              BigDecimal.ZERO,
              "처분 대가: " + asset.getCode(),
              null));
    }
    // 차변: 처분손실
    if (gainLoss.signum() < 0) {
      requireAccount(accounts.disposalLossAccount(), ErrorCode.DISPOSAL_ACCOUNT_NOT_CONFIGURED);
      lines.add(
          new JournalLineRequest(
              accounts.disposalLossAccount().getId(),
              gainLoss.negate(),
              BigDecimal.ZERO,
              "유형자산처분손실: " + asset.getCode(),
              null));
    }
    // 대변: 자산(취득원가) 제거
    lines.add(
        new JournalLineRequest(
            asset.getAssetAccount().getId(),
            BigDecimal.ZERO,
            cost,
            "유형자산 제거: " + asset.getCode(),
            null));
    // 대변: 처분이익
    if (gainLoss.signum() > 0) {
      requireAccount(accounts.disposalGainAccount(), ErrorCode.DISPOSAL_ACCOUNT_NOT_CONFIGURED);
      lines.add(
          new JournalLineRequest(
              accounts.disposalGainAccount().getId(),
              BigDecimal.ZERO,
              gainLoss,
              "유형자산처분이익: " + asset.getCode(),
              null));
    }

    JournalEntryCreateRequest jeRequest =
        new JournalEntryCreateRequest(
            request.disposalDate(),
            period.getId(),
            "고정자산 처분 " + asset.getCode(),
            JournalEntryType.ADJUSTMENT,
            baseCurrencyService.currentBaseCurrencyCode(),
            lines);
    Long journalEntryId = journalEntryService.createInternal(jeRequest).id();
    journalEntryRepository
        .findById(journalEntryId)
        .ifPresent(je -> je.linkReference(ReferenceTypes.ASSET_DISPOSAL, asset.getId()));
  }

  private static void requireAccount(Account account, ErrorCode errorCode) {
    if (account == null) {
      throw new ErpException(errorCode);
    }
  }

  FixedAsset getOrThrow(Long id) {
    return fixedAssetRepository
        .findById(id)
        .orElseThrow(() -> new ErpException(ErrorCode.RESOURCE_NOT_FOUND, "고정자산을 찾을 수 없습니다"));
  }
}
