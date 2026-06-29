package com.erp.finance;

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
import com.erp.finance.domain.model.ApInvoice;
import com.erp.finance.domain.model.TaxType;
import com.erp.finance.domain.model.Vendor;
import com.erp.finance.domain.repository.ApInvoiceRepository;
import com.erp.finance.domain.repository.VendorRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * AC-12: 요약 기준통화 합계 집계가 base_amount 산정분만 합산하고(미산정 null 행 제외), 기존 통화별 분리 합계는 영향받지 않는지(회귀) 검증한다.
 * base_amount는 거래 시점 스냅샷(PR2)이므로 합계는 단순 SUM이며, 여기서는 산정분은 applyBaseSnapshot으로, 미산정 행은 스냅샷 미적용으로 만든다.
 */
@Transactional
class FxBaseTotalAggregationIntegrationTest extends AbstractIntegrationTest {

  @Autowired private ApInvoiceRepository apInvoiceRepository;
  @Autowired private VendorRepository vendorRepository;
  @Autowired private OpportunityRepository opportunityRepository;
  @Autowired private CrmAccountRepository crmAccountRepository;
  @Autowired private PipelineStageRepository pipelineStageRepository;

  @BeforeEach
  void authenticateUser() {
    authenticate("test-user", "finance:read", "crm:read");
  }

  @Test
  void apInvoice_unpaidBaseTotal_usesOutstandingBalance_andStaysConsistentWithCurrencySplit() {
    Vendor vendor =
        vendorRepository.save(
            Vendor.of("V-FX", "FX공급사", "000-00-11111", "홍길동", "v@test.com", "010-0000-0000", 30));
    // 부분지급(KRW, rate 1): 전액 1,000,000 스냅샷이지만 400,000 지급 → 미지급잔액 600,000
    apInvoiceRepository.save(
        pricedPartiallyPaid(vendor, "AP-KRW", "1000000", "KRW", "1000000", "1", "400000"));
    // 미지급(USD, rate 1300): base = 1300 × 미지급잔액 100 = 130,000
    apInvoiceRepository.save(priced(vendor, "AP-USD", "100", "USD", "130000", "1300"));
    // 미산정(환율 부재): base_amount NULL — SUM에서 제외돼야 한다
    apInvoiceRepository.save(unpriced(vendor, "AP-JPY", "5000", "JPY"));

    // 기준통화 합계 = 미지급잔액 기준(전액 1,000,000 아님): KRW 600,000 + USD 130,000 = 730,000.
    // JPY는 미산정(null)이라 제외.
    assertThat(apInvoiceRepository.sumUnpaidBaseTotal()).isEqualByComparingTo("730000");

    // 합계가 "일부 미환산"임을 알리는 신호: 미산정(JPY) 1건이 제외됐으므로 카운트 > 0.
    assertThat(apInvoiceRepository.countUnpaidUnconverted()).isEqualTo(1L);

    // 통화별 분리도 미지급잔액 기준(회귀) — currency 정렬: JPY, KRW, USD
    List<CurrencyAmount> split = apInvoiceRepository.sumUnpaidAmountByCurrency();
    assertThat(split).extracting(CurrencyAmount::currency).containsExactly("JPY", "KRW", "USD");
    assertThat(amountOf(split, "JPY")).isEqualByComparingTo("5000");
    assertThat(amountOf(split, "KRW")).isEqualByComparingTo("600000");
    assertThat(amountOf(split, "USD")).isEqualByComparingTo("100");

    // 정합성: base 합계 = 산정분(KRW·USD) 통화별 미지급잔액을 스냅샷 환율로 환산한 합과 일치.
    // KRW 600,000×1 + USD 100×1300 = 730,000. (JPY는 미산정이라 base에서 제외 → 부분 합계)
    BigDecimal pricedSplitInBase =
        amountOf(split, "KRW")
            .multiply(BigDecimal.ONE)
            .add(amountOf(split, "USD").multiply(new BigDecimal("1300")));
    assertThat(apInvoiceRepository.sumUnpaidBaseTotal()).isEqualByComparingTo(pricedSplitInBase);
  }

