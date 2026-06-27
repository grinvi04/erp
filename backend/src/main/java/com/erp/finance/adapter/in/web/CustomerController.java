package com.erp.finance.adapter.in.web;

import com.erp.common.response.ApiResponse;
import com.erp.common.response.PageResponse;
import com.erp.finance.application.dto.CustomerCreateRequest;
import com.erp.finance.application.dto.CustomerResponse;
import com.erp.finance.application.dto.CustomerUpdateRequest;
import com.erp.finance.application.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/finance/customers")
@RequiredArgsConstructor
public class CustomerController {

  private final CustomerService customerService;

  @GetMapping
  public ResponseEntity<ApiResponse<PageResponse<CustomerResponse>>> findAll(
      @RequestParam(required = false) String keyword,
      @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
    return ResponseEntity.ok(ApiResponse.ok(customerService.findAll(keyword, pageable)));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<CustomerResponse>> findById(@PathVariable Long id) {
    return ResponseEntity.ok(ApiResponse.ok(customerService.findById(id)));
  }

  @PostMapping
  public ResponseEntity<ApiResponse<CustomerResponse>> create(
      @Valid @RequestBody CustomerCreateRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.ok(customerService.create(request)));
  }

  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<CustomerResponse>> update(
      @PathVariable Long id, @Valid @RequestBody CustomerUpdateRequest request) {
    return ResponseEntity.ok(ApiResponse.ok(customerService.update(id, request)));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deactivate(@PathVariable Long id) {
    customerService.deactivate(id);
    return ResponseEntity.noContent().build();
  }
}
