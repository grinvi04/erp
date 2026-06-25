package com.erp.crm.application.service;

import com.erp.crm.application.dto.CrmSummaryResponse;
import com.erp.crm.domain.model.ActivityStatus;
import com.erp.crm.domain.model.LeadStatus;
import com.erp.crm.domain.repository.ActivityRepository;
import com.erp.crm.domain.repository.LeadRepository;
import com.erp.crm.domain.repository.OpportunityRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CrmSummaryServiceTest {

    @Mock private OpportunityRepository opportunityRepository;
    @Mock private LeadRepository leadRepository;
    @Mock private ActivityRepository activityRepository;
    @Mock private com.erp.common.security.PermissionChecker permissionChecker;
    @InjectMocks private CrmSummaryService crmSummaryService;

    @Test
    void getSummary_aggregatesPipelineLeadAndActivityCounts() {
        given(opportunityRepository.countOpen()).willReturn(11L);
        given(opportunityRepository.sumOpenAmount()).willReturn(new BigDecimal("9800000.00"));
        given(leadRepository.countByStatus(LeadStatus.NEW)).willReturn(6L);
        given(activityRepository.countByStatus(ActivityStatus.OPEN)).willReturn(15L);

        CrmSummaryResponse result = crmSummaryService.getSummary();

        assertThat(result.openOpportunities()).isEqualTo(11L);
        assertThat(result.openOpportunityAmount()).isEqualByComparingTo("9800000.00");
        assertThat(result.newLeads()).isEqualTo(6L);
        assertThat(result.openActivities()).isEqualTo(15L);
    }

    @Test
    void getSummary_nullOpenAmount_defaultsToZero() {
        given(opportunityRepository.countOpen()).willReturn(0L);
        given(opportunityRepository.sumOpenAmount()).willReturn(null);
        given(leadRepository.countByStatus(LeadStatus.NEW)).willReturn(0L);
        given(activityRepository.countByStatus(ActivityStatus.OPEN)).willReturn(0L);

        CrmSummaryResponse result = crmSummaryService.getSummary();

        assertThat(result.openOpportunityAmount()).isEqualByComparingTo("0");
    }
}
