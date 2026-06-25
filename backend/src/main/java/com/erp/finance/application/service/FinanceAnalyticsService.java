package com.erp.finance.application.service;

import com.erp.finance.application.dto.MonthlyInvoiceResponse;
import com.erp.finance.domain.repository.ApInvoiceRepository;
import com.erp.finance.domain.repository.MonthlyInvoiceRow;
import java.math.BigDecimal;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FinanceAnalyticsService {

    private static final int MONTHS_IN_YEAR = 12;

    private final ApInvoiceRepository apInvoiceRepository;

    public List<MonthlyInvoiceResponse> getMonthlyInvoices(Integer year) {
        int targetYear = year != null ? year : Year.now().getValue();

        Map<Integer, MonthlyInvoiceRow> rowMap = apInvoiceRepository.monthlyInvoices(targetYear)
                .stream()
                .collect(Collectors.toMap(MonthlyInvoiceRow::getMonth, r -> r));

        List<MonthlyInvoiceResponse> result = new ArrayList<>(MONTHS_IN_YEAR);
        for (int month = 1; month <= MONTHS_IN_YEAR; month++) {
            MonthlyInvoiceRow row = rowMap.get(month);
            if (row != null) {
                result.add(new MonthlyInvoiceResponse(month, row.getCount(), row.getTotalAmount()));
            } else {
                result.add(new MonthlyInvoiceResponse(month, 0L, BigDecimal.ZERO));
            }
        }
        return result;
    }
}
