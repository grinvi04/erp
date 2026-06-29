package com.erp.finance.adapter.in.web;

import com.erp.common.response.ApiResponse;
import com.erp.finance.application.dto.BaseCurrencyResponse;
import com.erp.finance.application.dto.BaseCurrencyUpdateRequest;
import com.erp.finance.application.dto.ExchangeRateCreateRequest;
import com.erp.finance.application.dto.ExchangeRateResponse;
import com.erp.finance.application.dto.FxGainLossAccountResponse;
import com.erp.finance.application.dto.FxGainLossAccountUpdateRequest;
import com.erp.finance.application.dto.FxOverviewResponse;
import com.erp.finance.application.dto.VatAccountResponse;
import com.erp.finance.application.dto.VatAccountUpdateRequest;
import com.erp.finance.application.service.BaseCurrencyService;
import com.erp.finance.application.service.ExchangeRateService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** FX 기반 — 테넌트 기준통화 설정 + 환율(수동 등록·목록). 변경은 FINANCE_SETTING_WRITE(서비스에서 검사). */
@RestController
@RequestMapping("/api/finance/fx")
@RequiredArgsConstructor
public class FxController {

  private final BaseCurrencyService baseCurrencyService;
  private final ExchangeRateService exchangeRateService;

  /** FX 설정 한눈 조회 — 기준통화·환율·환차계정 통합 반환(데이터 없어도 기본값으로 200). */
  @GetMapping
  public ResponseEntity<ApiResponse<FxOverviewResponse>> getOverview() {
    return ResponseEntity.ok(
        ApiResponse.ok(
            new FxOverviewResponse(
                baseCurrencyService.getBaseCurrency(),
                exchangeRateService.findAll(),
                baseCurrencyService.getFxGainLossAccounts())));
  }

  @GetMapping("/base-currency")
  public ResponseEntity<ApiResponse<BaseCurrencyResponse>> getBaseCurrency() {
    return ResponseEntity.ok(ApiResponse.ok(baseCurrencyService.getBaseCurrency()));
  }

  @PutMapping("/base-currency")
  public ResponseEntity<ApiResponse<BaseCurrencyResponse>> updateBaseCurrency(
      @Valid @RequestBody BaseCurrencyUpdateRequest request) {
    return ResponseEntity.ok(ApiResponse.ok(baseCurrencyService.updateBaseCurrency(request)));
  }

  @GetMapping("/gain-loss-accounts")
  public ResponseEntity<ApiResponse<FxGainLossAccountResponse>> getFxGainLossAccounts() {
    return ResponseEntity.ok(ApiResponse.ok(baseCurrencyService.getFxGainLossAccounts()));
  }

  @PutMapping("/gain-loss-accounts")
  public ResponseEntity<ApiResponse<FxGainLossAccountResponse>> updateFxGainLossAccounts(
      @Valid @RequestBody FxGainLossAccountUpdateRequest request) {
    return ResponseEntity.ok(ApiResponse.ok(baseCurrencyService.updateFxGainLossAccounts(request)));
  }

  @GetMapping("/vat-accounts")
  public ResponseEntity<ApiResponse<VatAccountResponse>> getVatAccounts() {
    return ResponseEntity.ok(ApiResponse.ok(baseCurrencyService.getVatAccounts()));
  }

  @PutMapping("/vat-accounts")
  public ResponseEntity<ApiResponse<VatAccountResponse>> updateVatAccounts(
      @Valid @RequestBody VatAccountUpdateRequest request) {
    return ResponseEntity.ok(ApiResponse.ok(baseCurrencyService.updateVatAccounts(request)));
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
