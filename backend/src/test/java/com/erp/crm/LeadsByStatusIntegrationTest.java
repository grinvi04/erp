package com.erp.crm;

import com.erp.common.AbstractIntegrationTest;
import com.erp.crm.application.dto.LeadStatusCountResponse;
import com.erp.crm.application.service.CrmAnalyticsService;
import com.erp.crm.domain.model.Lead;
import com.erp.crm.domain.model.LeadStatus;
import com.erp.crm.domain.repository.LeadRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class LeadsByStatusIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private LeadRepository leadRepository;
    @Autowired
    private CrmAnalyticsService crmAnalyticsService;

    @BeforeEach
    void authenticateUser() {
        authenticate("test-user", "crm:read", "crm:write");
    }

    @Test
    void leadsByStatus_fillsAllStatusValuesWithZeroForMissing() {
        // NEW x2, QUALIFIED x1 — all other statuses absent
        leadRepository.save(Lead.of("Kim", "A", "Corp", null, null, null, null, "user1", null));
        leadRepository.save(Lead.of("Lee", "B", "Corp", null, null, null, null, "user1", null));
        Lead qualified = Lead.of("Park", "C", "Corp", null, null, null, null, "user1", null);
        qualified.qualify();
        leadRepository.save(qualified);

        List<LeadStatusCountResponse> result = crmAnalyticsService.getLeadsByStatus();

        // All LeadStatus values should be present
        assertThat(result).hasSize(LeadStatus.values().length);

        Map<LeadStatus, Long> countMap = result.stream()
                .collect(Collectors.toMap(LeadStatusCountResponse::status, LeadStatusCountResponse::count));

        assertThat(countMap.get(LeadStatus.NEW)).isEqualTo(2L);
        assertThat(countMap.get(LeadStatus.QUALIFIED)).isEqualTo(1L);
        assertThat(countMap.get(LeadStatus.CONTACTED)).isEqualTo(0L);
        assertThat(countMap.get(LeadStatus.CONVERTED)).isEqualTo(0L);
        assertThat(countMap.get(LeadStatus.DISQUALIFIED)).isEqualTo(0L);
    }
}
