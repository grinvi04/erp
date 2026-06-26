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
    @Mock private com.erp.common.security.PermissionChecker permissionChecker;
    @InjectMocks private FinanceSummaryService financeSummaryService;

    @Test
    void getSummary_aggregatesInvoiceAndJournalCounts() {
        given(apInvoiceRepository.countUnpaid()).willReturn(5L);
        given(apInvoiceRepository.sumUnpaidAmountByCurrency()).willReturn(List.of(
                new CurrencyAmount("KRW", new BigDecimal("12345.67")),
                new CurrencyAmount("USD", new BigDecimal("89.00"))));
        given(journalEntryRepository.countByStatus(JournalEntryStatus.DRAFT)).willReturn(2L);

        FinanceSummaryResponse result = financeSummaryService.getSummary();

        assertThat(result.unpaidInvoices()).isEqualTo(5L);
        assertThat(result.unpaidAmounts())
                .extracting(CurrencyAmount::currency)
                .containsExactly("KRW", "USD");
        assertThat(result.unpaidAmounts().get(0).amount()).isEqualByComparingTo("12345.67");
        assertThat(result.unpaidAmounts().get(1).amount()).isEqualByComparingTo("89.00");
        assertThat(result.draftJournalEntries()).isEqualTo(2L);
    }

    @Test
    void getSummary_noUnpaidInvoices_returnsEmptyList() {
        given(apInvoiceRepository.countUnpaid()).willReturn(0L);
        given(apInvoiceRepository.sumUnpaidAmountByCurrency()).willReturn(List.of());
        given(journalEntryRepository.countByStatus(JournalEntryStatus.DRAFT)).willReturn(0L);

        FinanceSummaryResponse result = financeSummaryService.getSummary();

        assertThat(result.unpaidAmounts()).isEmpty();
    }
}
