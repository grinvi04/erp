package com.erp.hr.adapter.in.web;

import com.erp.common.response.ApiResponse;
import com.erp.common.response.PageResponse;
import com.erp.hr.application.dto.ApprovalActionRequest;
import com.erp.hr.application.dto.LeaveRequestCreateRequest;
import com.erp.hr.application.dto.LeaveRequestResponse;
import com.erp.hr.application.service.LeaveRequestService;
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
@RequestMapping("/api/hr/leave-requests")
@RequiredArgsConstructor
public class LeaveRequestController {

    private final LeaveRequestService leaveRequestService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<LeaveRequestResponse>>> find(
        @RequestParam(required = false) Long employeeId,
        @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<LeaveRequestResponse> result = employeeId != null
            ? PageResponse.from(leaveRequestService.findByEmployee(employeeId, pageable))
            : PageResponse.from(leaveRequestService.findAll(pageable));
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<LeaveRequestResponse>> create(
        @Valid @RequestBody LeaveRequestCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok(leaveRequestService.create(request)));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<LeaveRequestResponse>> approve(
        @PathVariable Long id,
        @Valid @RequestBody ApprovalActionRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(leaveRequestService.approve(id, request)));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<LeaveRequestResponse>> reject(
        @PathVariable Long id,
        @Valid @RequestBody ApprovalActionRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(leaveRequestService.reject(id, request)));
    }
}
