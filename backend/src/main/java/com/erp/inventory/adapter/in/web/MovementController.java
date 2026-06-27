package com.erp.inventory.adapter.in.web;

import com.erp.common.response.ApiResponse;
import com.erp.common.response.PageResponse;
import com.erp.inventory.application.dto.MovementCreateRequest;
import com.erp.inventory.application.dto.MovementRejectRequest;
import com.erp.inventory.application.dto.MovementResponse;
import com.erp.inventory.application.service.MovementService;
import com.erp.inventory.domain.model.MovementStatus;
import com.erp.inventory.domain.model.MovementType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory/movements")
@RequiredArgsConstructor
public class MovementController {

    private final MovementService movementService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<MovementResponse>>> findAll(
            @RequestParam(required = false) MovementType type,
            @RequestParam(required = false) MovementStatus status,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(movementService.findAll(type, status, pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MovementResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(movementService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MovementResponse>> create(
            @Valid @RequestBody MovementCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(movementService.create(req)));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<ApiResponse<MovementResponse>> confirm(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(movementService.confirm(id)));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<ApiResponse<MovementResponse>> submit(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(movementService.submitForApproval(id)));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<MovementResponse>> approve(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(movementService.approve(id)));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<MovementResponse>> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(movementService.cancel(id)));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<MovementResponse>> reject(
            @PathVariable Long id, @Valid @RequestBody MovementRejectRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(movementService.reject(id, request.comment())));
    }

    @PostMapping("/{id}/withdraw")
    public ResponseEntity<ApiResponse<MovementResponse>> withdraw(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(movementService.withdraw(id)));
    }
}
