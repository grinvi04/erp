package com.erp.finance.adapter.in.web;

import com.erp.common.response.ApiResponse;
import com.erp.common.response.PageResponse;
import com.erp.finance.application.dto.ArInvoiceCreateRequest;
import com.erp.finance.application.dto.ArInvoicePayRequest;
import com.erp.finance.application.dto.ArInvoiceResponse;
import com.erp.finance.application.service.ArInvoiceService;
import com.erp.finance.domain.model.ArInvoiceStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/finance/ar-invoices")
@RequiredArgsConstructor
public class ArInvoiceController {

  private final ArInvoiceService arInvoiceService;

  @GetMapping
  public ResponseEntity<ApiResponse<PageResponse<ArInvoiceResponse>>> findAll(
      @RequestParam(required = false) ArInvoiceStatus status,
      @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
    return ResponseEntity.ok(ApiResponse.ok(arInvoiceService.findAll(status, pageable)));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<ArInvoiceResponse>> findById(@PathVariable Long id) {
    return ResponseEntity.ok(ApiResponse.ok(arInvoiceService.findById(id)));
  }

  @GetMapping("/customer/{customerId}")
  public ResponseEntity<ApiResponse<PageResponse<ArInvoiceResponse>>> findByCustomer(
      @PathVariable Long customerId,
      @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
    return ResponseEntity.ok(ApiResponse.ok(arInvoiceService.findByCustomer(customerId, pageable)));
  }

  @PostMapping
  public ResponseEntity<ApiResponse<ArInvoiceResponse>> create(
      @Valid @RequestBody ArInvoiceCreateRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.ok(arInvoiceService.create(request)));
  }

  @PostMapping("/{id}/submit")
  public ResponseEntity<ApiResponse<ArInvoiceResponse>> submit(@PathVariable Long id) {
    return ResponseEntity.ok(ApiResponse.ok(arInvoiceService.submit(id)));
  }

  @PostMapping("/{id}/approve")
  public ResponseEntity<ApiResponse<ArInvoiceResponse>> approve(@PathVariable Long id) {
    return ResponseEntity.ok(ApiResponse.ok(arInvoiceService.approve(id)));
  }

  @PostMapping("/{id}/pay")
  public ResponseEntity<ApiResponse<ArInvoiceResponse>> pay(
      @PathVariable Long id, @Valid @RequestBody ArInvoicePayRequest request) {
    return ResponseEntity.ok(ApiResponse.ok(arInvoiceService.pay(id, request)));
  }

  @PostMapping("/{id}/cancel")
  public ResponseEntity<ApiResponse<ArInvoiceResponse>> cancel(@PathVariable Long id) {
    return ResponseEntity.ok(ApiResponse.ok(arInvoiceService.cancel(id)));
  }
}
