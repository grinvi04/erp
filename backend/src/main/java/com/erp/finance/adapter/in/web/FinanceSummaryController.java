package com.erp.finance.adapter.in.web;

import com.erp.common.response.ApiResponse;
import com.erp.finance.application.dto.FinanceSummaryResponse;
import com.erp.finance.application.service.FinanceSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/finance/summary")
@RequiredArgsConstructor
public class FinanceSummaryController {

    private final FinanceSummaryService financeSummaryService;

    @GetMapping
    public ResponseEntity<ApiResponse<FinanceSummaryResponse>> getSummary() {
        return ResponseEntity.ok(ApiResponse.ok(financeSummaryService.getSummary()));
    }
}
