package com.erp.finance.adapter.in.web;

import com.erp.common.response.ApiResponse;
import com.erp.finance.application.dto.MonthlyInvoiceResponse;
import com.erp.finance.application.service.FinanceAnalyticsService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/finance/analytics")
@RequiredArgsConstructor
public class FinanceAnalyticsController {

    private final FinanceAnalyticsService financeAnalyticsService;

    @GetMapping("/monthly-invoices")
    public ResponseEntity<ApiResponse<List<MonthlyInvoiceResponse>>> getMonthlyInvoices(
            @RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(ApiResponse.ok(financeAnalyticsService.getMonthlyInvoices(year)));
    }
}
