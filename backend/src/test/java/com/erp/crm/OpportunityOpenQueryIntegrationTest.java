package com.erp.crm;

import com.erp.common.AbstractIntegrationTest;
import com.erp.crm.domain.model.Account;
import com.erp.crm.domain.model.AccountType;
import com.erp.crm.domain.model.Opportunity;
import com.erp.crm.domain.model.PipelineStage;
import com.erp.crm.domain.repository.CrmAccountRepository;
import com.erp.crm.domain.repository.OpportunityRepository;
import com.erp.crm.domain.repository.PipelineStageRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class OpportunityOpenQueryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private OpportunityRepository opportunityRepository;
    @Autowired
    private CrmAccountRepository accountRepository;
    @Autowired
    private PipelineStageRepository pipelineStageRepository;

    private Account account;
    private PipelineStage openStage;
    private PipelineStage closedWonStage;
    private PipelineStage closedLostStage;

    @BeforeEach
    void setUp() {
        account = accountRepository.save(
                Account.of("ACC-TEST", "Test Account", null, "IT",
                        null, null, null, null, null,
                        AccountType.PROSPECT, "owner@test.com"));

        openStage = pipelineStageRepository.save(
                PipelineStage.of("Negotiation", 3, 60, false, false));

        closedWonStage = pipelineStageRepository.save(
                PipelineStage.of("Closed Won", 5, 100, true, false));

        closedLostStage = pipelineStageRepository.save(
                PipelineStage.of("Closed Lost", 6, 0, false, true));
    }

    private Opportunity opportunity(String name, PipelineStage stage, BigDecimal amount) {
        return opportunityRepository.save(
                Opportunity.of(account, name, stage, amount, "KRW",
                        LocalDate.of(2025, 12, 31), 50, "owner@test.com", null, null));
    }

    @Test
    void countOpen_and_sumOpenAmount_onlyIncludeOpenStageOpportunities() {
        // Open stage: two opportunities with amounts 100 and 200
        opportunity("Opp-Open-100", openStage, BigDecimal.valueOf(100));
        opportunity("Opp-Open-200", openStage, BigDecimal.valueOf(200));

        // Open stage: one opportunity with NULL amount — counted in countOpen but 0 in sum
        opportunity("Opp-Open-Null", openStage, null);

        // Closed-won stage: amount=999 → excluded from both count and sum
        opportunity("Opp-ClosedWon", closedWonStage, BigDecimal.valueOf(999));

        // Closed-lost stage: amount=888 → excluded from both count and sum
        opportunity("Opp-ClosedLost", closedLostStage, BigDecimal.valueOf(888));

        assertThat(opportunityRepository.countOpen(false, java.util.Set.of()))
                .as("3 open-stage opportunities (100, 200, null) should be counted; closed-won/lost excluded")
                .isEqualTo(3L);

        assertThat(opportunityRepository.sumOpenAmount(false, java.util.Set.of()))
                .as("100 + 200 + COALESCE(null, 0) = 300; closed opportunities excluded")
                .isEqualByComparingTo(BigDecimal.valueOf(300));
    }

    @Test
    void countOpen_noOpenOpportunities_returnsZeroForBoth() {
        opportunity("Opp-Won", closedWonStage, BigDecimal.valueOf(500));
        opportunity("Opp-Lost", closedLostStage, BigDecimal.valueOf(250));

        assertThat(opportunityRepository.countOpen(false, java.util.Set.of())).isEqualTo(0L);
        assertThat(opportunityRepository.sumOpenAmount(false, java.util.Set.of()))
                .as("COALESCE(SUM, 0) must return 0 when no open opportunities exist")
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void countOpen_nullAmountContributesZeroToSum() {
        // Single open opportunity with null amount
        opportunity("Opp-NullAmt", openStage, null);

        assertThat(opportunityRepository.countOpen(false, java.util.Set.of()))
                .as("Null-amount opportunity is still an open opportunity")
                .isEqualTo(1L);

        assertThat(opportunityRepository.sumOpenAmount(false, java.util.Set.of()))
                .as("Null amount contributes 0 to sum via COALESCE")
                .isEqualByComparingTo(BigDecimal.ZERO);
    }
}
