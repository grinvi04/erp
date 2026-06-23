package com.erp.crm.adapter.in.web;

import com.erp.common.response.ApiResponse;
import com.erp.common.response.PageResponse;
import com.erp.crm.application.dto.OpportunityCreateRequest;
import com.erp.crm.application.dto.OpportunityResponse;
import com.erp.crm.application.dto.OpportunityUpdateRequest;
import com.erp.crm.application.service.OpportunityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
@RequestMapping("/api/crm/opportunities")
@RequiredArgsConstructor
public class OpportunityController {

    private final OpportunityService opportunityService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<OpportunityResponse>>> search(
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) Long stageId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(
                opportunityService.search(accountId, stageId, pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OpportunityResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(opportunityService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OpportunityResponse>> create(
            @Valid @RequestBody OpportunityCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(opportunityService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<OpportunityResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody OpportunityUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(opportunityService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        opportunityService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
