package com.erp.inventory.adapter.in.web;

import com.erp.common.response.ApiResponse;
import com.erp.inventory.application.dto.InventorySummaryResponse;
import com.erp.inventory.application.service.InventorySummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory/summary")
@RequiredArgsConstructor
public class InventorySummaryController {

  private final InventorySummaryService inventorySummaryService;

  @GetMapping
  public ResponseEntity<ApiResponse<InventorySummaryResponse>> getSummary() {
    return ResponseEntity.ok(ApiResponse.ok(inventorySummaryService.getSummary()));
  }
}
