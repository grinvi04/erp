package com.erp.finance.application.service;

import com.erp.common.security.Permission;
import com.erp.common.security.PermissionChecker;
import com.erp.finance.application.dto.MonthlyInvoiceByCurrencyResponse;
import com.erp.finance.application.dto.MonthlyInvoiceResponse;
import com.erp.finance.domain.repository.ApInvoiceRepository;
import com.erp.finance.domain.repository.MonthlyInvoiceRow;
import java.math.BigDecimal;
import java.time.Year;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FinanceAnalyticsService {

    private static final int MONTHS_IN_YEAR = 12;

    private final ApInvoiceRepository apInvoiceRepository;
    private final PermissionChecker permissionChecker;

    public List<MonthlyInvoiceByCurrencyResponse> getMonthlyInvoices(Integer year) {
        permissionChecker.require(Permission.FINANCE_READ);
        int targetYear = year != null ? year : Year.now().getValue();

        // 통화별로 행을 모은다. 쿼리가 통화순으로 정렬되어 LinkedHashMap이 통화 정렬을 보존한다.
        Map<String, Map<Integer, MonthlyInvoiceRow>> byCurrency = new LinkedHashMap<>();
        for (MonthlyInvoiceRow row : apInvoiceRepository.monthlyInvoices(targetYear)) {
            byCurrency
                    .computeIfAbsent(row.getCurrency(), c -> new LinkedHashMap<>())
                    .put(row.getMonth(), row);
        }

        List<MonthlyInvoiceByCurrencyResponse> result = new ArrayList<>(byCurrency.size());
        for (Map.Entry<String, Map<Integer, MonthlyInvoiceRow>> entry : byCurrency.entrySet()) {
            Map<Integer, MonthlyInvoiceRow> rowMap = entry.getValue();
            List<MonthlyInvoiceResponse> months = new ArrayList<>(MONTHS_IN_YEAR);
            for (int month = 1; month <= MONTHS_IN_YEAR; month++) {
                MonthlyInvoiceRow row = rowMap.get(month);
                if (row != null) {
                    months.add(new MonthlyInvoiceResponse(month, row.getCount(), row.getTotalAmount()));
                } else {
                    months.add(new MonthlyInvoiceResponse(month, 0L, BigDecimal.ZERO));
                }
            }
            result.add(new MonthlyInvoiceByCurrencyResponse(entry.getKey(), months));
        }
        return result;
    }
}
