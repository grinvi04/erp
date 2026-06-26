package com.erp.inventory.adapter.in.web;

import com.erp.common.response.ApiResponse;
import com.erp.inventory.application.dto.ItemCategoryCreateRequest;
import com.erp.inventory.application.dto.ItemCategoryResponse;
import com.erp.inventory.application.dto.ItemCategoryUpdateRequest;
import com.erp.inventory.application.service.ItemCategoryService;
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
@RequestMapping("/api/inventory/item-categories")
@RequiredArgsConstructor
public class ItemCategoryController {

    private final ItemCategoryService itemCategoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ItemCategoryResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.ok(itemCategoryService.findAll()));
    }

    @GetMapping("/roots")
    public ResponseEntity<ApiResponse<List<ItemCategoryResponse>>> findRoots() {
        return ResponseEntity.ok(ApiResponse.ok(itemCategoryService.findRoots()));
    }

    @GetMapping("/{id}/children")
    public ResponseEntity<ApiResponse<List<ItemCategoryResponse>>> findChildren(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(itemCategoryService.findChildren(id)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ItemCategoryResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(itemCategoryService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ItemCategoryResponse>> create(
            @Valid @RequestBody ItemCategoryCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(itemCategoryService.create(req)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ItemCategoryResponse>> update(@PathVariable Long id,
            @Valid @RequestBody ItemCategoryUpdateRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(itemCategoryService.update(id, req)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        itemCategoryService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
