package com.erp.crm.application.service;

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
            Long stageId, String name, int order, long count, String amount) {
        PipelineDistributionRow row = org.mockito.Mockito.mock(PipelineDistributionRow.class);
        given(row.getStageId()).willReturn(stageId);
        given(row.getStageName()).willReturn(name);
        given(row.getStageOrder()).willReturn(order);
        given(row.getCount()).willReturn(count);
        given(row.getTotalAmount()).willReturn(new BigDecimal(amount));
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
                pipelineRow(10L, "리드", 1, 4L, "1000000.00"),
                pipelineRow(20L, "제안", 2, 2L, "5000000.00"));
        givenAllScope();
        given(pipelineStageRepository.pipelineDistribution(anyBoolean(), anyCollection()))
                .willReturn(rows);

        List<PipelineDistributionResponse> result = crmAnalyticsService.getPipelineDistribution();

        assertThat(result).hasSize(2);
        assertThat(result.get(0)).isEqualTo(
                new PipelineDistributionResponse(10L, "리드", 1, 4L, new BigDecimal("1000000.00")));
        assertThat(result.get(1)).isEqualTo(
                new PipelineDistributionResponse(20L, "제안", 2, 2L, new BigDecimal("5000000.00")));
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
