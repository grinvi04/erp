package com.erp.finance.application.service;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.finance.application.dto.FiscalPeriodCreateRequest;
import com.erp.finance.application.dto.FiscalPeriodResponse;
import com.erp.finance.application.dto.FiscalYearCreateRequest;
import com.erp.finance.application.dto.FiscalYearResponse;
import com.erp.finance.domain.model.FiscalPeriod;
import com.erp.finance.domain.model.FiscalPeriodStatus;
import com.erp.finance.domain.model.FiscalYear;
import com.erp.finance.domain.repository.FiscalPeriodRepository;
import com.erp.finance.domain.repository.FiscalYearRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class FiscalYearServiceTest {

    @Mock
    private FiscalYearRepository fiscalYearRepository;

    @Mock
    private FiscalPeriodRepository fiscalPeriodRepository;

    @InjectMocks
    private FiscalYearService fiscalYearService;

    @Test
    void create_newYear_returnsFiscalYearResponse() {
        FiscalYearCreateRequest request = new FiscalYearCreateRequest(2025,
            LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));
        given(fiscalYearRepository.existsByYear(2025)).willReturn(false);
        FiscalYear fy = FiscalYear.of(2025, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));
        given(fiscalYearRepository.save(any())).willReturn(fy);

        FiscalYearResponse result = fiscalYearService.create(request);

        assertThat(result.year()).isEqualTo(2025);
    }

    @Test
    void create_duplicateYear_throwsFiscalYearDuplicate() {
        given(fiscalYearRepository.existsByYear(2025)).willReturn(true);

        ErpException ex = assertThrows(ErpException.class, () ->
            fiscalYearService.create(new FiscalYearCreateRequest(2025,
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31))));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FISCAL_YEAR_DUPLICATE);
    }

    @Test
    void close_openYear_noOpenPeriods_closesSuccessfully() {
        FiscalYear fy = FiscalYear.of(2024, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));
        given(fiscalYearRepository.findById(1L)).willReturn(Optional.of(fy));
        given(fiscalPeriodRepository.findByFiscalYearIdAndStatus(1L, FiscalPeriodStatus.OPEN))
            .willReturn(List.of());

        FiscalYearResponse result = fiscalYearService.close(1L);

        assertThat(result.status().name()).isEqualTo("CLOSED");
    }

    @Test
    void close_withOpenPeriods_closesAllPeriodsAndYear() {
        FiscalYear fy = FiscalYear.of(2024, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));
        FiscalPeriod openPeriod = FiscalPeriod.of(fy, 1, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31));
        given(fiscalYearRepository.findById(1L)).willReturn(Optional.of(fy));
        given(fiscalPeriodRepository.findByFiscalYearIdAndStatus(1L, FiscalPeriodStatus.OPEN))
            .willReturn(List.of(openPeriod));

        fiscalYearService.close(1L);

        assertThat(openPeriod.isOpen()).isFalse();
        assertThat(fy.isOpen()).isFalse();
    }

    @Test
    void close_notFound_throwsFiscalYearNotFound() {
        given(fiscalYearRepository.findById(99L)).willReturn(Optional.empty());

        ErpException ex = assertThrows(ErpException.class, () -> fiscalYearService.close(99L));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FISCAL_YEAR_NOT_FOUND);
    }

    @Test
    void createPeriod_valid_returnsFiscalPeriodResponse() {
        FiscalYear fy = FiscalYear.of(2025, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));
        given(fiscalYearRepository.findById(1L)).willReturn(Optional.of(fy));
        given(fiscalPeriodRepository.existsByFiscalYearIdAndPeriodNumber(1L, 1)).willReturn(false);
        FiscalPeriod fp = FiscalPeriod.of(fy, 1, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31));
        given(fiscalPeriodRepository.save(any())).willReturn(fp);

        FiscalPeriodResponse result = fiscalYearService.createPeriod(1L,
            new FiscalPeriodCreateRequest(1, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31)));

        assertThat(result.periodNumber()).isEqualTo(1);
    }

    @Test
    void createPeriod_duplicate_throwsFiscalPeriodDuplicate() {
        FiscalYear fy = FiscalYear.of(2025, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));
        given(fiscalYearRepository.findById(1L)).willReturn(Optional.of(fy));
        given(fiscalPeriodRepository.existsByFiscalYearIdAndPeriodNumber(1L, 1)).willReturn(true);

        ErpException ex = assertThrows(ErpException.class, () ->
            fiscalYearService.createPeriod(1L,
                new FiscalPeriodCreateRequest(1, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31))));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FISCAL_PERIOD_DUPLICATE);
    }

    @Test
    void createPeriod_dateOutOfYearRange_throwsFiscalPeriodDateOutOfRange() {
        FiscalYear fy = FiscalYear.of(2025, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));
        given(fiscalYearRepository.findById(1L)).willReturn(Optional.of(fy));
        given(fiscalPeriodRepository.existsByFiscalYearIdAndPeriodNumber(1L, 1)).willReturn(false);

        ErpException ex = assertThrows(ErpException.class, () ->
            fiscalYearService.createPeriod(1L,
                new FiscalPeriodCreateRequest(1, LocalDate.of(2025, 1, 1), LocalDate.of(2026, 1, 31))));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FISCAL_PERIOD_DATE_OUT_OF_RANGE);
    }

    @Test
    void findPeriodsByYear_returnsList() {
        FiscalYear fy = FiscalYear.of(2025, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));
        given(fiscalYearRepository.findById(1L)).willReturn(Optional.of(fy));
        FiscalPeriod fp = FiscalPeriod.of(fy, 1, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31));
        given(fiscalPeriodRepository.findByFiscalYearIdOrderByPeriodNumberAsc(1L)).willReturn(List.of(fp));

        List<FiscalPeriodResponse> result = fiscalYearService.findPeriodsByYear(1L);

        assertThat(result).hasSize(1);
    }
}
