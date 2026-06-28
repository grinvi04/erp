package com.erp.hr.adapter.in.web;

import com.erp.common.response.ApiResponse;
import com.erp.hr.application.dto.LeavePolicyCreateRequest;
import com.erp.hr.application.dto.LeavePolicyResponse;
import com.erp.hr.application.service.LeavePolicyService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hr/leave-policies")
@RequiredArgsConstructor
public class LeavePolicyController {

  private final LeavePolicyService leavePolicyService;

  @GetMapping
  public ResponseEntity<ApiResponse<List<LeavePolicyResponse>>> findAll() {
    return ResponseEntity.ok(ApiResponse.ok(leavePolicyService.findAll()));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<LeavePolicyResponse>> findById(@PathVariable Long id) {
    return ResponseEntity.ok(ApiResponse.ok(leavePolicyService.findById(id)));
  }

  @PostMapping
  public ResponseEntity<ApiResponse<LeavePolicyResponse>> create(
      @Valid @RequestBody LeavePolicyCreateRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.ok(leavePolicyService.create(request)));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    leavePolicyService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
