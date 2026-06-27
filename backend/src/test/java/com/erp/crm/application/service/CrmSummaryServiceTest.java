package com.erp.crm.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.erp.common.response.CurrencyAmount;
import com.erp.crm.application.dto.CrmSummaryResponse;
import com.erp.crm.domain.model.ActivityStatus;
import com.erp.crm.domain.model.LeadStatus;
import com.erp.crm.domain.repository.ActivityRepository;
import com.erp.crm.domain.repository.LeadRepository;
import com.erp.crm.domain.repository.OpportunityRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CrmSummaryServiceTest {

  @Mock private OpportunityRepository opportunityRepository;
  @Mock private LeadRepository leadRepository;
  @Mock private ActivityRepository activityRepository;
  @Mock private CrmDataScopeResolver dataScopeResolver;
  @Mock private com.erp.common.currency.CurrencyConversionPort currencyConversionPort;
  @Mock private com.erp.common.security.PermissionChecker permissionChecker;
  @InjectMocks private CrmSummaryService crmSummaryService;

  @Test
  void getSummary_aggregatesPipelineLeadAndActivityCounts() {
    given(dataScopeResolver.ownerScope())
        .willReturn(new CrmDataScopeResolver.OwnerScope(false, Set.of()));
    given(opportunityRepository.countOpen(anyBoolean(), anyCollection())).willReturn(11L);
    given(opportunityRepository.sumOpenAmountByCurrency(anyBoolean(), anyCollection()))
        .willReturn(
            List.of(
                new CurrencyAmount("KRW", new BigDecimal("9800000.00")),
                new CurrencyAmount("USD", new BigDecimal("1500.00"))));
    given(leadRepository.countByStatus(eq(LeadStatus.NEW), anyBoolean(), anyCollection()))
        .willReturn(6L);
    given(activityRepository.countByStatus(eq(ActivityStatus.OPEN), anyBoolean(), anyCollection()))
        .willReturn(15L);
    given(currencyConversionPort.baseCurrencyCode()).willReturn("KRW");
    given(opportunityRepository.sumOpenBaseTotal(anyBoolean(), anyCollection()))
        .willReturn(new BigDecimal("11750000.00"));

    CrmSummaryResponse result = crmSummaryService.getSummary();

    assertThat(result.openOpportunities()).isEqualTo(11L);
    // 통화별 분리는 그대로 유지(회귀)
    assertThat(result.openOpportunityAmounts())
        .extracting(CurrencyAmount::currency)
        .containsExactly("KRW", "USD");
    assertThat(result.openOpportunityAmounts().get(0).amount()).isEqualByComparingTo("9800000.00");
    assertThat(result.openOpportunityAmounts().get(1).amount()).isEqualByComparingTo("1500.00");
    assertThat(result.newLeads()).isEqualTo(6L);
    assertThat(result.openActivities()).isEqualTo(15L);
    // 기준통화 합계 추가
    assertThat(result.baseCurrency()).isEqualTo("KRW");
    assertThat(result.openOpportunityBaseTotal()).isEqualByComparingTo("11750000.00");
  }

  @Test
  void getSummary_nullOpenAmount_defaultsToZero() {
    given(dataScopeResolver.ownerScope())
        .willReturn(new CrmDataScopeResolver.OwnerScope(false, Set.of()));
    given(opportunityRepository.countOpen(anyBoolean(), anyCollection())).willReturn(0L);
    given(opportunityRepository.sumOpenAmountByCurrency(anyBoolean(), anyCollection()))
        .willReturn(List.of());
    given(leadRepository.countByStatus(eq(LeadStatus.NEW), anyBoolean(), anyCollection()))
        .willReturn(0L);
    given(activityRepository.countByStatus(eq(ActivityStatus.OPEN), anyBoolean(), anyCollection()))
        .willReturn(0L);
    given(currencyConversionPort.baseCurrencyCode()).willReturn("KRW");
    // 산정된 진행중 기회가 없으면 기준통화 합계는 null
    given(opportunityRepository.sumOpenBaseTotal(anyBoolean(), anyCollection())).willReturn(null);

    CrmSummaryResponse result = crmSummaryService.getSummary();

    assertThat(result.openOpportunityAmounts()).isEmpty();
    assertThat(result.openOpportunityBaseTotal()).isNull();
    assertThat(result.baseCurrency()).isEqualTo("KRW");
  }

  @Test
  void getSummary_appliesOwnerScopeToAggregates() {
    given(dataScopeResolver.ownerScope())
        .willReturn(new CrmDataScopeResolver.OwnerScope(true, Set.of("user-1", "user-2")));
    given(opportunityRepository.countOpen(anyBoolean(), anyCollection())).willReturn(1L);
    given(opportunityRepository.sumOpenAmountByCurrency(anyBoolean(), anyCollection()))
        .willReturn(List.of(new CurrencyAmount("KRW", BigDecimal.TEN)));
    given(leadRepository.countByStatus(eq(LeadStatus.NEW), anyBoolean(), anyCollection()))
        .willReturn(1L);
    given(activityRepository.countByStatus(eq(ActivityStatus.OPEN), anyBoolean(), anyCollection()))
        .willReturn(1L);
    given(currencyConversionPort.baseCurrencyCode()).willReturn("KRW");
    given(opportunityRepository.sumOpenBaseTotal(anyBoolean(), anyCollection()))
        .willReturn(BigDecimal.TEN);

    crmSummaryService.getSummary();

    @SuppressWarnings("unchecked")
    ArgumentCaptor<java.util.Collection<String>> ownerIds =
        ArgumentCaptor.forClass(java.util.Collection.class);
    verify(opportunityRepository).countOpen(eq(true), ownerIds.capture());
    assertThat(ownerIds.getValue()).containsExactlyInAnyOrder("user-1", "user-2");
    verify(opportunityRepository).sumOpenAmountByCurrency(eq(true), anyCollection());
    // 기준통화 합계도 동일 스코프로 집계
    verify(opportunityRepository).sumOpenBaseTotal(eq(true), anyCollection());
    verify(leadRepository).countByStatus(eq(LeadStatus.NEW), eq(true), anyCollection());
    verify(activityRepository).countByStatus(eq(ActivityStatus.OPEN), eq(true), anyCollection());
  }
}