  @Test
  void apInvoice_unpaidBaseTotal_nullWhenNoPricedRows() {
    Vendor vendor =
        vendorRepository.save(
            Vendor.of("V-NP", "미산정공급사", "000-00-22222", "홍길동", "np@test.com", "010-0000-0000", 30));
    apInvoiceRepository.save(unpriced(vendor, "AP-NP", "5000", "JPY"));

    // 산정된 미지급 행이 없으면 0이 아니라 null(미산정과 0 구분)
    assertThat(apInvoiceRepository.sumUnpaidBaseTotal()).isNull();
  }

  @Test
  void opportunity_openBaseTotal_sumsPricedRowsOnly_andCurrencySplitUnaffected() {
    // 진행중 단계(is_closed_won=false, is_closed_lost=false)
    PipelineStage stage = pipelineStageRepository.save(PipelineStage.of("탐색", 1, 10, false, false));
    Account account =
        crmAccountRepository.save(
            Account.of(
                "ACC-FX",
                "FX고객사",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                AccountType.CUSTOMER,
                "owner"));

    opportunityRepository.save(
        pricedOpp(account, stage, "OPP-KRW", "5000000", "KRW", "5000000", "1"));
    opportunityRepository.save(
        pricedOpp(account, stage, "OPP-USD", "1000", "USD", "1300000", "1300"));
    opportunityRepository.save(unpricedOpp(account, stage, "OPP-JPY", "200000", "JPY"));

    boolean allScope = false;
    Set<String> noOwners = Set.of();

    // 기준통화 합계 = 산정분만(JPY 제외)
    assertThat(opportunityRepository.sumOpenBaseTotal(allScope, noOwners))
        .isEqualByComparingTo("6300000");

    // "일부 미환산" 신호: 미산정(JPY) 1건이 제외됐으므로 카운트 > 0.
    assertThat(opportunityRepository.countOpenUnconverted(allScope, noOwners)).isEqualTo(1L);

    // 통화별 분리 유지(회귀) — JPY, KRW, USD
    List<CurrencyAmount> split = opportunityRepository.sumOpenAmountByCurrency(allScope, noOwners);
    assertThat(split).extracting(CurrencyAmount::currency).containsExactly("JPY", "KRW", "USD");
    assertThat(amountOf(split, "KRW")).isEqualByComparingTo("5000000");
    assertThat(amountOf(split, "USD")).isEqualByComparingTo("1000");
    assertThat(amountOf(split, "JPY")).isEqualByComparingTo("200000");
  }

  private ApInvoice priced(
      Vendor vendor, String no, String amount, String currency, String base, String rate) {
    ApInvoice inv = unpriced(vendor, no, amount, currency);
    inv.applyBaseSnapshot(new BigDecimal(base), new BigDecimal(rate));
    return inv;
  }

  /** 산정분(스냅샷 환율 적용) + 부분지급. paid만큼 지급해 미지급잔액(total-paid)을 남긴다. */
  private ApInvoice pricedPartiallyPaid(
      Vendor vendor,
      String no,
      String total,
      String currency,
      String base,
      String rate,
      String paid) {
    ApInvoice inv = priced(vendor, no, total, currency, base, rate);
    inv.submit();
    inv.approve();
    inv.pay(new BigDecimal(paid));
    return inv;
  }

  private ApInvoice unpriced(Vendor vendor, String no, String amount, String currency) {
    return ApInvoice.create(
        no,
        vendor,
        LocalDate.of(2025, 1, 1),
        LocalDate.of(2025, 1, 31),
        new BigDecimal(amount),
        TaxType.EXEMPT,
        currency,
        null);
  }

  private Opportunity pricedOpp(
      Account account,
      PipelineStage stage,
      String name,
      String amount,
      String currency,
      String base,
      String rate) {
    Opportunity opp = unpricedOpp(account, stage, name, amount, currency);
    opp.applyBaseSnapshot(new BigDecimal(base), new BigDecimal(rate));
    return opp;
  }

  private Opportunity unpricedOpp(
      Account account, PipelineStage stage, String name, String amount, String currency) {
    return Opportunity.of(
        account,
        name,
        stage,
        new BigDecimal(amount),
        currency,
        LocalDate.of(2026, 12, 31),
        10,
        "owner",
        null,
        null);
  }

  private BigDecimal amountOf(List<CurrencyAmount> list, String currency) {
    return list.stream()
        .filter(c -> c.currency().equals(currency))
        .findFirst()
        .orElseThrow()
        .amount();
  }
}
