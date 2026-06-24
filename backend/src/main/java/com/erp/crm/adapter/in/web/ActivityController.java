package com.erp.crm.adapter.in.web;

import com.erp.common.response.ApiResponse;
import com.erp.common.response.PageResponse;
import com.erp.crm.application.dto.ActivityCreateRequest;
import com.erp.crm.application.dto.ActivityResponse;
import com.erp.crm.application.service.ActivityService;
import com.erp.crm.domain.model.ActivityStatus;
import com.erp.crm.domain.model.ActivityType;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/crm/activities")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ActivityResponse>>> search(
            @RequestParam(required = false) Long opportunityId,
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) ActivityType activityType,
            @RequestParam(required = false) ActivityStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(
                activityService.search(opportunityId, accountId, activityType, status, pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ActivityResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(activityService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ActivityResponse>> create(
            @Valid @RequestBody ActivityCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(activityService.create(request)));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<ActivityResponse>> complete(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(activityService.complete(id)));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<ActivityResponse>> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(activityService.cancel(id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        activityService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
