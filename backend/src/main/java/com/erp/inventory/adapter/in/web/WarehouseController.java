package com.erp.inventory.adapter.in.web;

import com.erp.common.response.ApiResponse;
import com.erp.inventory.application.dto.WarehouseCreateRequest;
import com.erp.inventory.application.dto.WarehouseResponse;
import com.erp.inventory.application.dto.WarehouseUpdateRequest;
import com.erp.inventory.application.service.WarehouseService;
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
@RequestMapping("/api/inventory/warehouses")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<WarehouseResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.ok(warehouseService.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WarehouseResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(warehouseService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<WarehouseResponse>> create(
            @Valid @RequestBody WarehouseCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(warehouseService.create(req)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<WarehouseResponse>> update(@PathVariable Long id,
            @Valid @RequestBody WarehouseUpdateRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(warehouseService.update(id, req)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable Long id) {
        warehouseService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
