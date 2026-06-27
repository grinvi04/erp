package com.erp.crm.adapter.in.web;

import com.erp.common.response.ApiResponse;
import com.erp.crm.application.dto.PipelineStageCreateRequest;
import com.erp.crm.application.dto.PipelineStageResponse;
import com.erp.crm.application.dto.PipelineStageUpdateRequest;
import com.erp.crm.application.service.PipelineStageService;
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
@RequestMapping("/api/crm/pipeline-stages")
@RequiredArgsConstructor
public class PipelineStageController {

    private final PipelineStageService stageService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PipelineStageResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.ok(stageService.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PipelineStageResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(stageService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PipelineStageResponse>> create(
            @Valid @RequestBody PipelineStageCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(stageService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PipelineStageResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody PipelineStageUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(stageService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        stageService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
