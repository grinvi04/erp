package com.erp.inventory.adapter.in.web;

import com.erp.common.response.ApiResponse;
import com.erp.common.response.PageResponse;
import com.erp.inventory.application.dto.StockResponse;
import com.erp.inventory.application.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory/stocks")
@RequiredArgsConstructor
public class StockController {

  private final StockService stockService;

  @GetMapping("/by-item")
  public ResponseEntity<ApiResponse<PageResponse<StockResponse>>> findByItem(
      @RequestParam Long itemId, Pageable pageable) {
    return ResponseEntity.ok(ApiResponse.ok(stockService.findByItem(itemId, pageable)));
  }

  @GetMapping("/by-warehouse")
  public ResponseEntity<ApiResponse<PageResponse<StockResponse>>> findByWarehouse(
      @RequestParam Long warehouseId, Pageable pageable) {
    return ResponseEntity.ok(ApiResponse.ok(stockService.findByWarehouse(warehouseId, pageable)));
  }
}
