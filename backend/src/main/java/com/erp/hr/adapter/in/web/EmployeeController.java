package com.erp.hr.adapter.in.web;

import com.erp.common.response.ApiResponse;
import com.erp.common.response.PageResponse;
import com.erp.hr.application.dto.EmployeeCreateRequest;
import com.erp.hr.application.dto.EmployeePromoteRequest;
import com.erp.hr.application.dto.EmployeeResponse;
import com.erp.hr.application.dto.EmployeeTerminateRequest;
import com.erp.hr.application.dto.EmployeeTransferRequest;
import com.erp.hr.application.dto.EmployeeUpdateRequest;
import com.erp.hr.application.service.EmployeeService;
import com.erp.hr.domain.model.EmployeeStatus;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hr/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<EmployeeResponse>>> findAll(
        @RequestParam(required = false) EmployeeStatus status,
        @RequestParam(required = false) Long departmentId,
        @RequestParam(required = false) String keyword,
        @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(
            PageResponse.from(employeeService.findAll(status, departmentId, keyword, pageable))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EmployeeResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(employeeService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EmployeeResponse>> create(
        @Valid @RequestBody EmployeeCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok(employeeService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EmployeeResponse>> update(
        @PathVariable Long id,
        @Valid @RequestBody EmployeeUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(employeeService.update(id, request)));
    }

    @PostMapping("/{id}/transfer")
    public ResponseEntity<ApiResponse<EmployeeResponse>> transfer(
        @PathVariable Long id,
        @Valid @RequestBody EmployeeTransferRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(employeeService.transfer(id, request)));
    }

    @PostMapping("/{id}/promote")
    public ResponseEntity<ApiResponse<EmployeeResponse>> promote(
        @PathVariable Long id,
        @Valid @RequestBody EmployeePromoteRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(employeeService.promote(id, request)));
    }

    @PostMapping("/{id}/terminate")
    public ResponseEntity<ApiResponse<EmployeeResponse>> terminate(
        @PathVariable Long id,
        @Valid @RequestBody EmployeeTerminateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(employeeService.terminate(id, request)));
    }

    @PostMapping("/{id}/on-leave")
    public ResponseEntity<ApiResponse<EmployeeResponse>> onLeave(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(employeeService.onLeave(id)));
    }

    @PostMapping("/{id}/return-from-leave")
    public ResponseEntity<ApiResponse<EmployeeResponse>> returnFromLeave(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(employeeService.returnFromLeave(id)));
    }
}
