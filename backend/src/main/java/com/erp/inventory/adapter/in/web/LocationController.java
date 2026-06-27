package com.erp.inventory.adapter.in.web;

import com.erp.common.response.ApiResponse;
import com.erp.inventory.application.dto.LocationCreateRequest;
import com.erp.inventory.application.dto.LocationResponse;
import com.erp.inventory.application.dto.LocationUpdateRequest;
import com.erp.inventory.application.service.LocationService;
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
@RequestMapping("/api/inventory/locations")
@RequiredArgsConstructor
public class LocationController {

  private final LocationService locationService;

  @GetMapping
  public ResponseEntity<ApiResponse<List<LocationResponse>>> findByWarehouse(
      @RequestParam Long warehouseId) {
    return ResponseEntity.ok(ApiResponse.ok(locationService.findByWarehouse(warehouseId)));
  }

  @GetMapping("/{id}/children")
  public ResponseEntity<ApiResponse<List<LocationResponse>>> findChildren(@PathVariable Long id) {
    return ResponseEntity.ok(ApiResponse.ok(locationService.findChildren(id)));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<LocationResponse>> findById(@PathVariable Long id) {
    return ResponseEntity.ok(ApiResponse.ok(locationService.findById(id)));
  }

  @PostMapping
  public ResponseEntity<ApiResponse<LocationResponse>> create(
      @Valid @RequestBody LocationCreateRequest req) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.ok(locationService.create(req)));
  }

  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<LocationResponse>> update(
      @PathVariable Long id, @Valid @RequestBody LocationUpdateRequest req) {
    return ResponseEntity.ok(ApiResponse.ok(locationService.update(id, req)));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable Long id) {
    locationService.deactivate(id);
    return ResponseEntity.ok(ApiResponse.ok());
  }
}
