package com.erp.inventory.adapter.in.web;

import com.erp.common.response.ApiResponse;
import com.erp.inventory.application.dto.CategoryItemCountResponse;
import com.erp.inventory.application.dto.LowStockItemResponse;
import com.erp.inventory.application.dto.MonthlyMovementByTypeResponse;
import com.erp.inventory.application.dto.MovementTypeCountResponse;
import com.erp.inventory.application.dto.WarehouseStockResponse;
import com.erp.inventory.application.service.InventoryAnalyticsService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory/analytics")
@RequiredArgsConstructor
public class InventoryAnalyticsController {

  private final InventoryAnalyticsService inventoryAnalyticsService;

  @GetMapping("/by-category")
  public ResponseEntity<ApiResponse<List<CategoryItemCountResponse>>> getActiveItemsByCategory() {
    return ResponseEntity.ok(ApiResponse.ok(inventoryAnalyticsService.getActiveItemsByCategory()));
  }

  @GetMapping("/by-warehouse")
  public ResponseEntity<ApiResponse<List<WarehouseStockResponse>>> getStockByWarehouse() {
    return ResponseEntity.ok(ApiResponse.ok(inventoryAnalyticsService.getStockByWarehouse()));
  }

  @GetMapping("/movements-by-type")
  public ResponseEntity<ApiResponse<List<MovementTypeCountResponse>>> getMovementsByType() {
    return ResponseEntity.ok(ApiResponse.ok(inventoryAnalyticsService.getMovementsByType()));
  }

  @GetMapping("/monthly-movements")
  public ResponseEntity<ApiResponse<List<MonthlyMovementByTypeResponse>>> getMonthlyMovements(
      @RequestParam(required = false) Integer year) {
    return ResponseEntity.ok(ApiResponse.ok(inventoryAnalyticsService.getMonthlyMovements(year)));
  }

  @GetMapping("/low-stock")
  public ResponseEntity<ApiResponse<List<LowStockItemResponse>>> getLowStockItems() {
    return ResponseEntity.ok(ApiResponse.ok(inventoryAnalyticsService.getLowStockItems()));
  }
}
