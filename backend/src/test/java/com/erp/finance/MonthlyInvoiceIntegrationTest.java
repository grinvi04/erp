package com.erp.finance;

import com.erp.common.AbstractIntegrationTest;
import com.erp.finance.application.dto.MonthlyInvoiceByCurrencyResponse;
import com.erp.finance.application.dto.MonthlyInvoiceResponse;
import com.erp.finance.application.service.FinanceAnalyticsService;
import com.erp.finance.domain.model.ApInvoice;
import com.erp.finance.domain.model.Vendor;
import com.erp.finance.domain.repository.ApInvoiceRepository;
import com.erp.finance.domain.repository.VendorRepository;
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
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject("test-user").claim("sub", "test-user").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt,
            List.of(new SimpleGrantedAuthority("finance:read"), new SimpleGrantedAuthority("finance:write"))));
        vendor = vendorRepository.save(
                Vendor.of("V-ANALYTICS", "Analytics Vendor", "000-00-11111",
                        "홍길동", "vendor@test.com", "010-0000-0000", 30));
    }

    @AfterEach
    void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    private ApInvoice invoice(String no, LocalDate date, BigDecimal amount, String currency) {
        return apInvoiceRepository.save(
                ApInvoice.create(no, vendor, date, date.plusDays(30), amount, currency, null));
    }

    private List<MonthlyInvoiceResponse> monthsOf(List<MonthlyInvoiceByCurrencyResponse> result, String currency) {
        return result.stream()
                .filter(s -> s.currency().equals(currency))
                .findFirst()
                .orElseThrow()
                .months();
    }

    @Test
    void monthlyInvoices_separatesByCurrencyAndFillsAll12Months() {
        // KRW — 2026-01: 2 invoices, 2026-03: 1 invoice
        invoice("INV-2026-01A", LocalDate.of(2026, 1, 10), BigDecimal.valueOf(100_000), "KRW");
        invoice("INV-2026-01B", LocalDate.of(2026, 1, 20), BigDecimal.valueOf(200_000), "KRW");
        invoice("INV-2026-03A", LocalDate.of(2026, 3, 15), BigDecimal.valueOf(50_000), "KRW");
        // USD — 2026-01: 1 invoice (혼합통화 — KRW와 합산되면 안 됨)
        invoice("INV-2026-01U", LocalDate.of(2026, 1, 5), BigDecimal.valueOf(500), "USD");

        List<MonthlyInvoiceByCurrencyResponse> result = financeAnalyticsService.getMonthlyInvoices(2026);

        // 통화별 2시리즈, currency 정렬 (KRW < USD)
        assertThat(result).extracting(MonthlyInvoiceByCurrencyResponse::currency)
                .containsExactly("KRW", "USD");

        // KRW: 12개월 0채움, 통화 내 월합산
        List<MonthlyInvoiceResponse> krw = monthsOf(result, "KRW");
        assertThat(krw).hasSize(12);
        assertThat(krw.get(0).month()).isEqualTo(1);
        assertThat(krw.get(0).count()).isEqualTo(2L);
        assertThat(krw.get(0).totalAmount()).isEqualByComparingTo(BigDecimal.valueOf(300_000));
        assertThat(krw.get(1).count()).isEqualTo(0L);
        assertThat(krw.get(1).totalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(krw.get(2).month()).isEqualTo(3);
        assertThat(krw.get(2).count()).isEqualTo(1L);
        assertThat(krw.get(2).totalAmount()).isEqualByComparingTo(BigDecimal.valueOf(50_000));
        for (int i = 3; i < 12; i++) {
            assertThat(krw.get(i).count()).as("KRW month %d should be 0", i + 1).isEqualTo(0L);
            assertThat(krw.get(i).totalAmount()).as("KRW month %d amount 0", i + 1)
                    .isEqualByComparingTo(BigDecimal.ZERO);
        }

        // USD: 별도 12개월 시리즈, 1월에만 USD 금액 (KRW와 분리됨)
        List<MonthlyInvoiceResponse> usd = monthsOf(result, "USD");
        assertThat(usd).hasSize(12);
        assertThat(usd.get(0).count()).isEqualTo(1L);
        assertThat(usd.get(0).totalAmount()).isEqualByComparingTo(BigDecimal.valueOf(500));
        for (int i = 1; i < 12; i++) {
            assertThat(usd.get(i).count()).as("USD month %d should be 0", i + 1).isEqualTo(0L);
            assertThat(usd.get(i).totalAmount()).as("USD month %d amount 0", i + 1)
                    .isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Test
    void monthlyInvoices_emptyWhenNoData() {
        assertThat(financeAnalyticsService.getMonthlyInvoices(2026)).isEmpty();
    }
}
