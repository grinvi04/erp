package com.erp.crm.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.erp.common.currency.CurrencyConversionPort;
import com.erp.common.currency.CurrencyConversionPort.Conversion;
import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.crm.application.dto.OpportunityCreateRequest;
import com.erp.crm.application.dto.OpportunityResponse;
import com.erp.crm.domain.model.Account;
import com.erp.crm.domain.model.AccountType;
import com.erp.crm.domain.model.Opportunity;
import com.erp.crm.domain.model.PipelineStage;
import com.erp.crm.domain.repository.OpportunityRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OpportunityServiceTest {

  @Mock private OpportunityRepository opportunityRepository;
  @Mock private CrmAccountService accountService;
  @Mock private PipelineStageService stageService;
  @Mock private com.erp.common.security.PermissionChecker permissionChecker;
  @Mock private com.erp.common.security.CurrentUserProvider currentUserProvider;
  @Mock private com.erp.crm.application.service.CrmDataScopeResolver dataScopeResolver;
  @Mock private CurrencyConversionPort currencyConversionPort;
  @InjectMocks private OpportunityService opportunityService;

  private Account buildAccount() {
    return Account.of(
        "ACC-001",
        "테스트고객사",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        AccountType.CUSTOMER,
        "user-001");
  }

  private PipelineStage buildStage() {
    return PipelineStage.of("탐색", 1, 20, false, false);
  }

  @Test
  void create_validRequest_savesWithCurrentUserAsOwner() {
    Account account = buildAccount();
    PipelineStage stage = buildStage();

    given(accountService.getOrThrow(1L)).willReturn(account);
    given(stageService.getOrThrow(1L)).willReturn(stage);
    given(currentUserProvider.getCurrentUserId()).willReturn("auth-user-sub");
    given(opportunityRepository.save(any(Opportunity.class))).willAnswer(inv -> inv.getArgument(0));
    given(currencyConversionPort.tryConvert(any(), any(), any())).willReturn(Optional.empty());

    OpportunityCreateRequest req =
        new OpportunityCreateRequest(
            1L,
            "2026 클라우드 전환",
            1L,
            new BigDecimal("50000000"),
            "KRW",
            LocalDate.of(2026, 12, 31),
            20,
            "REFERRAL",
            "상세설명");

    OpportunityResponse result = opportunityService.create(req);

    assertThat(result.name()).isEqualTo("2026 클라우드 전환");
    assertThat(result.probability()).isEqualTo(20);
    assertThat(result.ownerId()).isEqualTo("auth-user-sub");
  }

  @Test
  void create_foreignCurrencyWithRate_storesBaseSnapshot() {
    // AC-8: 생성 시 환율로 amount를 기준통화 환산해 base_amount·exchange_rate 저장 (USD 1000 × 1300).
    given(accountService.getOrThrow(1L)).willReturn(buildAccount());
    given(stageService.getOrThrow(1L)).willReturn(buildStage());
    given(currentUserProvider.getCurrentUserId()).willReturn("auth-user-sub");
    given(currencyConversionPort.tryConvert(any(), any(), any()))
        .willReturn(
            Optional.of(
                new Conversion(new BigDecimal("1300000.00"), new BigDecimal("1300.00000000"))));
    ArgumentCaptor<Opportunity> captor = ArgumentCaptor.forClass(Opportunity.class);
    given(opportunityRepository.save(captor.capture())).willAnswer(inv -> inv.getArgument(0));

    opportunityService.create(
        new OpportunityCreateRequest(
            1L,
            "USD 딜",
            1L,
            new BigDecimal("1000"),
            "USD",
            LocalDate.of(2026, 12, 31),
            20,
            "REFERRAL",
            null));

    Opportunity saved = captor.getValue();
    assertThat(saved.getBaseAmount()).isEqualByComparingTo("1300000.00");
    assertThat(saved.getExchangeRate()).isEqualByComparingTo("1300.00000000");
  }

  @Test
  void create_noRate_leavesBaseSnapshotNull() {
    // AC-11: 환율 부재 통화는 정상 생성하되 base_amount·exchange_rate를 null(미산정)로 남긴다(거부 안 함).
    given(accountService.getOrThrow(1L)).willReturn(buildAccount());
    given(stageService.getOrThrow(1L)).willReturn(buildStage());
    given(currentUserProvider.getCurrentUserId()).willReturn("auth-user-sub");
    given(currencyConversionPort.tryConvert(any(), any(), any())).willReturn(Optional.empty());
    ArgumentCaptor<Opportunity> captor = ArgumentCaptor.forClass(Opportunity.class);
    given(opportunityRepository.save(captor.capture())).willAnswer(inv -> inv.getArgument(0));

    opportunityService.create(
        new OpportunityCreateRequest(
            1L,
            "JPY 딜",
            1L,
            new BigDecimal("5000"),
            "JPY",
            LocalDate.of(2026, 12, 31),
            20,
            "REFERRAL",
            null));

    Opportunity saved = captor.getValue();
    assertThat(saved.getBaseAmount()).isNull();
    assertThat(saved.getExchangeRate()).isNull();
  }

  @Test
  void update_doesNotRecalculateBaseSnapshot() {
    // AC-9: 스냅샷 불변 — 생성 시 환산값을 고정한다. 이후 거래 수정(update)은 base_amount를
    //       재계산하지 않으므로, 런타임 환율이 변경돼도 기존 거래의 기준통화 환산액은 불변이다.
    Opportunity opp =
        Opportunity.of(
            buildAccount(),
            "USD 딜",
            buildStage(),
            new BigDecimal("1000"),
            "USD",
            LocalDate.of(2026, 12, 31),
            20,
            "owner",
            null,
            null);
    opp.applyBaseSnapshot(new BigDecimal("1300000.00"), new BigDecimal("1300.00000000"));

    opp.update(
        "USD 딜 수정",
        buildStage(),
        new BigDecimal("2000"),
        "USD",
        LocalDate.of(2026, 12, 31),
        30,
        "owner",
        null,
        null);

    assertThat(opp.getBaseAmount()).isEqualByComparingTo("1300000.00");
    assertThat(opp.getExchangeRate()).isEqualByComparingTo("1300.00000000");
  }

  @Test
  void findById_notFound_throwsOpportunityNotFound() {
    given(opportunityRepository.findById(99L)).willReturn(Optional.empty());

    ErpException ex = assertThrows(ErpException.class, () -> opportunityService.findById(99L));
    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.OPPORTUNITY_NOT_FOUND);
  }
}
