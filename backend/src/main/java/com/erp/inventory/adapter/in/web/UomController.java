package com.erp.inventory.adapter.in.web;

import com.erp.common.response.ApiResponse;
import com.erp.inventory.application.dto.UomCreateRequest;
import com.erp.inventory.application.dto.UomResponse;
import com.erp.inventory.application.dto.UomUpdateRequest;
import com.erp.inventory.application.service.UomService;
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
@RequestMapping("/api/inventory/uoms")
@RequiredArgsConstructor
public class UomController {

    private final UomService uomService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<UomResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.ok(uomService.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UomResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(uomService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UomResponse>> create(@Valid @RequestBody UomCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(uomService.create(req)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UomResponse>> update(@PathVariable Long id,
            @Valid @RequestBody UomUpdateRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(uomService.update(id, req)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        uomService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
