package com.erp.finance.adapter.in.web;

import com.erp.common.response.ApiResponse;
import com.erp.finance.application.dto.BaseCurrencyResponse;
import com.erp.finance.application.dto.BaseCurrencyUpdateRequest;
import com.erp.finance.application.dto.ExchangeRateCreateRequest;
import com.erp.finance.application.dto.ExchangeRateResponse;
import com.erp.finance.application.service.BaseCurrencyService;
import com.erp.finance.application.service.ExchangeRateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * FX 기반 — 테넌트 기준통화 설정 + 환율(수동 등록·목록). 변경은 FINANCE_SETTING_WRITE(서비스에서 검사).
 */
@RestController
@RequestMapping("/api/finance/fx")
@RequiredArgsConstructor
public class FxController {

    private final BaseCurrencyService baseCurrencyService;
    private final ExchangeRateService exchangeRateService;

    @GetMapping("/base-currency")
    public ResponseEntity<ApiResponse<BaseCurrencyResponse>> getBaseCurrency() {
        return ResponseEntity.ok(ApiResponse.ok(baseCurrencyService.getBaseCurrency()));
    }

    @PutMapping("/base-currency")
    public ResponseEntity<ApiResponse<BaseCurrencyResponse>> updateBaseCurrency(
        @Valid @RequestBody BaseCurrencyUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(baseCurrencyService.updateBaseCurrency(request)));
    }

    @GetMapping("/rates")
    public ResponseEntity<ApiResponse<List<ExchangeRateResponse>>> findRates() {
        return ResponseEntity.ok(ApiResponse.ok(exchangeRateService.findAll()));
    }

    @PostMapping("/rates")
    public ResponseEntity<ApiResponse<ExchangeRateResponse>> createRate(
        @Valid @RequestBody ExchangeRateCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok(exchangeRateService.register(request)));
    }
}
