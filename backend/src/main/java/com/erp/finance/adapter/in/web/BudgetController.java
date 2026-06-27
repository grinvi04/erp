package com.erp.finance.adapter.in.web;

import com.erp.common.response.ApiResponse;
import com.erp.finance.application.dto.BudgetCreateRequest;
import com.erp.finance.application.dto.BudgetResponse;
import com.erp.finance.application.dto.BudgetUpdateRequest;
import com.erp.finance.application.service.BudgetService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/finance/budgets")
@RequiredArgsConstructor
public class BudgetController {

  private final BudgetService budgetService;

  @GetMapping
  public ResponseEntity<ApiResponse<List<BudgetResponse>>> findByFiscalYear(
      @RequestParam Long fiscalYearId) {
    return ResponseEntity.ok(ApiResponse.ok(budgetService.findByFiscalYear(fiscalYearId)));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<BudgetResponse>> findById(@PathVariable Long id) {
    return ResponseEntity.ok(ApiResponse.ok(budgetService.findById(id)));
  }

  @PostMapping
  public ResponseEntity<ApiResponse<BudgetResponse>> create(
      @Valid @RequestBody BudgetCreateRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.ok(budgetService.create(request)));
  }

  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<BudgetResponse>> update(
      @PathVariable Long id, @Valid @RequestBody BudgetUpdateRequest request) {
    return ResponseEntity.ok(ApiResponse.ok(budgetService.update(id, request)));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    budgetService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
