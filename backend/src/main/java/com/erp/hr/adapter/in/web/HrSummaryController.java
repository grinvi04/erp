package com.erp.hr.adapter.in.web;

import com.erp.common.response.ApiResponse;
import com.erp.hr.application.dto.HrSummaryResponse;
import com.erp.hr.application.service.HrSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hr/summary")
@RequiredArgsConstructor
public class HrSummaryController {

  private final HrSummaryService hrSummaryService;

  @GetMapping
  public ResponseEntity<ApiResponse<HrSummaryResponse>> getSummary() {
    return ResponseEntity.ok(ApiResponse.ok(hrSummaryService.getSummary()));
  }
}
