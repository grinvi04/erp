package com.erp.finance.adapter.in.web;

import com.erp.common.response.ApiResponse;
import com.erp.finance.application.dto.FiscalPeriodCreateRequest;
import com.erp.finance.application.dto.FiscalPeriodResponse;
import com.erp.finance.application.dto.FiscalYearCreateRequest;
import com.erp.finance.application.dto.FiscalYearResponse;
import com.erp.finance.application.service.FiscalYearService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/finance/fiscal-years")
@RequiredArgsConstructor
public class FiscalYearController {

  private final FiscalYearService fiscalYearService;

  @GetMapping
  public ResponseEntity<ApiResponse<List<FiscalYearResponse>>> findAll() {
    return ResponseEntity.ok(ApiResponse.ok(fiscalYearService.findAll()));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<FiscalYearResponse>> findById(@PathVariable Long id) {
    return ResponseEntity.ok(ApiResponse.ok(fiscalYearService.findById(id)));
  }

  @PostMapping
  public ResponseEntity<ApiResponse<FiscalYearResponse>> create(
      @Valid @RequestBody FiscalYearCreateRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.ok(fiscalYearService.create(request)));
  }

  @PostMapping("/{id}/close")
  public ResponseEntity<ApiResponse<FiscalYearResponse>> close(@PathVariable Long id) {
    return ResponseEntity.ok(ApiResponse.ok(fiscalYearService.close(id)));
  }

  @GetMapping("/{fiscalYearId}/periods")
  public ResponseEntity<ApiResponse<List<FiscalPeriodResponse>>> findPeriods(
      @PathVariable Long fiscalYearId) {
    return ResponseEntity.ok(ApiResponse.ok(fiscalYearService.findPeriodsByYear(fiscalYearId)));
  }

  @PostMapping("/{fiscalYearId}/periods")
  public ResponseEntity<ApiResponse<FiscalPeriodResponse>> createPeriod(
      @PathVariable Long fiscalYearId, @Valid @RequestBody FiscalPeriodCreateRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.ok(fiscalYearService.createPeriod(fiscalYearId, request)));
  }

  @PostMapping("/periods/{periodId}/close")
  public ResponseEntity<ApiResponse<FiscalPeriodResponse>> closePeriod(
      @PathVariable Long periodId) {
    return ResponseEntity.ok(ApiResponse.ok(fiscalYearService.closePeriod(periodId)));
  }
}
