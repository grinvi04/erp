package com.erp.finance.adapter.in.web;

import com.erp.common.response.ApiResponse;
import com.erp.common.response.PageResponse;
import com.erp.finance.application.dto.TaxInvoiceIssueRequest;
import com.erp.finance.application.dto.TaxInvoiceResponse;
import com.erp.finance.application.service.TaxInvoiceService;
import com.erp.finance.domain.model.TaxInvoiceStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 전자세금계산서 — 목록·상세·발행(AR에서 파생)·취소·국세청 표준 XML 다운로드. 발행·취소는 FINANCE_WRITE, 조회·XML은 FINANCE_READ(서비스에서
 * 검사).
 */
@RestController
@RequiredArgsConstructor
public class TaxInvoiceController {

  private final TaxInvoiceService taxInvoiceService;

  @GetMapping("/api/finance/tax-invoices")
  public ResponseEntity<ApiResponse<PageResponse<TaxInvoiceResponse>>> findAll(
      @RequestParam(required = false) TaxInvoiceStatus status,
      @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
    return ResponseEntity.ok(ApiResponse.ok(taxInvoiceService.findAll(status, pageable)));
  }

  @GetMapping("/api/finance/tax-invoices/{id}")
  public ResponseEntity<ApiResponse<TaxInvoiceResponse>> findById(@PathVariable Long id) {
    return ResponseEntity.ok(ApiResponse.ok(taxInvoiceService.findById(id)));
  }

  /** AR 인보이스에서 세금계산서 발행. */
  @PostMapping("/api/finance/ar-invoices/{arInvoiceId}/tax-invoice")
  public ResponseEntity<ApiResponse<TaxInvoiceResponse>> issue(
      @PathVariable Long arInvoiceId, @Valid @RequestBody TaxInvoiceIssueRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.ok(taxInvoiceService.issue(arInvoiceId, request)));
  }

  @PostMapping("/api/finance/tax-invoices/{id}/cancel")
  public ResponseEntity<ApiResponse<TaxInvoiceResponse>> cancel(@PathVariable Long id) {
    return ResponseEntity.ok(ApiResponse.ok(taxInvoiceService.cancel(id)));
  }

  /** 국세청 표준 XML 다운로드(application/xml). */
  @GetMapping("/api/finance/tax-invoices/{id}/xml")
  public ResponseEntity<String> downloadXml(@PathVariable Long id) {
    String xml = taxInvoiceService.generateXml(id);
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_XML)
        .header(
            HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"tax-invoice-" + id + ".xml\"")
        .body(xml);
  }
}
