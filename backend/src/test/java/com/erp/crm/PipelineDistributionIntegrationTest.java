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
    void pipelineDistribution_separatesAmountsByCurrencyAndPreservesEmptyStages() {
        // Stage A: order=1, KRW 2건(100+200) + USD 1건(500) — 혼합통화
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
        opportunityRepository.save(Opportunity.of(account, "Opp 3", stageA,
                BigDecimal.valueOf(500), "USD", LocalDate.of(2026, 12, 31), 10, "user1", null, null));

        List<PipelineDistributionResponse> result = crmAnalyticsService.getPipelineDistribution();

        assertThat(result).hasSize(2);

        PipelineDistributionResponse rowA = result.stream()
                .filter(r -> r.stageId().equals(stageA.getId()))
                .findFirst()
                .orElseThrow();
        // count는 통화 합산(KRW 2 + USD 1), amounts는 통화별로 분리
        assertThat(rowA.count()).isEqualTo(3L);
        assertThat(rowA.amounts()).hasSize(2);
        assertThat(rowA.amounts().get(0).currency()).isEqualTo("KRW");
        assertThat(rowA.amounts().get(0).amount()).isEqualByComparingTo(BigDecimal.valueOf(300));
        assertThat(rowA.amounts().get(1).currency()).isEqualTo("USD");
        assertThat(rowA.amounts().get(1).amount()).isEqualByComparingTo(BigDecimal.valueOf(500));

        // 빈 단계는 count=0, amounts 빈 리스트로 보존
        PipelineDistributionResponse rowB = result.stream()
                .filter(r -> r.stageId().equals(stageB.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(rowB.count()).isEqualTo(0L);
        assertThat(rowB.amounts()).isEmpty();
    }
}
