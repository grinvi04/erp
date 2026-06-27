package com.erp.crm;

import static org.assertj.core.api.Assertions.assertThat;

import com.erp.common.AbstractIntegrationTest;
import com.erp.common.response.CurrencyAmount;
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

@Transactional
class OpportunityOpenQueryIntegrationTest extends AbstractIntegrationTest {

  @Autowired private OpportunityRepository opportunityRepository;
  @Autowired private CrmAccountRepository accountRepository;
  @Autowired private PipelineStageRepository pipelineStageRepository;

  private Account account;
  private PipelineStage openStage;
  private PipelineStage closedWonStage;
  private PipelineStage closedLostStage;

  @BeforeEach
  void setUp() {
    account =
        accountRepository.save(
            Account.of(
                "ACC-TEST",
                "Test Account",
                null,
                "IT",
                null,
                null,
                null,
                null,
                null,
                AccountType.PROSPECT,
                "owner@test.com"));

    openStage = pipelineStageRepository.save(PipelineStage.of("Negotiation", 3, 60, false, false));

    closedWonStage =
        pipelineStageRepository.save(PipelineStage.of("Closed Won", 5, 100, true, false));

    closedLostStage =
        pipelineStageRepository.save(PipelineStage.of("Closed Lost", 6, 0, false, true));
  }

  private Opportunity opportunity(String name, PipelineStage stage, BigDecimal amount) {
    return opportunity(name, stage, amount, "KRW");
  }

  private Opportunity opportunity(
      String name, PipelineStage stage, BigDecimal amount, String currency) {
    return opportunityRepository.save(
        Opportunity.of(
            account,
            name,
            stage,
            amount,
            currency,
            LocalDate.of(2025, 12, 31),
            50,
            "owner@test.com",
            null,
            null));
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
        .as(
            "3 open-stage opportunities (100, 200, null) should be counted; closed-won/lost excluded")
        .isEqualTo(3L);

    var rows = opportunityRepository.sumOpenAmountByCurrency(false, java.util.Set.of());
    assertThat(rows).extracting(CurrencyAmount::currency).containsExactly("KRW");
    assertThat(rows.get(0).amount())
        .as("100 + 200 + COALESCE(null, 0) = 300; closed opportunities excluded")
        .isEqualByComparingTo(BigDecimal.valueOf(300));
  }

  @Test
  void sumOpenAmountByCurrency_splitsOpenOpportunitiesByCurrency() {
    // Open stage, two currencies — must be reported as separate rows, no FX conversion / mixing
    opportunity("Opp-KRW-1", openStage, BigDecimal.valueOf(1000), "KRW");
    opportunity("Opp-KRW-2", openStage, BigDecimal.valueOf(500), "KRW");
    opportunity("Opp-USD-1", openStage, BigDecimal.valueOf(70), "USD");

    // Closed opportunities (any currency) are excluded
    opportunity("Opp-USD-Won", closedWonStage, BigDecimal.valueOf(999), "USD");

    var rows = opportunityRepository.sumOpenAmountByCurrency(false, java.util.Set.of());
    assertThat(rows)
        .as("ORDER BY currency → KRW then USD; KRW=1000+500=1500, USD=70")
        .extracting(CurrencyAmount::currency)
        .containsExactly("KRW", "USD");
    assertThat(rows.get(0).amount()).isEqualByComparingTo(BigDecimal.valueOf(1500));
    assertThat(rows.get(1).amount()).isEqualByComparingTo(BigDecimal.valueOf(70));
  }

  @Test
  void countOpen_noOpenOpportunities_returnsZeroForBoth() {
    opportunity("Opp-Won", closedWonStage, BigDecimal.valueOf(500));
    opportunity("Opp-Lost", closedLostStage, BigDecimal.valueOf(250));

    assertThat(opportunityRepository.countOpen(false, java.util.Set.of())).isEqualTo(0L);
    assertThat(opportunityRepository.sumOpenAmountByCurrency(false, java.util.Set.of()))
        .as("No open opportunities → no currency groups → empty list")
        .isEmpty();
  }

  @Test
  void countOpen_nullAmountContributesZeroToSum() {
    // Single open opportunity with null amount
    opportunity("Opp-NullAmt", openStage, null);

    assertThat(opportunityRepository.countOpen(false, java.util.Set.of()))
        .as("Null-amount opportunity is still an open opportunity")
        .isEqualTo(1L);

    // GROUP BY currency still yields the KRW group; its summed amount is 0 via COALESCE
    var rows = opportunityRepository.sumOpenAmountByCurrency(false, java.util.Set.of());
    assertThat(rows).extracting(CurrencyAmount::currency).containsExactly("KRW");
    assertThat(rows.get(0).amount())
        .as("Null amount contributes 0 to sum via COALESCE")
        .isEqualByComparingTo(BigDecimal.ZERO);
  }
}
