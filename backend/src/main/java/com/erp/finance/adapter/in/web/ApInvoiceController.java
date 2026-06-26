package com.erp.finance.adapter.in.web;

import com.erp.common.response.ApiResponse;
import com.erp.common.response.PageResponse;
import com.erp.finance.application.dto.ApInvoiceCreateRequest;
import com.erp.finance.application.dto.ApInvoicePayRequest;
import com.erp.finance.application.dto.ApInvoiceResponse;
import com.erp.finance.application.service.ApInvoiceService;
import com.erp.finance.domain.model.ApInvoiceStatus;
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
@RequestMapping("/api/finance/invoices")
@RequiredArgsConstructor
public class ApInvoiceController {

    private final ApInvoiceService apInvoiceService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ApInvoiceResponse>>> findAll(
        @RequestParam(required = false) ApInvoiceStatus status,
        @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(apInvoiceService.findAll(status, pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ApInvoiceResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(apInvoiceService.findById(id)));
    }

    @GetMapping("/vendor/{vendorId}")
    public ResponseEntity<ApiResponse<PageResponse<ApInvoiceResponse>>> findByVendor(
        @PathVariable Long vendorId,
        @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(apInvoiceService.findByVendor(vendorId, pageable)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ApInvoiceResponse>> create(
        @Valid @RequestBody ApInvoiceCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(apInvoiceService.create(request)));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<ApiResponse<ApInvoiceResponse>> submit(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(apInvoiceService.submit(id)));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<ApInvoiceResponse>> approve(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(apInvoiceService.approve(id)));
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<ApiResponse<ApInvoiceResponse>> pay(
        @PathVariable Long id,
        @Valid @RequestBody ApInvoicePayRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(apInvoiceService.pay(id, request)));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<ApInvoiceResponse>> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(apInvoiceService.cancel(id)));
    }
}
