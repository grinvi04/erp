package com.erp.finance.application.service;

import com.erp.common.response.CurrencyAmount;
import com.erp.finance.application.dto.FinanceSummaryResponse;
import com.erp.finance.domain.model.JournalEntryStatus;
import com.erp.finance.domain.repository.ApInvoiceRepository;
import com.erp.finance.domain.repository.JournalEntryRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class FinanceSummaryServiceTest {

    @Mock private ApInvoiceRepository apInvoiceRepository;
    @Mock private JournalEntryRepository journalEntryRepository;
    @Mock private BaseCurrencyService baseCurrencyService;
    @Mock private com.erp.common.security.PermissionChecker permissionChecker;
    @InjectMocks private FinanceSummaryService financeSummaryService;

    @Test
    void getSummary_aggregatesInvoiceAndJournalCounts() {
        given(apInvoiceRepository.countUnpaid()).willReturn(5L);
        given(apInvoiceRepository.sumUnpaidAmountByCurrency()).willReturn(List.of(
                new CurrencyAmount("KRW", new BigDecimal("12345.67")),
                new CurrencyAmount("USD", new BigDecimal("89.00"))));
        given(journalEntryRepository.countByStatus(JournalEntryStatus.DRAFT)).willReturn(2L);
        given(baseCurrencyService.currentBaseCurrencyCode()).willReturn("KRW");
        // 통화별 분리(12345.67 KRW + 89.00 USD환산)와 별개의 기준통화 합계
        given(apInvoiceRepository.sumUnpaidBaseTotal()).willReturn(new BigDecimal("131045.67"));

        FinanceSummaryResponse result = financeSummaryService.getSummary();

        assertThat(result.unpaidInvoices()).isEqualTo(5L);
        // 통화별 분리는 그대로 유지(회귀)
        assertThat(result.unpaidAmounts())
                .extracting(CurrencyAmount::currency)
                .containsExactly("KRW", "USD");
        assertThat(result.unpaidAmounts().get(0).amount()).isEqualByComparingTo("12345.67");
        assertThat(result.unpaidAmounts().get(1).amount()).isEqualByComparingTo("89.00");
        assertThat(result.draftJournalEntries()).isEqualTo(2L);
        // 기준통화 합계 추가
        assertThat(result.baseCurrency()).isEqualTo("KRW");
        assertThat(result.unpaidBaseTotal()).isEqualByComparingTo("131045.67");
    }

    @Test
    void getSummary_noUnpaidInvoices_returnsEmptyList() {
        given(apInvoiceRepository.countUnpaid()).willReturn(0L);
        given(apInvoiceRepository.sumUnpaidAmountByCurrency()).willReturn(List.of());
        given(journalEntryRepository.countByStatus(JournalEntryStatus.DRAFT)).willReturn(0L);
        given(baseCurrencyService.currentBaseCurrencyCode()).willReturn("KRW");
        // 산정된 미지급 행이 없으면 기준통화 합계는 null(0 아님)
        given(apInvoiceRepository.sumUnpaidBaseTotal()).willReturn(null);

        FinanceSummaryResponse result = financeSummaryService.getSummary();

        assertThat(result.unpaidAmounts()).isEmpty();
        assertThat(result.unpaidBaseTotal()).isNull();
        assertThat(result.baseCurrency()).isEqualTo("KRW");
    }
}
