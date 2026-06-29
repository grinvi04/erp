package com.erp.finance.adapter.in.web;

import com.erp.common.response.ApiResponse;
import com.erp.finance.application.dto.VatReturnResponse;
import com.erp.finance.application.service.VatReturnService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 부가세 신고 기초자료 — 신고기간(from~to) 매출·매입 집계·납부세액·합계표. FINANCE_READ(서비스에서 검사). */
@RestController
@RequestMapping("/api/finance/vat-return")
@RequiredArgsConstructor
public class VatReturnController {

  private final VatReturnService vatReturnService;

  @GetMapping
  public ResponseEntity<ApiResponse<VatReturnResponse>> getVatReturn(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return ResponseEntity.ok(ApiResponse.ok(vatReturnService.getVatReturn(from, to)));
  }
}
