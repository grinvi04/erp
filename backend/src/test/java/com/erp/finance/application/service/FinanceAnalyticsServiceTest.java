package com.erp.finance.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;

import com.erp.finance.application.dto.MonthlyInvoiceAnalyticsResponse;
import com.erp.finance.application.dto.MonthlyInvoiceResponse;
import com.erp.finance.domain.repository.ApInvoiceRepository;
import com.erp.finance.domain.repository.MonthlyBaseRow;
import com.erp.finance.domain.repository.MonthlyInvoiceRow;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FinanceAnalyticsServiceTest {

  @Mock private ApInvoiceRepository apInvoiceRepository;
  @Mock private BaseCurrencyService baseCurrencyService;
  @Mock private com.erp.common.security.PermissionChecker permissionChecker;
  @InjectMocks private FinanceAnalyticsService financeAnalyticsService;

  private MonthlyInvoiceRow invoiceRow(int month, String currency, long count, String amount) {
    MonthlyInvoiceRow row = org.mockito.Mockito.mock(MonthlyInvoiceRow.class);
    lenient().when(row.getMonth()).thenReturn(month);
    lenient().when(row.getCurrency()).thenReturn(currency);
    lenient().when(row.getCount()).thenReturn(count);
    lenient().when(row.getTotalAmount()).thenReturn(new BigDecimal(amount));
    return row;
  }

  private MonthlyBaseRow baseRow(int month, String baseTotal) {
    MonthlyBaseRow row = org.mockito.Mockito.mock(MonthlyBaseRow.class);
    lenient().when(row.getMonth()).thenReturn(month);
    lenient().when(row.getBaseTotal()).thenReturn(new BigDecimal(baseTotal));
    return row;
  }

  @Test
  void getMonthlyInvoices_returnsCurrencySeriesPlusBaseTotalSeries() {
    // 모의 행은 given() 호출 전에 구성한다(중첩 stubbing → UnfinishedStubbingException 방지).
    List<MonthlyInvoiceRow> invoiceRows =
        List.of(invoiceRow(3, "KRW", 2L, "1000000"), invoiceRow(3, "USD", 1L, "500"));
    // 기준통화 환산 합계: 3월 = KRW 1000000 + USD환산 650000 = 1650000
    List<MonthlyBaseRow> baseRows = List.of(baseRow(3, "1650000"));
    given(baseCurrencyService.currentBaseCurrencyCode()).willReturn("KRW");
    given(apInvoiceRepository.monthlyInvoices(2026)).willReturn(invoiceRows);
    given(apInvoiceRepository.monthlyBaseTotals(2026)).willReturn(baseRows);

    MonthlyInvoiceAnalyticsResponse result = financeAnalyticsService.getMonthlyInvoices(2026);

    assertThat(result.baseCurrency()).isEqualTo("KRW");
    // 통화별 분리는 그대로 유지(회귀)
    assertThat(result.byCurrency()).extracting("currency").containsExactly("KRW", "USD");
    // 기준통화 합계 시리즈 — 12개월, 3월만 값
    assertThat(result.baseMonthlyTotals()).hasSize(12);
    MonthlyInvoiceResponse march = result.baseMonthlyTotals().get(2);
    assertThat(march.month()).isEqualTo(3);
    assertThat(march.totalAmount()).isEqualByComparingTo("1650000");
    assertThat(result.baseMonthlyTotals().get(0).totalAmount()).isEqualByComparingTo("0");
  }

  @Test
  void getMonthlyInvoices_noPricedRows_emptyBaseSeries() {
    List<MonthlyInvoiceRow> invoiceRows = List.of(invoiceRow(3, "USD", 1L, "500"));
    given(baseCurrencyService.currentBaseCurrencyCode()).willReturn("KRW");
    given(apInvoiceRepository.monthlyInvoices(2026)).willReturn(invoiceRows);
    // 산정된(base_amount not-null) 행이 없으면 빈 시리즈
    given(apInvoiceRepository.monthlyBaseTotals(2026)).willReturn(List.of());

    MonthlyInvoiceAnalyticsResponse result = financeAnalyticsService.getMonthlyInvoices(2026);

    assertThat(result.byCurrency()).hasSize(1);
    assertThat(result.baseMonthlyTotals()).isEmpty();
  }
}
