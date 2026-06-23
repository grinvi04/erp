package com.erp.hr.adapter.in.web;

import com.erp.common.response.ApiResponse;
import com.erp.hr.application.dto.LeaveRequestCreateRequest;
import com.erp.hr.application.dto.LeaveRequestResponse;
import com.erp.hr.application.service.LeaveRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/hr/leave-requests")
@RequiredArgsConstructor
public class LeaveRequestController {

    private final LeaveRequestService leaveRequestService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<LeaveRequestResponse>>> findByEmployee(
        @RequestParam Long employeeId) {
        return ResponseEntity.ok(ApiResponse.ok(leaveRequestService.findByEmployee(employeeId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<LeaveRequestResponse>> create(
        @Valid @RequestBody LeaveRequestCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok(leaveRequestService.create(request)));
    }
}
