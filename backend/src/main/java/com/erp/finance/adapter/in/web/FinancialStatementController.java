package com.erp.finance.adapter.in.web;

import com.erp.common.response.ApiResponse;
import com.erp.finance.application.dto.BalanceSheetResponse;
import com.erp.finance.application.dto.IncomeStatementResponse;
import com.erp.finance.application.dto.TrialBalanceResponse;
import com.erp.finance.application.service.BalanceSheetService;
import com.erp.finance.application.service.IncomeStatementService;
import com.erp.finance.application.service.TrialBalanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 재무제표 조회 — 시산표·손익계산서·재무상태표(모두 FINANCE_READ, 읽기 전용). 회계연도(year) 미지정 시 현재 연도를 기준으로 한다. */
@RestController
@RequestMapping("/api/finance/reports")
@RequiredArgsConstructor
public class FinancialStatementController {

  private final TrialBalanceService trialBalanceService;
  private final IncomeStatementService incomeStatementService;
  private final BalanceSheetService balanceSheetService;

  @GetMapping("/trial-balance")
  public ResponseEntity<ApiResponse<TrialBalanceResponse>> trialBalance(
      @RequestParam(required = false) Integer year) {
    return ResponseEntity.ok(ApiResponse.ok(trialBalanceService.getTrialBalance(year)));
  }

  @GetMapping("/income-statement")
  public ResponseEntity<ApiResponse<IncomeStatementResponse>> incomeStatement(
      @RequestParam(required = false) Integer year) {
    return ResponseEntity.ok(ApiResponse.ok(incomeStatementService.getIncomeStatement(year)));
  }

  @GetMapping("/balance-sheet")
  public ResponseEntity<ApiResponse<BalanceSheetResponse>> balanceSheet(
      @RequestParam(required = false) Integer year) {
    return ResponseEntity.ok(ApiResponse.ok(balanceSheetService.getBalanceSheet(year)));
  }
}
