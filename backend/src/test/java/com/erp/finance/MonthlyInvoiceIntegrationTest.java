package com.erp.finance;

import com.erp.common.AbstractIntegrationTest;
import com.erp.finance.application.dto.MonthlyInvoiceResponse;
import com.erp.finance.application.service.FinanceAnalyticsService;
import com.erp.finance.domain.model.ApInvoice;
import com.erp.finance.domain.model.Vendor;
import com.erp.finance.domain.repository.ApInvoiceRepository;
import com.erp.finance.domain.repository.VendorRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class MonthlyInvoiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ApInvoiceRepository apInvoiceRepository;
    @Autowired
    private VendorRepository vendorRepository;
    @Autowired
    private FinanceAnalyticsService financeAnalyticsService;

    private Vendor vendor;

    @BeforeEach
    void setUp() {
        vendor = vendorRepository.save(
                Vendor.of("V-ANALYTICS", "Analytics Vendor", "000-00-11111",
                        "홍길동", "vendor@test.com", "010-0000-0000", 30));
    }

    private ApInvoice invoice(String no, LocalDate date, BigDecimal amount) {
        return apInvoiceRepository.save(
                ApInvoice.create(no, vendor, date, date.plusDays(30), amount, "KRW", null));
    }

    @Test
    void monthlyInvoices_fillsAll12MonthsAndCountsCorrectly() {
        // 2026-01: 2 invoices, 2026-03: 1 invoice
        invoice("INV-2026-01A", LocalDate.of(2026, 1, 10), BigDecimal.valueOf(100_000));
        invoice("INV-2026-01B", LocalDate.of(2026, 1, 20), BigDecimal.valueOf(200_000));
        invoice("INV-2026-03A", LocalDate.of(2026, 3, 15), BigDecimal.valueOf(50_000));

        List<MonthlyInvoiceResponse> result = financeAnalyticsService.getMonthlyInvoices(2026);

        assertThat(result).hasSize(12);

        MonthlyInvoiceResponse jan = result.get(0);
        assertThat(jan.month()).isEqualTo(1);
        assertThat(jan.count()).isEqualTo(2L);
        assertThat(jan.totalAmount()).isEqualByComparingTo(BigDecimal.valueOf(300_000));

        MonthlyInvoiceResponse feb = result.get(1);
        assertThat(feb.month()).isEqualTo(2);
        assertThat(feb.count()).isEqualTo(0L);
        assertThat(feb.totalAmount()).isEqualByComparingTo(BigDecimal.ZERO);

        MonthlyInvoiceResponse mar = result.get(2);
        assertThat(mar.month()).isEqualTo(3);
        assertThat(mar.count()).isEqualTo(1L);
        assertThat(mar.totalAmount()).isEqualByComparingTo(BigDecimal.valueOf(50_000));

        // All remaining months should be zero
        for (int i = 3; i < 12; i++) {
            assertThat(result.get(i).count())
                    .as("Month %d should have 0 invoices", i + 1)
                    .isEqualTo(0L);
            assertThat(result.get(i).totalAmount())
                    .as("Month %d should have 0 total amount", i + 1)
                    .isEqualByComparingTo(BigDecimal.ZERO);
        }
    }
}
