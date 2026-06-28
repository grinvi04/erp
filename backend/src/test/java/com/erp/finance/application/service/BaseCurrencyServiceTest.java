package com.erp.finance.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.security.Permission;
import com.erp.common.security.PermissionChecker;
import com.erp.finance.application.dto.BaseCurrencyResponse;
import com.erp.finance.application.dto.BaseCurrencyUpdateRequest;
import com.erp.finance.application.dto.FxGainLossAccountResponse;
import com.erp.finance.application.dto.FxGainLossAccountUpdateRequest;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BaseCurrencyServiceTest {

  @Mock private TenantBaseCurrencyRepository repository;
  @Mock private ApInvoiceRepository apInvoiceRepository;
  @Mock private ArInvoiceRepository arInvoiceRepository;
  @Mock private JournalEntryRepository journalEntryRepository;
  @Mock private AccountRepository accountRepository;
  @Mock private PermissionChecker permissionChecker;

  @InjectMocks private BaseCurrencyService baseCurrencyService;

  @Test
  void getBaseCurrency_noSetting_returnsKrwDefault() {
    // AC-1: 미설정 시 KRW 기본 반환.
    given(repository.findFirstByOrderByIdAsc()).willReturn(Optional.empty());

    BaseCurrencyResponse result = baseCurrencyService.getBaseCurrency();

    assertThat(result.baseCurrency()).isEqualTo("KRW");
  }

  @Test
  void getBaseCurrency_existing_returnsStoredCurrency() {
    given(repository.findFirstByOrderByIdAsc())
        .willReturn(Optional.of(TenantBaseCurrency.of("USD")));

    BaseCurrencyResponse result = baseCurrencyService.getBaseCurrency();

    assertThat(result.baseCurrency()).isEqualTo("USD");
  }

  @Test
  void updateBaseCurrency_firstTime_createsRow() {
    // AC-1: 변경 — 미설정이면 새로 저장.
    given(repository.findFirstByOrderByIdAsc()).willReturn(Optional.empty());
    given(repository.save(any())).willReturn(TenantBaseCurrency.of("USD"));

    BaseCurrencyResponse result =
        baseCurrencyService.updateBaseCurrency(new BaseCurrencyUpdateRequest("USD"));

    assertThat(result.baseCurrency()).isEqualTo("USD");
  }

  @Test
  void updateBaseCurrency_snapshotExists_throwsNotAllowed() {
    // 가드: 다른 통화로 변경 시 base_amount 스냅샷이 있으면 거부(409). 기존 설정 불변(save 미호출).
    given(repository.findFirstByOrderByIdAsc())
        .willReturn(Optional.of(TenantBaseCurrency.of("KRW")));
    given(apInvoiceRepository.existsByBaseAmountIsNotNull()).willReturn(true);

    ErpException ex =
        assertThrows(
            ErpException.class,
            () -> baseCurrencyService.updateBaseCurrency(new BaseCurrencyUpdateRequest("USD")));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.BASE_CURRENCY_CHANGE_NOT_ALLOWED);
  }

  @Test
  void updateBaseCurrency_noSnapshot_changeAllowed() {
    // 가드 미발동: 스냅샷이 전혀 없으면(아직 FX 거래 없음) 다른 통화로 변경 허용.
    given(repository.findFirstByOrderByIdAsc())
        .willReturn(Optional.of(TenantBaseCurrency.of("USD")));

    BaseCurrencyResponse result =
        baseCurrencyService.updateBaseCurrency(new BaseCurrencyUpdateRequest("EUR"));

    assertThat(result.baseCurrency()).isEqualTo("EUR");
  }

  @Test
  void updateBaseCurrency_sameValue_noOpAllowedEvenWithSnapshot() {
    // 동일 값 PUT(no-op)은 스냅샷이 있어도 허용 — 통화가 실제로 바뀌지 않으므로 가드 미발동.
    given(repository.findFirstByOrderByIdAsc())
        .willReturn(Optional.of(TenantBaseCurrency.of("USD")));

    BaseCurrencyResponse result =
        baseCurrencyService.updateBaseCurrency(new BaseCurrencyUpdateRequest("USD"));

    assertThat(result.baseCurrency()).isEqualTo("USD");
  }

  @Test
  void updateBaseCurrency_withoutPermission_throwsForbidden() {
    // AC-7: FINANCE_SETTING_WRITE 없으면 변경 불가.
    doThrow(new ErpException(ErrorCode.FORBIDDEN))
        .when(permissionChecker)
        .require(Permission.FINANCE_SETTING_WRITE);

    ErpException ex =
        assertThrows(
            ErpException.class,
            () -> baseCurrencyService.updateBaseCurrency(new BaseCurrencyUpdateRequest("USD")));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
  }

  @Test
  void getFxGainLossAccounts_existing_returnsAccountIds() {
    // AC-1 조회: 설정된 환차이익·환차손 계정 ID 반환.
    Account gain = mock(Account.class);
    Account loss = mock(Account.class);
    given(gain.getId()).willReturn(10L);
    given(loss.getId()).willReturn(20L);
    TenantBaseCurrency entity = TenantBaseCurrency.of("USD");
    entity.assignFxAccounts(gain, loss);
    given(repository.findFirstByOrderByIdAsc()).willReturn(Optional.of(entity));

    FxGainLossAccountResponse result = baseCurrencyService.getFxGainLossAccounts();

    assertThat(result.fxGainAccountId()).isEqualTo(10L);
    assertThat(result.fxLossAccountId()).isEqualTo(20L);
  }

  @Test
  void getFxGainLossAccounts_noSetting_returnsNulls() {
    given(repository.findFirstByOrderByIdAsc()).willReturn(Optional.empty());

    FxGainLossAccountResponse result = baseCurrencyService.getFxGainLossAccounts();

    assertThat(result.fxGainAccountId()).isNull();
    assertThat(result.fxLossAccountId()).isNull();
  }

  @Test
  void updateFxGainLossAccounts_setsBothAccounts() {
    // AC-1 변경: 환차이익·환차손 계정 지정 → 설정 저장·응답.
    Account gain = mock(Account.class);
    Account loss = mock(Account.class);
    given(gain.getId()).willReturn(10L);
    given(loss.getId()).willReturn(20L);
    given(accountRepository.findById(10L)).willReturn(Optional.of(gain));
    given(accountRepository.findById(20L)).willReturn(Optional.of(loss));
    TenantBaseCurrency entity = TenantBaseCurrency.of("USD");
    given(repository.findFirstByOrderByIdAsc()).willReturn(Optional.of(entity));

    FxGainLossAccountResponse result =
        baseCurrencyService.updateFxGainLossAccounts(new FxGainLossAccountUpdateRequest(10L, 20L));

    assertThat(result.fxGainAccountId()).isEqualTo(10L);
    assertThat(result.fxLossAccountId()).isEqualTo(20L);
    assertThat(entity.getFxGainAccount()).isSameAs(gain);
    assertThat(entity.getFxLossAccount()).isSameAs(loss);
  }

  @Test
  void updateFxGainLossAccounts_withoutPermission_throwsForbidden() {
    // AC-10: FINANCE_SETTING_WRITE 없으면 403.
    doThrow(new ErpException(ErrorCode.FORBIDDEN))
        .when(permissionChecker)
        .require(Permission.FINANCE_SETTING_WRITE);

    ErpException ex =
        assertThrows(
            ErpException.class,
            () ->
                baseCurrencyService.updateFxGainLossAccounts(
                    new FxGainLossAccountUpdateRequest(10L, 20L)));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
  }

  @Test
  void getVatAccounts_existing_returnsAccountIds() {
    Account receivable = mock(Account.class);
    Account payable = mock(Account.class);
    given(receivable.getId()).willReturn(11L);
    given(payable.getId()).willReturn(21L);
    TenantBaseCurrency entity = TenantBaseCurrency.of("KRW");
    entity.assignVatAccounts(receivable, payable);
    given(repository.findFirstByOrderByIdAsc()).willReturn(Optional.of(entity));

    VatAccountResponse result = baseCurrencyService.getVatAccounts();

    assertThat(result.vatReceivableAccountId()).isEqualTo(11L);
    assertThat(result.vatPayableAccountId()).isEqualTo(21L);
  }

  @Test
  void getVatAccounts_noSetting_returnsNulls() {
    given(repository.findFirstByOrderByIdAsc()).willReturn(Optional.empty());

    VatAccountResponse result = baseCurrencyService.getVatAccounts();

    assertThat(result.vatReceivableAccountId()).isNull();
    assertThat(result.vatPayableAccountId()).isNull();
  }

  @Test
  void updateVatAccounts_setsBothAccounts() {
    Account receivable = mock(Account.class);
    Account payable = mock(Account.class);
    given(receivable.getId()).willReturn(11L);
    given(payable.getId()).willReturn(21L);
    given(accountRepository.findById(11L)).willReturn(Optional.of(receivable));
    given(accountRepository.findById(21L)).willReturn(Optional.of(payable));
    TenantBaseCurrency entity = TenantBaseCurrency.of("KRW");
    given(repository.findFirstByOrderByIdAsc()).willReturn(Optional.of(entity));

    VatAccountResponse result =
        baseCurrencyService.updateVatAccounts(new VatAccountUpdateRequest(11L, 21L));

    assertThat(result.vatReceivableAccountId()).isEqualTo(11L);
    assertThat(result.vatPayableAccountId()).isEqualTo(21L);
    assertThat(entity.getVatReceivableAccount()).isSameAs(receivable);
    assertThat(entity.getVatPayableAccount()).isSameAs(payable);
  }

  @Test
  void updateVatAccounts_withoutPermission_throwsForbidden() {
    // AC-10: FINANCE_SETTING_WRITE 없으면 403.
    doThrow(new ErpException(ErrorCode.FORBIDDEN))
        .when(permissionChecker)
        .require(Permission.FINANCE_SETTING_WRITE);

    ErpException ex =
        assertThrows(
            ErpException.class,
            () -> baseCurrencyService.updateVatAccounts(new VatAccountUpdateRequest(11L, 21L)));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
  }
}
