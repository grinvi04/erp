package com.erp.finance;

import static org.assertj.core.api.Assertions.assertThat;

import com.erp.common.AbstractIntegrationTest;
import com.erp.finance.domain.model.ArInvoice;
import com.erp.finance.domain.model.ChargeType;
import com.erp.finance.domain.model.Customer;
import com.erp.finance.domain.model.PartySnapshot;
import com.erp.finance.domain.model.TaxInvoice;
import com.erp.finance.domain.model.TaxInvoiceStatus;
import com.erp.finance.domain.model.TaxType;
import com.erp.finance.domain.repository.ArInvoiceRepository;
import com.erp.finance.domain.repository.CustomerRepository;
import com.erp.finance.domain.repository.PartyAmountRow;
import com.erp.finance.domain.repository.TaxInvoiceRepository;
import com.erp.finance.domain.repository.TaxTypeAmountRow;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * 매출(발행 세금계산서) 집계 쿼리 검증(실 DB GROUP BY) — 과세구분별 합계·매출처별 합계표·기간 경계·상태 필터·사업자번호 null 그룹. TaxInvoice는
 * ar_invoice FK를 가지므로 최소 AR 행을 시드해 참조 무결성을 만족시킨다.
 */
@Transactional
class TaxInvoiceSalesAggregationIntegrationTest extends AbstractIntegrationTest {

  @Autowired private TaxInvoiceRepository taxInvoiceRepository;
  @Autowired private ArInvoiceRepository arInvoiceRepository;
  @Autowired private CustomerRepository customerRepository;

  private static final LocalDate FROM = LocalDate.of(2025, 4, 1);
  private static final LocalDate TO = LocalDate.of(2025, 6, 30);

  private Customer customer;
  private int arSeq = 0;

  @BeforeEach
  void setUp() {
    authenticate("seeder", "finance:read");
    customer = customerRepository.save(Customer.of("C-AGG", "고객", null, null, null, null, 30));
  }

  private void saveSales(
      LocalDate writeDate,
      TaxType taxType,
      long supply,
      long vat,
      String buyerBizNo,
      String buyerName,
      TaxInvoiceStatus status) {
    ArInvoice ar =
        arInvoiceRepository.save(
            ArInvoice.create(
                "AR-AGG-" + (++arSeq),
                customer,
                writeDate,
                writeDate.plusDays(30),
                BigDecimal.valueOf(supply),
                taxType,
                "KRW",
                null));
    TaxInvoice t =
        TaxInvoice.issue(
            ar.getId(),
            taxType,
            ChargeType.CHARGE,
            writeDate,
            BigDecimal.valueOf(supply),
            BigDecimal.valueOf(vat),
            BigDecimal.valueOf(supply + vat),
            "품목",
            PartySnapshot.of("(주)공급자", "1208147521", null, null, null, null),
            PartySnapshot.of(buyerName, buyerBizNo, null, null, null, null),
            null);
    if (status == TaxInvoiceStatus.CANCELLED) {
      t.cancel();
    }
    taxInvoiceRepository.save(t);
  }

