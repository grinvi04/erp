package com.erp.crm.application.service;

import com.erp.common.response.CurrencyAmount;
import com.erp.common.security.Permission;
import com.erp.common.security.PermissionChecker;
import com.erp.crm.application.dto.LeadStatusCountResponse;
import com.erp.crm.application.dto.PipelineDistributionResponse;
import com.erp.crm.domain.model.LeadStatus;
import com.erp.crm.domain.repository.LeadRepository;
import com.erp.crm.domain.repository.LeadStatusCountRow;
import com.erp.crm.domain.repository.PipelineDistributionRow;
import com.erp.crm.domain.repository.PipelineStageRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CrmAnalyticsServiceTest {

    @Mock private PipelineStageRepository pipelineStageRepository;
    @Mock private LeadRepository leadRepository;
    @Mock private CrmDataScopeResolver dataScopeResolver;
    @Mock private PermissionChecker permissionChecker;
    @InjectMocks private CrmAnalyticsService crmAnalyticsService;

    private void givenAllScope() {
        given(dataScopeResolver.ownerScope())
                .willReturn(new CrmDataScopeResolver.OwnerScope(false, Set.of()));
    }

    private PipelineDistributionRow pipelineRow(
            Long stageId, String name, int order, String currency, long count, String amount) {
        PipelineDistributionRow row = org.mockito.Mockito.mock(PipelineDistributionRow.class);
        // 단계 식별 컬럼은 그룹 첫 행에서만 읽히고, 금액은 currency!=null인 행에서만 읽히므로 lenient.
        lenient().when(row.getStageId()).thenReturn(stageId);
        lenient().when(row.getStageName()).thenReturn(name);
        lenient().when(row.getStageOrder()).thenReturn(order);
        lenient().when(row.getCurrency()).thenReturn(currency);
        lenient().when(row.getCount()).thenReturn(count);
        lenient().when(row.getTotalAmount()).thenReturn(amount == null ? null : new BigDecimal(amount));
        return row;
    }

    private LeadStatusCountRow leadRow(LeadStatus status, long count) {
        LeadStatusCountRow row = org.mockito.Mockito.mock(LeadStatusCountRow.class);
        given(row.getStatus()).willReturn(status);
        given(row.getCount()).willReturn(count);
        return row;
    }

    @Test
    void getPipelineDistribution_mapsRowsToResponsesPreservingOrder() {
        List<PipelineDistributionRow> rows = List.of(
                pipelineRow(10L, "리드", 1, "KRW", 4L, "1000000.00"),
                pipelineRow(20L, "제안", 2, "KRW", 2L, "5000000.00"));
        givenAllScope();
        given(pipelineStageRepository.pipelineDistribution(anyBoolean(), anyCollection()))
                .willReturn(rows);

        List<PipelineDistributionResponse> result = crmAnalyticsService.getPipelineDistribution();

        assertThat(result).hasSize(2);
        assertThat(result.get(0)).isEqualTo(
                new PipelineDistributionResponse(10L, "리드", 1, 4L,
                        List.of(new CurrencyAmount("KRW", new BigDecimal("1000000.00")))));
        assertThat(result.get(1)).isEqualTo(
                new PipelineDistributionResponse(20L, "제안", 2, 2L,
                        List.of(new CurrencyAmount("KRW", new BigDecimal("5000000.00")))));
    }

    @Test
    void getPipelineDistribution_groupsCurrenciesPerStageAndPreservesEmptyStage() {
        // 한 단계(리드)에 KRW·USD 두 통화 행 + 빈 단계(제안)는 currency=null·count=0 단일 행.
        List<PipelineDistributionRow> rows = List.of(
                pipelineRow(10L, "리드", 1, "KRW", 2L, "300.00"),
                pipelineRow(10L, "리드", 1, "USD", 1L, "50.00"),
                pipelineRow(20L, "제안", 2, null, 0L, null));
        givenAllScope();
        given(pipelineStageRepository.pipelineDistribution(anyBoolean(), anyCollection()))
                .willReturn(rows);

        List<PipelineDistributionResponse> result = crmAnalyticsService.getPipelineDistribution();

        assertThat(result).hasSize(2);

        PipelineDistributionResponse lead = result.get(0);
        assertThat(lead.stageId()).isEqualTo(10L);
        assertThat(lead.count()).isEqualTo(3L); // 통화 합산
        assertThat(lead.amounts()).containsExactly(
                new CurrencyAmount("KRW", new BigDecimal("300.00")),
                new CurrencyAmount("USD", new BigDecimal("50.00")));

        PipelineDistributionResponse empty = result.get(1);
        assertThat(empty.stageId()).isEqualTo(20L);
        assertThat(empty.count()).isEqualTo(0L);
        assertThat(empty.amounts()).isEmpty();
    }

    @Test
    void getPipelineDistribution_requiresCrmRead() {
        givenAllScope();
        given(pipelineStageRepository.pipelineDistribution(anyBoolean(), anyCollection()))
                .willReturn(List.of());

        crmAnalyticsService.getPipelineDistribution();

        verify(permissionChecker).require(Permission.CRM_READ);
    }

    @Test
    void getPipelineDistribution_appliesOwnerScope() {
        given(dataScopeResolver.ownerScope())
                .willReturn(new CrmDataScopeResolver.OwnerScope(true, Set.of("user-1")));
        given(pipelineStageRepository.pipelineDistribution(anyBoolean(), anyCollection()))
                .willReturn(List.of());

        crmAnalyticsService.getPipelineDistribution();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<java.util.Collection<String>> ownerIds =
                ArgumentCaptor.forClass(java.util.Collection.class);
        verify(pipelineStageRepository).pipelineDistribution(eq(true), ownerIds.capture());
        assertThat(ownerIds.getValue()).containsExactly("user-1");
    }

    @Test
    void getLeadsByStatus_returnsAllStatusesWithZeroForMissing() {
        List<LeadStatusCountRow> rows = List.of(
                leadRow(LeadStatus.NEW, 5L),
                leadRow(LeadStatus.QUALIFIED, 3L));
        givenAllScope();
        given(leadRepository.countByStatusGrouped(anyBoolean(), anyCollection())).willReturn(rows);

        List<LeadStatusCountResponse> result = crmAnalyticsService.getLeadsByStatus();

        Map<LeadStatus, Long> counts = result.stream()
                .collect(Collectors.toMap(LeadStatusCountResponse::status, LeadStatusCountResponse::count));
        assertThat(counts).containsOnlyKeys(LeadStatus.values());
        assertThat(counts.get(LeadStatus.NEW)).isEqualTo(5L);
        assertThat(counts.get(LeadStatus.QUALIFIED)).isEqualTo(3L);
        assertThat(counts.get(LeadStatus.CONTACTED)).isEqualTo(0L);
        assertThat(counts.get(LeadStatus.CONVERTED)).isEqualTo(0L);
        assertThat(counts.get(LeadStatus.DISQUALIFIED)).isEqualTo(0L);
    }

    @Test
    void getLeadsByStatus_emptyRepository_allZero() {
        givenAllScope();
        given(leadRepository.countByStatusGrouped(anyBoolean(), anyCollection())).willReturn(List.of());

        List<LeadStatusCountResponse> result = crmAnalyticsService.getLeadsByStatus();

        assertThat(result).hasSize(LeadStatus.values().length);
        assertThat(result).allMatch(r -> r.count() == 0L);
    }

    @Test
    void getLeadsByStatus_requiresCrmRead() {
        givenAllScope();
        given(leadRepository.countByStatusGrouped(anyBoolean(), anyCollection())).willReturn(List.of());

        crmAnalyticsService.getLeadsByStatus();

        verify(permissionChecker).require(Permission.CRM_READ);
    }

    @Test
    void getLeadsByStatus_appliesOwnerScope() {
        given(dataScopeResolver.ownerScope())
                .willReturn(new CrmDataScopeResolver.OwnerScope(true, Set.of("user-1")));
        given(leadRepository.countByStatusGrouped(anyBoolean(), anyCollection())).willReturn(List.of());

        crmAnalyticsService.getLeadsByStatus();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<java.util.Collection<String>> ownerIds =
                ArgumentCaptor.forClass(java.util.Collection.class);
        verify(leadRepository).countByStatusGrouped(eq(true), ownerIds.capture());
        assertThat(ownerIds.getValue()).containsExactly("user-1");
    }
}
