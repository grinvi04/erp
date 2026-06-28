package com.erp.finance.adapter.in.web;

import com.erp.common.response.ApiResponse;
import com.erp.finance.application.dto.AccountCreateRequest;
import com.erp.finance.application.dto.AccountResponse;
import com.erp.finance.application.dto.AccountUpdateRequest;
import com.erp.finance.application.service.AccountService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/api/finance/accounts")
@RequiredArgsConstructor
public class AccountController {

  private final AccountService accountService;

  @GetMapping
  public ResponseEntity<ApiResponse<List<AccountResponse>>> findAll() {
    return ResponseEntity.ok(ApiResponse.ok(accountService.findAll()));
  }

  @GetMapping("/roots")
  public ResponseEntity<ApiResponse<List<AccountResponse>>> findRoots() {
    return ResponseEntity.ok(ApiResponse.ok(accountService.findRoots()));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<AccountResponse>> findById(@PathVariable Long id) {
    return ResponseEntity.ok(ApiResponse.ok(accountService.findById(id)));
  }

  @GetMapping("/{id}/children")
  public ResponseEntity<ApiResponse<List<AccountResponse>>> findChildren(@PathVariable Long id) {
    return ResponseEntity.ok(ApiResponse.ok(accountService.findByParent(id)));
  }

  @PostMapping
  public ResponseEntity<ApiResponse<AccountResponse>> create(
      @Valid @RequestBody AccountCreateRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.ok(accountService.create(request)));
  }

  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<AccountResponse>> update(
      @PathVariable Long id, @Valid @RequestBody AccountUpdateRequest request) {
    return ResponseEntity.ok(ApiResponse.ok(accountService.update(id, request)));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deactivate(@PathVariable Long id) {
    accountService.deactivate(id);
    return ResponseEntity.noContent().build();
  }
}
