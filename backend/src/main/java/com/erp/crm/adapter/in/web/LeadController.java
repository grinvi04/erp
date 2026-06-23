package com.erp.crm.adapter.in.web;

import com.erp.common.response.ApiResponse;
import com.erp.common.response.PageResponse;
import com.erp.crm.application.dto.LeadConvertRequest;
import com.erp.crm.application.dto.LeadCreateRequest;
import com.erp.crm.application.dto.LeadResponse;
import com.erp.crm.application.dto.LeadUpdateRequest;
import com.erp.crm.application.service.LeadService;
import com.erp.crm.domain.model.LeadStatus;
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
@RequestMapping("/api/crm/leads")
@RequiredArgsConstructor
public class LeadController {

    private final LeadService leadService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<LeadResponse>>> search(
            @RequestParam(required = false) LeadStatus status,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(leadService.search(status, keyword, pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LeadResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(leadService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<LeadResponse>> create(
            @Valid @RequestBody LeadCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(leadService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<LeadResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody LeadUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(leadService.update(id, request)));
    }

    @PostMapping("/{id}/convert")
    public ResponseEntity<ApiResponse<LeadResponse>> convert(
            @PathVariable Long id,
            @Valid @RequestBody LeadConvertRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(leadService.convert(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        leadService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
