package com.erp.finance.adapter.in.web;

import com.erp.common.response.ApiResponse;
import com.erp.common.response.PageResponse;
import com.erp.finance.application.dto.VendorCreateRequest;
import com.erp.finance.application.dto.VendorResponse;
import com.erp.finance.application.dto.VendorUpdateRequest;
import com.erp.finance.application.service.VendorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/finance/vendors")
@RequiredArgsConstructor
public class VendorController {

    private final VendorService vendorService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<VendorResponse>>> findAll(
        @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(vendorService.findAll(pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VendorResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(vendorService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<VendorResponse>> create(
        @Valid @RequestBody VendorCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(vendorService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<VendorResponse>> update(
        @PathVariable Long id,
        @Valid @RequestBody VendorUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(vendorService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        vendorService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
