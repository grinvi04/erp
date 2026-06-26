package com.erp.finance;

import com.erp.common.AbstractIntegrationTest;
import com.erp.common.response.CurrencyAmount;
import com.erp.finance.domain.model.ApInvoice;
import com.erp.finance.domain.model.Vendor;
import com.erp.finance.domain.repository.ApInvoiceRepository;
import com.erp.finance.domain.repository.VendorRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class ApInvoiceUnpaidQueryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ApInvoiceRepository invoiceRepository;
    @Autowired
    private VendorRepository vendorRepository;

    private Vendor vendor;

    @BeforeEach
    void setUp() {
        vendor = vendorRepository.save(
                Vendor.of("V-TEST", "Test Vendor", "000-00-00000",
                        "홍길동", "vendor@test.com", "010-0000-0000", 30));
    }

    private ApInvoice approved(String no, BigDecimal total) {
        return approved(no, total, "KRW");
    }

    private ApInvoice approved(String no, BigDecimal total, String currency) {
        ApInvoice inv = ApInvoice.create(no, vendor,
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 2, 1),
                total, currency, null);
        inv.submit();
        inv.approve();
        return invoiceRepository.save(inv);
    }

    @Test
    void countUnpaid_and_sumUnpaidAmountByCurrency_groupOnlyUnpaidInvoicesPerCurrency() {
        // Invoice 1: KRW, totalAmount=1000, paidAmount=0 (APPROVED) → unpaid, outstanding=1000
        approved("INV-001", BigDecimal.valueOf(1000));

        // Invoice 1b: KRW partially paid → totalAmount=400, paidAmount=100 → outstanding=300
        ApInvoice inv1b = approved("INV-001B", BigDecimal.valueOf(400));
        inv1b.pay(BigDecimal.valueOf(100));
        invoiceRepository.save(inv1b);

        // Invoice USD: separate currency, totalAmount=80, paidAmount=0 → outstanding=80
        approved("INV-USD", BigDecimal.valueOf(80), "USD");

        // Invoice 2: KRW fully paid → status PAID → NOT counted
        ApInvoice inv2 = approved("INV-002", BigDecimal.valueOf(500));
        inv2.pay(BigDecimal.valueOf(500));
        invoiceRepository.save(inv2);

        // Invoice 3: KRW CANCELLED → NOT counted
        ApInvoice inv3 = ApInvoice.create("INV-003", vendor,
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 2, 1),
                BigDecimal.valueOf(300), "KRW", null);
        inv3.cancel();
        invoiceRepository.save(inv3);

        assertThat(invoiceRepository.countUnpaid())
                .as("KRW(1000) + KRW(300 outstanding) + USD(80) = 3 unpaid invoices; paid/cancelled excluded")
                .isEqualTo(3L);

        // ORDER BY currency → KRW row before USD row; amounts summed within each currency, no cross-currency mix
        var rows = invoiceRepository.sumUnpaidAmountByCurrency();
        assertThat(rows)
                .as("Outstanding split by currency: KRW=1000+300=1300, USD=80 (no FX conversion)")
                .extracting(CurrencyAmount::currency)
                .containsExactly("KRW", "USD");
        assertThat(rows.get(0).amount()).isEqualByComparingTo(BigDecimal.valueOf(1300));
        assertThat(rows.get(1).amount()).isEqualByComparingTo(BigDecimal.valueOf(80));
    }

    @Test
    void countUnpaid_partiallyPaidInvoice_isStillCountedAsUnpaid() {
        // Invoice partially paid: totalAmount=800, paidAmount=300 → outstanding=500 → unpaid
        ApInvoice partial = approved("INV-PARTIAL", BigDecimal.valueOf(800));
        partial.pay(BigDecimal.valueOf(300));
        invoiceRepository.save(partial);

        // Invoice fully paid → excluded
        ApInvoice full = approved("INV-FULL", BigDecimal.valueOf(200));
        full.pay(BigDecimal.valueOf(200));
        invoiceRepository.save(full);

        assertThat(invoiceRepository.countUnpaid()).isEqualTo(1L);
        var rows = invoiceRepository.sumUnpaidAmountByCurrency();
        assertThat(rows).extracting(CurrencyAmount::currency).containsExactly("KRW");
        assertThat(rows.get(0).amount()).isEqualByComparingTo(BigDecimal.valueOf(500));
    }

    @Test
    void sumUnpaidAmountByCurrency_noUnpaidInvoices_returnsEmptyList() {
        // All invoices fully paid or cancelled — GROUP BY yields no rows
        ApInvoice inv = approved("INV-ZERO", BigDecimal.valueOf(100));
        inv.pay(BigDecimal.valueOf(100));
        invoiceRepository.save(inv);

        assertThat(invoiceRepository.countUnpaid()).isEqualTo(0L);
        assertThat(invoiceRepository.sumUnpaidAmountByCurrency())
                .as("No unpaid invoices → no currency groups → empty list")
                .isEmpty();
    }
}
