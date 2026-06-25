package com.erp.finance;

import com.erp.common.AbstractIntegrationTest;
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
        ApInvoice inv = ApInvoice.create(no, vendor,
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 2, 1),
                total, "KRW", null);
        inv.submit();
        inv.approve();
        return invoiceRepository.save(inv);
    }

    @Test
    void countUnpaid_and_sumUnpaidAmount_countOnlyUnpaidInvoices() {
        // Invoice 1: totalAmount=1000, paidAmount=0 (APPROVED) → unpaid, outstanding=1000
        approved("INV-001", BigDecimal.valueOf(1000));

        // Invoice 2: totalAmount=500, fully paid → status PAID → NOT counted
        ApInvoice inv2 = approved("INV-002", BigDecimal.valueOf(500));
        inv2.pay(BigDecimal.valueOf(500));
        invoiceRepository.save(inv2);

        // Invoice 3: CANCELLED → NOT counted
        ApInvoice inv3 = ApInvoice.create("INV-003", vendor,
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 2, 1),
                BigDecimal.valueOf(300), "KRW", null);
        inv3.cancel();
        invoiceRepository.save(inv3);

        assertThat(invoiceRepository.countUnpaid())
                .as("Only invoice 1 (APPROVED, paidAmount < totalAmount) should be counted")
                .isEqualTo(1L);

        assertThat(invoiceRepository.sumUnpaidAmount())
                .as("Outstanding amount for invoice 1: 1000 - 0 = 1000")
                .isEqualByComparingTo(BigDecimal.valueOf(1000));
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
        assertThat(invoiceRepository.sumUnpaidAmount())
                .isEqualByComparingTo(BigDecimal.valueOf(500));
    }

    @Test
    void sumUnpaidAmount_noUnpaidInvoices_returnsZero() {
        // All invoices fully paid or cancelled — sum should be 0 (COALESCE)
        ApInvoice inv = approved("INV-ZERO", BigDecimal.valueOf(100));
        inv.pay(BigDecimal.valueOf(100));
        invoiceRepository.save(inv);

        assertThat(invoiceRepository.countUnpaid()).isEqualTo(0L);
        assertThat(invoiceRepository.sumUnpaidAmount())
                .as("COALESCE(SUM, 0) must return 0 when no unpaid invoices exist")
                .isEqualByComparingTo(BigDecimal.ZERO);
    }
}
