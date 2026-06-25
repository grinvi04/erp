package com.erp.crm;

import com.erp.common.AbstractIntegrationTest;
import com.erp.crm.application.dto.PipelineDistributionResponse;
import com.erp.crm.application.service.CrmAnalyticsService;
import com.erp.crm.domain.model.Account;
import com.erp.crm.domain.model.AccountType;
import com.erp.crm.domain.model.Opportunity;
import com.erp.crm.domain.model.PipelineStage;
import com.erp.crm.domain.repository.CrmAccountRepository;
import com.erp.crm.domain.repository.OpportunityRepository;
import com.erp.crm.domain.repository.PipelineStageRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class PipelineDistributionIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private PipelineStageRepository pipelineStageRepository;

    @BeforeEach
    void authenticate() {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject("test-user").claim("sub", "test-user").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt,
            List.of(new SimpleGrantedAuthority("crm:read"), new SimpleGrantedAuthority("crm:write"))));
    }

    @AfterEach
    void clearAuth() { SecurityContextHolder.clearContext(); }
    @Autowired
    private OpportunityRepository opportunityRepository;
    @Autowired
    private CrmAccountRepository crmAccountRepository;
    @Autowired
    private CrmAnalyticsService crmAnalyticsService;

    @Test
    void pipelineDistribution_includesZeroOpportunityStages() {
        // Stage A: order=1, 2 opportunities with amounts 100 and 200
        PipelineStage stageA = pipelineStageRepository.save(
                PipelineStage.of("Prospecting", 1, 10, false, false));
        // Stage B: order=2, zero opportunities
        PipelineStage stageB = pipelineStageRepository.save(
                PipelineStage.of("Closed Won", 2, 100, true, false));

        Account account = crmAccountRepository.save(Account.of(
                "ACC-001", "Test Corp", null, null, null, null, null,
                null, null, AccountType.CUSTOMER, "user1"));

        opportunityRepository.save(Opportunity.of(account, "Opp 1", stageA,
                BigDecimal.valueOf(100), "KRW", LocalDate.of(2026, 12, 31), 10, "user1", null, null));
        opportunityRepository.save(Opportunity.of(account, "Opp 2", stageA,
                BigDecimal.valueOf(200), "KRW", LocalDate.of(2026, 12, 31), 10, "user1", null, null));

        List<PipelineDistributionResponse> result = crmAnalyticsService.getPipelineDistribution();

        assertThat(result).hasSize(2);

        PipelineDistributionResponse rowA = result.stream()
                .filter(r -> r.stageId().equals(stageA.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(rowA.count()).isEqualTo(2L);
        assertThat(rowA.totalAmount()).isEqualByComparingTo(BigDecimal.valueOf(300));

        PipelineDistributionResponse rowB = result.stream()
                .filter(r -> r.stageId().equals(stageB.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(rowB.count()).isEqualTo(0L);
        assertThat(rowB.totalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
