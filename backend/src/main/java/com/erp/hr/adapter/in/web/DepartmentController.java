package com.erp.hr.adapter.in.web;

import com.erp.common.response.ApiResponse;
import com.erp.hr.application.dto.DepartmentCreateRequest;
import com.erp.hr.application.dto.DepartmentResponse;
import com.erp.hr.application.dto.DepartmentUpdateRequest;
import com.erp.hr.application.service.DepartmentService;
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
@RequestMapping("/api/hr/departments")
@RequiredArgsConstructor
public class DepartmentController {

  private final DepartmentService departmentService;

  @GetMapping
  public ResponseEntity<ApiResponse<List<DepartmentResponse>>> findAll() {
    return ResponseEntity.ok(ApiResponse.ok(departmentService.findAll()));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<DepartmentResponse>> findById(@PathVariable Long id) {
    return ResponseEntity.ok(ApiResponse.ok(departmentService.findById(id)));
  }

  @PostMapping
  public ResponseEntity<ApiResponse<DepartmentResponse>> create(
      @Valid @RequestBody DepartmentCreateRequest request) {
    DepartmentResponse response = departmentService.create(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
  }

  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<DepartmentResponse>> update(
      @PathVariable Long id, @Valid @RequestBody DepartmentUpdateRequest request) {
    return ResponseEntity.ok(ApiResponse.ok(departmentService.update(id, request)));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    departmentService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