  @Test
  void sumSalesByTaxType_groupsAndExcludesCancelledAndOutOfPeriod() {
    // 기간 내: 과세(공급 100만·세액 10만), 영세율(공급 50만·세액 0), 면세(공급 30만·세액 0)
    saveSales(
        FROM, TaxType.TAXABLE, 1_000_000, 100_000, "2208612345", "(주)A", TaxInvoiceStatus.ISSUED);
    saveSales(TO, TaxType.ZERO_RATED, 500_000, 0, "2208612345", "(주)A", TaxInvoiceStatus.ISSUED);
    saveSales(
        LocalDate.of(2025, 5, 1),
        TaxType.EXEMPT,
        300_000,
        0,
        "3308612345",
        "(주)B",
        TaxInvoiceStatus.ISSUED);
    // 제외: 취소 건, 기간 밖 건
    saveSales(
        LocalDate.of(2025, 5, 2),
        TaxType.TAXABLE,
        999_999,
        99_999,
        "2208612345",
        "(주)A",
        TaxInvoiceStatus.CANCELLED);
    saveSales(
        LocalDate.of(2025, 3, 31),
        TaxType.TAXABLE,
        777_777,
        77_777,
        "2208612345",
        "(주)A",
        TaxInvoiceStatus.ISSUED);

    Map<TaxType, TaxTypeAmountRow> byType =
        taxInvoiceRepository.sumSalesByTaxType(FROM, TO).stream()
            .collect(Collectors.toMap(TaxTypeAmountRow::getTaxType, Function.identity()));

    assertThat(byType.get(TaxType.TAXABLE).getSupplyTotal()).isEqualByComparingTo("1000000");
    assertThat(byType.get(TaxType.TAXABLE).getVatTotal()).isEqualByComparingTo("100000");
    assertThat(byType.get(TaxType.ZERO_RATED).getSupplyTotal()).isEqualByComparingTo("500000");
    assertThat(byType.get(TaxType.ZERO_RATED).getVatTotal()).isEqualByComparingTo("0");
    assertThat(byType.get(TaxType.EXEMPT).getSupplyTotal()).isEqualByComparingTo("300000");
    // 취소·기간밖 제외 → 과세는 100만만
    assertThat(byType.get(TaxType.TAXABLE).getSupplyTotal()).isEqualByComparingTo("1000000");
  }

  @Test
  void aggregateSalesByBuyer_groupsBySameBusinessNo() {
    // 같은 사업자번호 2건 → 1행 합산, 다른 거래처 1건, 사업자번호 null 1건
    saveSales(
        FROM, TaxType.TAXABLE, 1_000_000, 100_000, "2208612345", "(주)A", TaxInvoiceStatus.ISSUED);
    saveSales(
        TO, TaxType.TAXABLE, 2_000_000, 200_000, "2208612345", "(주)A", TaxInvoiceStatus.ISSUED);
    saveSales(
        LocalDate.of(2025, 5, 1),
        TaxType.TAXABLE,
        500_000,
        50_000,
        "3308612345",
        "(주)B",
        TaxInvoiceStatus.ISSUED);
    saveSales(
        LocalDate.of(2025, 5, 2),
        TaxType.TAXABLE,
        100_000,
        10_000,
        null,
        "(주)무번호",
        TaxInvoiceStatus.ISSUED);

    List<PartyAmountRow> rows = taxInvoiceRepository.aggregateSalesByBuyer(FROM, TO);

    Map<String, PartyAmountRow> byName =
        rows.stream().collect(Collectors.toMap(PartyAmountRow::getName, Function.identity()));
    assertThat(byName.get("(주)A").getCount()).isEqualTo(2);
    assertThat(byName.get("(주)A").getSupplyTotal()).isEqualByComparingTo("3000000");
    assertThat(byName.get("(주)A").getVatTotal()).isEqualByComparingTo("300000");
    assertThat(byName.get("(주)B").getCount()).isEqualTo(1);
    // 사업자번호 null 거래처도 누락 없이 포함(AC-8)
    assertThat(byName).containsKey("(주)무번호");
    assertThat(byName.get("(주)무번호").getBusinessNo()).isNull();
  }

  @Test
  void periodBoundary_isInclusive() {
    saveSales(
        FROM, TaxType.TAXABLE, 100_000, 10_000, "2208612345", "(주)A", TaxInvoiceStatus.ISSUED);
    saveSales(TO, TaxType.TAXABLE, 200_000, 20_000, "2208612345", "(주)A", TaxInvoiceStatus.ISSUED);

    var rows = taxInvoiceRepository.sumSalesByTaxType(FROM, TO);
    assertThat(rows).hasSize(1);
    assertThat(rows.get(0).getSupplyTotal()).isEqualByComparingTo("300000"); // 경계일 둘 다 포함
  }
}
