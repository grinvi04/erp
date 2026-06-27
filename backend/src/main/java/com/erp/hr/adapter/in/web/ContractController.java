package com.erp.hr.adapter.in.web;

import com.erp.common.response.ApiResponse;
import com.erp.hr.application.dto.ContractCreateRequest;
import com.erp.hr.application.dto.ContractResponse;
import com.erp.hr.application.service.ContractService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hr/employees/{employeeId}/contracts")
@RequiredArgsConstructor
public class ContractController {

  private final ContractService contractService;

  @GetMapping
  public ResponseEntity<ApiResponse<List<ContractResponse>>> findByEmployee(
      @PathVariable Long employeeId) {
    return ResponseEntity.ok(ApiResponse.ok(contractService.findByEmployee(employeeId)));
  }

  @PostMapping
  public ResponseEntity<ApiResponse<ContractResponse>> create(
      @PathVariable Long employeeId, @Valid @RequestBody ContractCreateRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.ok(contractService.create(employeeId, request)));
  }
}
