package com.erp.finance;

import static org.assertj.core.api.Assertions.assertThat;

import com.erp.common.AbstractIntegrationTest;
import com.erp.finance.domain.model.ApInvoice;
import com.erp.finance.domain.model.ApInvoiceStatus;
import com.erp.finance.domain.model.TaxType;
import com.erp.finance.domain.model.Vendor;
import com.erp.finance.domain.repository.ApInvoiceRepository;
import com.erp.finance.domain.repository.PartyAmountRow;
import com.erp.finance.domain.repository.VendorRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

/**
 * 매입(매입 인보이스) 집계 쿼리 검증(실 DB GROUP BY) — 승인/완납만, 매입처별 사업자번호 합산, 기간·상태 필터. 집계 쿼리 검증이 목적이므로 상태는
 * reflection으로 직접 세팅(GL 전기 흐름 불필요).
 */
@Transactional
class ApInvoicePurchaseAggregationIntegrationTest extends AbstractIntegrationTest {

  @Autowired private ApInvoiceRepository apInvoiceRepository;
  @Autowired private VendorRepository vendorRepository;

  private static final LocalDate FROM = LocalDate.of(2025, 4, 1);
  private static final LocalDate TO = LocalDate.of(2025, 6, 30);

  private Vendor vendorA;
  private Vendor vendorB;
  private int seq = 0;

  @BeforeEach
  void setUp() {
    authenticate("seeder", "finance:read");
    vendorA = vendorRepository.save(Vendor.of("V-A", "(주)매입A", "2208612345", null, null, null, 30));
    vendorB = vendorRepository.save(Vendor.of("V-B", "(주)매입B", "3308612345", null, null, null, 30));
  }

  private void savePurchase(
      Vendor vendor, LocalDate invoiceDate, long supply, TaxType taxType, ApInvoiceStatus status) {
    savePurchase(vendor, invoiceDate, supply, taxType, status, "KRW");
  }

  private void savePurchase(
      Vendor vendor,
      LocalDate invoiceDate,
      long supply,
      TaxType taxType,
      ApInvoiceStatus status,
      String currency) {
    ApInvoice inv =
        ApInvoice.create(
            "AP-AGG-" + (++seq),
            vendor,
            invoiceDate,
            invoiceDate.plusDays(30),
            BigDecimal.valueOf(supply),
            taxType,
            currency,
            null);
    ReflectionTestUtils.setField(inv, "status", status);
    apInvoiceRepository.save(inv);
  }

  @Test
  void aggregatePurchasesByVendor_sumsApprovedAndExcludesDraftAndOutOfPeriod() {
    // 매입처A: 승인 2건(공급 100만+50만, 과세 → 세액 10만+5만)
    savePurchase(vendorA, FROM, 1_000_000, TaxType.TAXABLE, ApInvoiceStatus.APPROVED);
    savePurchase(vendorA, TO, 500_000, TaxType.TAXABLE, ApInvoiceStatus.PAID);
    // 매입처B: 승인 1건(공급 300만, 과세 → 세액 30만)
    savePurchase(
        vendorB, LocalDate.of(2025, 5, 1), 3_000_000, TaxType.TAXABLE, ApInvoiceStatus.APPROVED);
    // 제외: 미승인(DRAFT)·기간 밖·외화(USD, 국내 부가세 아님)
    savePurchase(
        vendorA, LocalDate.of(2025, 5, 2), 999_999, TaxType.TAXABLE, ApInvoiceStatus.DRAFT);
    savePurchase(
        vendorA, LocalDate.of(2025, 3, 31), 777_777, TaxType.TAXABLE, ApInvoiceStatus.APPROVED);
    savePurchase(
        vendorB,
        LocalDate.of(2025, 5, 3),
        888_888,
        TaxType.TAXABLE,
        ApInvoiceStatus.APPROVED,
        "USD");

    List<PartyAmountRow> rows = apInvoiceRepository.aggregatePurchasesByVendor(FROM, TO);

    Map<String, PartyAmountRow> byName =
        rows.stream().collect(Collectors.toMap(PartyAmountRow::getName, Function.identity()));
    // 매입처A: 승인·완납 2건, 공급 150만, 세액 15만 (DRAFT·기간밖 제외)
    assertThat(byName.get("(주)매입A").getCount()).isEqualTo(2);
    assertThat(byName.get("(주)매입A").getSupplyTotal()).isEqualByComparingTo("1500000");
    assertThat(byName.get("(주)매입A").getVatTotal()).isEqualByComparingTo("150000");
    assertThat(byName.get("(주)매입A").getBusinessNo()).isEqualTo("2208612345");
    // 매입처B
    assertThat(byName.get("(주)매입B").getCount()).isEqualTo(1);
    assertThat(byName.get("(주)매입B").getVatTotal()).isEqualByComparingTo("300000");
  }

  @Test
  void aggregatePurchasesByVendor_periodBoundaryInclusive() {
    savePurchase(vendorA, FROM, 100_000, TaxType.TAXABLE, ApInvoiceStatus.APPROVED);
    savePurchase(vendorA, TO, 200_000, TaxType.TAXABLE, ApInvoiceStatus.APPROVED);

    List<PartyAmountRow> rows = apInvoiceRepository.aggregatePurchasesByVendor(FROM, TO);
    assertThat(rows).hasSize(1);
    assertThat(rows.get(0).getSupplyTotal()).isEqualByComparingTo("300000");
  }
}
