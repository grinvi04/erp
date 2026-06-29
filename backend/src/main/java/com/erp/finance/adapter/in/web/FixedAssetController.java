package com.erp.finance.adapter.in.web;

import com.erp.common.response.ApiResponse;
import com.erp.common.response.PageResponse;
import com.erp.finance.application.dto.DepreciationAccountResponse;
import com.erp.finance.application.dto.DepreciationAccountUpdateRequest;
import com.erp.finance.application.dto.DepreciationEntryResponse;
import com.erp.finance.application.dto.DepreciationRunRequest;
import com.erp.finance.application.dto.DepreciationRunResponse;
import com.erp.finance.application.dto.FixedAssetCreateRequest;
import com.erp.finance.application.dto.FixedAssetDisposeRequest;
import com.erp.finance.application.dto.FixedAssetResponse;
import com.erp.finance.application.dto.ImpairmentAccountResponse;
import com.erp.finance.application.dto.ImpairmentAccountUpdateRequest;
import com.erp.finance.application.dto.ImpairmentEntryResponse;
import com.erp.finance.application.dto.ImpairmentRecognizeRequest;
import com.erp.finance.application.dto.ImpairmentRecognizeResponse;
import com.erp.finance.application.service.BaseCurrencyService;
import com.erp.finance.application.service.DepreciationPostingService;
import com.erp.finance.application.service.FixedAssetService;
import com.erp.finance.application.service.ImpairmentPostingService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 고정자산 — 대장 CRUD·월 감가상각 실행·처분·상각 계정 설정. 권한은 서비스에서 검사(등록·상각·처분 FINANCE_WRITE, 설정
 * FINANCE_SETTING_WRITE).
 */
@RestController
@RequestMapping("/api/finance/fixed-assets")
@RequiredArgsConstructor
public class FixedAssetController {

  private final FixedAssetService fixedAssetService;
  private final DepreciationPostingService depreciationPostingService;
  private final ImpairmentPostingService impairmentPostingService;
  private final BaseCurrencyService baseCurrencyService;

  @GetMapping
  public ResponseEntity<ApiResponse<PageResponse<FixedAssetResponse>>> findAll(
      @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
    return ResponseEntity.ok(ApiResponse.ok(fixedAssetService.findAll(pageable)));
  }

  @GetMapping("/depreciation-accounts")
  public ResponseEntity<ApiResponse<DepreciationAccountResponse>> getDepreciationAccounts() {
    return ResponseEntity.ok(ApiResponse.ok(baseCurrencyService.getDepreciationAccounts()));
  }

  @PutMapping("/depreciation-accounts")
  public ResponseEntity<ApiResponse<DepreciationAccountResponse>> updateDepreciationAccounts(
      @Valid @RequestBody DepreciationAccountUpdateRequest request) {
    return ResponseEntity.ok(
        ApiResponse.ok(baseCurrencyService.updateDepreciationAccounts(request)));
  }

  @PostMapping("/depreciation-run")
  public ResponseEntity<ApiResponse<DepreciationRunResponse>> runDepreciation(
      @Valid @RequestBody DepreciationRunRequest request) {
    return ResponseEntity.ok(
        ApiResponse.ok(depreciationPostingService.runForPeriod(request.fiscalPeriodId())));
  }

  @GetMapping("/impairment-accounts")
  public ResponseEntity<ApiResponse<ImpairmentAccountResponse>> getImpairmentAccounts() {
    return ResponseEntity.ok(ApiResponse.ok(baseCurrencyService.getImpairmentAccounts()));
  }

  @PutMapping("/impairment-accounts")
  public ResponseEntity<ApiResponse<ImpairmentAccountResponse>> updateImpairmentAccounts(
      @Valid @RequestBody ImpairmentAccountUpdateRequest request) {
    return ResponseEntity.ok(ApiResponse.ok(baseCurrencyService.updateImpairmentAccounts(request)));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<FixedAssetResponse>> findById(@PathVariable Long id) {
    return ResponseEntity.ok(ApiResponse.ok(fixedAssetService.findById(id)));
  }

  @GetMapping("/{id}/depreciation")
  public ResponseEntity<ApiResponse<List<DepreciationEntryResponse>>> findHistory(
      @PathVariable Long id) {
    return ResponseEntity.ok(ApiResponse.ok(fixedAssetService.findHistory(id)));
  }

  @GetMapping("/{id}/impairment")
  public ResponseEntity<ApiResponse<List<ImpairmentEntryResponse>>> findImpairmentHistory(
      @PathVariable Long id) {
    return ResponseEntity.ok(ApiResponse.ok(fixedAssetService.findImpairmentHistory(id)));
  }

  @PostMapping("/{id}/impairment")
  public ResponseEntity<ApiResponse<ImpairmentRecognizeResponse>> recognizeImpairment(
      @PathVariable Long id, @Valid @RequestBody ImpairmentRecognizeRequest request) {
    return ResponseEntity.ok(
        ApiResponse.ok(
            impairmentPostingService.recognizeImpairment(
                id, request.fiscalPeriodId(), request.recoverableAmount())));
  }

  @PostMapping
  public ResponseEntity<ApiResponse<FixedAssetResponse>> create(
      @Valid @RequestBody FixedAssetCreateRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.ok(fixedAssetService.create(request)));
  }

  @PostMapping("/{id}/dispose")
  public ResponseEntity<ApiResponse<FixedAssetResponse>> dispose(
      @PathVariable Long id, @Valid @RequestBody FixedAssetDisposeRequest request) {
    return ResponseEntity.ok(ApiResponse.ok(fixedAssetService.dispose(id, request)));
  }
}
