package com.erp.finance.application.service;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.security.Permission;
import com.erp.common.security.PermissionChecker;
import com.erp.finance.application.dto.BaseCurrencyResponse;
import com.erp.finance.application.dto.BaseCurrencyUpdateRequest;
import com.erp.finance.application.dto.DepreciationAccountResponse;
import com.erp.finance.application.dto.DepreciationAccountUpdateRequest;
import com.erp.finance.application.dto.FxGainLossAccountResponse;
import com.erp.finance.application.dto.FxGainLossAccountUpdateRequest;
import com.erp.finance.application.dto.ImpairmentAccountResponse;
import com.erp.finance.application.dto.ImpairmentAccountUpdateRequest;
import com.erp.finance.application.dto.VatAccountResponse;
import com.erp.finance.application.dto.VatAccountUpdateRequest;
import com.erp.finance.domain.model.Account;
import com.erp.finance.domain.model.TenantBaseCurrency;
import com.erp.finance.domain.repository.AccountRepository;
import com.erp.finance.domain.repository.ApInvoiceRepository;
import com.erp.finance.domain.repository.ArInvoiceRepository;
import com.erp.finance.domain.repository.JournalEntryRepository;
import com.erp.finance.domain.repository.TenantBaseCurrencyRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 테넌트 기준통화 설정 — 조회(미설정 시 KRW 기본)·변경(FINANCE_SETTING_WRITE). FiscalYear 마스터 패턴.
 *
 * <p>변경 가드: FX 거래는 생성 시점의 기준통화로 base_amount·exchange_rate를 스냅샷 저장한다(거래시점고정). 이후 기준통화를 바꾸면 과거 스냅샷이 새
 * 통화 코드로 잘못 라벨링돼 집계 의미가 어긋나므로, finance 거래(ap_invoice·ar_invoice·journal_entry)에 base_amount 스냅샷이
 * 하나라도 있으면 변경을 거부한다.
 *
 * <p><b>알려진 한계</b>: CRM Opportunity도 base_amount 스냅샷을 갖지만 모듈 경계상 finance에서 CRM repo를 직접 참조할 수 없어 이
 * 가드의 판정 대상에서 제외한다. 실무상 FX 거래가 생기면 보통 finance 거래(AP/AR/전표)가 함께 존재하므로 가드는 사실상 발동하며, opportunity만
 * 단독으로 스냅샷을 갖는 경우는 한계로 남는다 (필요 시 별도 SPI/이벤트로 확장 — 현재는 과한 추상화를 피한다).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BaseCurrencyService {

  private final TenantBaseCurrencyRepository repository;
  private final ApInvoiceRepository apInvoiceRepository;
  private final ArInvoiceRepository arInvoiceRepository;
  private final JournalEntryRepository journalEntryRepository;
  private final AccountRepository accountRepository;
  private final PermissionChecker permissionChecker;

  /** 환차손익 분개에 쓰일 환차이익·환차손 계정 — 둘 다 설정된 경우에만 전달(내부용). */
  public record FxGainLossAccounts(Account gainAccount, Account lossAccount) {}

  public BaseCurrencyResponse getBaseCurrency() {
    permissionChecker.require(Permission.FINANCE_READ);
    return BaseCurrencyResponse.of(currentBaseCurrencyCode());
  }

  /** 환산 등 내부 사용을 위한 현재 테넌트 기준통화 코드(미설정 시 KRW). 권한 검사 없음. */
  public String currentBaseCurrencyCode() {
    return repository
        .findFirstByOrderByIdAsc()
        .map(TenantBaseCurrency::getBaseCurrency)
        .orElse(TenantBaseCurrency.DEFAULT_BASE_CURRENCY);
  }

  @Transactional
  public BaseCurrencyResponse updateBaseCurrency(BaseCurrencyUpdateRequest request) {
    permissionChecker.require(Permission.FINANCE_SETTING_WRITE);
    // 실제 통화가 바뀔 때만 가드 — 동일 값 PUT(no-op)·최초 설정(KRW 기본 → KRW)은 허용한다.
    if (!request.baseCurrency().equals(currentBaseCurrencyCode()) && hasBaseAmountSnapshot()) {
      throw new ErpException(ErrorCode.BASE_CURRENCY_CHANGE_NOT_ALLOWED);
    }
    TenantBaseCurrency entity =
        repository
            .findFirstByOrderByIdAsc()
            .map(
                existing -> {
                  existing.changeBaseCurrency(request.baseCurrency());
                  return existing;
                })
            .orElseGet(() -> repository.save(TenantBaseCurrency.of(request.baseCurrency())));
    return BaseCurrencyResponse.of(entity.getBaseCurrency());
  }

  /** 환차손익 계정 설정 조회(FINANCE_READ). 미설정 항목은 null. */
  public FxGainLossAccountResponse getFxGainLossAccounts() {
    permissionChecker.require(Permission.FINANCE_READ);
    return repository
        .findFirstByOrderByIdAsc()
        .map(
            e ->
                FxGainLossAccountResponse.of(
                    e.getFxGainAccount() != null ? e.getFxGainAccount().getId() : null,
                    e.getFxLossAccount() != null ? e.getFxLossAccount().getId() : null))
        .orElseGet(() -> FxGainLossAccountResponse.of(null, null));
  }

  /**
   * 환차손익 계정 설정 변경(FINANCE_SETTING_WRITE). 환차이익·환차손 계정을 함께 지정·해제한다. 설정 행이 없으면 현재 기준통화(미설정 시 KRW)로 행을
   * 만들어 계정만 채운다(기준통화 변경 가드와 무관).
   */
  @Transactional
  public FxGainLossAccountResponse updateFxGainLossAccounts(
      FxGainLossAccountUpdateRequest request) {
    permissionChecker.require(Permission.FINANCE_SETTING_WRITE);
    Account gain = resolveAccount(request.fxGainAccountId());
    Account loss = resolveAccount(request.fxLossAccountId());
    TenantBaseCurrency entity =
        repository
            .findFirstByOrderByIdAsc()
            .orElseGet(() -> repository.save(TenantBaseCurrency.of(currentBaseCurrencyCode())));
    entity.assignFxAccounts(gain, loss);
    return FxGainLossAccountResponse.of(
        gain != null ? gain.getId() : null, loss != null ? loss.getId() : null);
  }

  /**
   * 환차손익 분개용 계정 — 환차이익·환차손이 <b>모두</b> 설정된 경우에만 반환(내부, 권한 검사 없음). 결제 자동 분개에서 호출하므로 FINANCE_READ를
   * 요구하지 않는다(currentBaseCurrencyCode와 동일 정책).
   */
  public Optional<FxGainLossAccounts> currentFxAccounts() {
    return repository
        .findFirstByOrderByIdAsc()
        .filter(e -> e.getFxGainAccount() != null && e.getFxLossAccount() != null)
        .map(e -> new FxGainLossAccounts(e.getFxGainAccount(), e.getFxLossAccount()));
  }

  /** 부가세 분개용 통제계정 — 부가세대급금(매입)·부가세예수금(매출), 각각 nullable(둘은 독립). */
  public record VatAccounts(Account receivableAccount, Account payableAccount) {}

  /** 부가세 통제계정 설정 조회(FINANCE_READ). 미설정 항목은 null. */
  public VatAccountResponse getVatAccounts() {
    permissionChecker.require(Permission.FINANCE_READ);
    return repository
        .findFirstByOrderByIdAsc()
        .map(
            e ->
                VatAccountResponse.of(
                    e.getVatReceivableAccount() != null
                        ? e.getVatReceivableAccount().getId()
                        : null,
                    e.getVatPayableAccount() != null ? e.getVatPayableAccount().getId() : null))
        .orElseGet(() -> VatAccountResponse.of(null, null));
  }

  /**
   * 부가세 통제계정 설정 변경(FINANCE_SETTING_WRITE). 부가세대급금·예수금을 함께 지정·해제한다. 설정 행이 없으면 현재 기준통화로 행을 만들어 계정만
   * 채운다.
   */
  @Transactional
  public VatAccountResponse updateVatAccounts(VatAccountUpdateRequest request) {
    permissionChecker.require(Permission.FINANCE_SETTING_WRITE);
    Account receivable = resolveAccount(request.vatReceivableAccountId());
    Account payable = resolveAccount(request.vatPayableAccountId());
    TenantBaseCurrency entity =
        repository
            .findFirstByOrderByIdAsc()
            .orElseGet(() -> repository.save(TenantBaseCurrency.of(currentBaseCurrencyCode())));
    entity.assignVatAccounts(receivable, payable);
    return VatAccountResponse.of(
        receivable != null ? receivable.getId() : null, payable != null ? payable.getId() : null);
  }

  /** 부가세 분개용 통제계정 — 대급금·예수금 각각 nullable(내부, 권한 검사 없음). 전기 자동분개에서 호출. */
  public VatAccounts currentVatAccounts() {
    return repository
        .findFirstByOrderByIdAsc()
        .map(e -> new VatAccounts(e.getVatReceivableAccount(), e.getVatPayableAccount()))
        .orElseGet(() -> new VatAccounts(null, null));
  }

  /** 감가상각·처분 분개용 계정 — 감가상각비·감가상각누계액·처분이익·처분손실(각 nullable). */
  public record DepreciationAccounts(
      Account expenseAccount,
      Account accumulatedAccount,
      Account disposalGainAccount,
      Account disposalLossAccount) {}

  /** 감가상각·처분 계정 설정 조회(FINANCE_READ). 미설정 항목은 null. */
  public DepreciationAccountResponse getDepreciationAccounts() {
    permissionChecker.require(Permission.FINANCE_READ);
    return repository
        .findFirstByOrderByIdAsc()
        .map(
            e ->
                DepreciationAccountResponse.of(
                    accountId(e.getDepreciationExpenseAccount()),
                    accountId(e.getAccumulatedDepreciationAccount()),
                    accountId(e.getDisposalGainAccount()),
                    accountId(e.getDisposalLossAccount())))
        .orElseGet(() -> DepreciationAccountResponse.of(null, null, null, null));
  }

  /** 감가상각·처분 계정 설정 변경(FINANCE_SETTING_WRITE). 행이 없으면 현재 기준통화로 생성 후 계정만 채운다. */
  @Transactional
  public DepreciationAccountResponse updateDepreciationAccounts(
      DepreciationAccountUpdateRequest request) {
    permissionChecker.require(Permission.FINANCE_SETTING_WRITE);
    Account expense = resolveAccount(request.depreciationExpenseAccountId());
    Account accumulated = resolveAccount(request.accumulatedDepreciationAccountId());
    Account gain = resolveAccount(request.disposalGainAccountId());
    Account loss = resolveAccount(request.disposalLossAccountId());
    TenantBaseCurrency entity =
        repository
            .findFirstByOrderByIdAsc()
            .orElseGet(() -> repository.save(TenantBaseCurrency.of(currentBaseCurrencyCode())));
    entity.assignDepreciationAccounts(expense, accumulated, gain, loss);
    return DepreciationAccountResponse.of(
        accountId(expense), accountId(accumulated), accountId(gain), accountId(loss));
  }

  /** 감가상각·처분 분개용 계정(내부, 권한 검사 없음). 상각/처분 자동분개에서 호출. */
  public DepreciationAccounts currentDepreciationAccounts() {
    return repository
        .findFirstByOrderByIdAsc()
        .map(
            e ->
                new DepreciationAccounts(
                    e.getDepreciationExpenseAccount(),
                    e.getAccumulatedDepreciationAccount(),
                    e.getDisposalGainAccount(),
                    e.getDisposalLossAccount()))
        .orElseGet(() -> new DepreciationAccounts(null, null, null, null));
  }

  /** 손상차손 분개용 계정 — 손상차손비·손상차손누계액(각 nullable). */
  public record ImpairmentAccounts(Account lossAccount, Account accumulatedAccount) {}

  /** 손상차손 계정 설정 조회(FINANCE_READ). 미설정 항목은 null. */
  public ImpairmentAccountResponse getImpairmentAccounts() {
    permissionChecker.require(Permission.FINANCE_READ);
    return repository
        .findFirstByOrderByIdAsc()
        .map(
            e ->
                ImpairmentAccountResponse.of(
                    accountId(e.getImpairmentLossAccount()),
                    accountId(e.getAccumulatedImpairmentAccount())))
        .orElseGet(() -> ImpairmentAccountResponse.of(null, null));
  }

  /** 손상차손 계정 설정 변경(FINANCE_SETTING_WRITE). 행이 없으면 현재 기준통화로 생성 후 계정만 채운다. */
  @Transactional
  public ImpairmentAccountResponse updateImpairmentAccounts(
      ImpairmentAccountUpdateRequest request) {
    permissionChecker.require(Permission.FINANCE_SETTING_WRITE);
    Account loss = resolveAccount(request.impairmentLossAccountId());
    Account accumulated = resolveAccount(request.accumulatedImpairmentAccountId());
    TenantBaseCurrency entity =
        repository
            .findFirstByOrderByIdAsc()
            .orElseGet(() -> repository.save(TenantBaseCurrency.of(currentBaseCurrencyCode())));
    entity.assignImpairmentAccounts(loss, accumulated);
    return ImpairmentAccountResponse.of(accountId(loss), accountId(accumulated));
  }

  /** 손상차손 분개용 계정(내부, 권한 검사 없음). 손상 자동분개에서 호출. */
  public ImpairmentAccounts currentImpairmentAccounts() {
    return repository
        .findFirstByOrderByIdAsc()
        .map(
            e ->
                new ImpairmentAccounts(
                    e.getImpairmentLossAccount(), e.getAccumulatedImpairmentAccount()))
        .orElseGet(() -> new ImpairmentAccounts(null, null));
  }

  private static Long accountId(Account a) {
    return a != null ? a.getId() : null;
  }

  private Account resolveAccount(Long accountId) {
    if (accountId == null) {
      return null;
    }
    return accountRepository
        .findById(accountId)
        .orElseThrow(() -> new ErpException(ErrorCode.ACCOUNT_NOT_FOUND));
  }

  /** finance 거래에 base_amount 스냅샷이 하나라도 있으면 true(현재 테넌트만 @TenantId 자동 필터). */
  private boolean hasBaseAmountSnapshot() {
    return apInvoiceRepository.existsByBaseAmountIsNotNull()
        || arInvoiceRepository.existsByBaseAmountIsNotNull()
        || journalEntryRepository.existsByBaseAmountIsNotNull();
  }
}
