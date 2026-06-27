package com.erp.hr.adapter.in.web;

import com.erp.common.response.ApiResponse;
import com.erp.hr.application.dto.JobGradeCreateRequest;
import com.erp.hr.application.dto.JobGradeResponse;
import com.erp.hr.application.dto.JobGradeUpdateRequest;
import com.erp.hr.application.service.JobGradeService;
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
@RequestMapping("/api/hr/job-grades")
@RequiredArgsConstructor
public class JobGradeController {

  private final JobGradeService jobGradeService;

  @GetMapping
  public ResponseEntity<ApiResponse<List<JobGradeResponse>>> findAll() {
    return ResponseEntity.ok(ApiResponse.ok(jobGradeService.findAll()));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<JobGradeResponse>> findById(@PathVariable Long id) {
    return ResponseEntity.ok(ApiResponse.ok(jobGradeService.findById(id)));
  }

  @PostMapping
  public ResponseEntity<ApiResponse<JobGradeResponse>> create(
      @Valid @RequestBody JobGradeCreateRequest request) {
    JobGradeResponse response = jobGradeService.create(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
  }

  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<JobGradeResponse>> update(
      @PathVariable Long id, @Valid @RequestBody JobGradeUpdateRequest request) {
    return ResponseEntity.ok(ApiResponse.ok(jobGradeService.update(id, request)));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    jobGradeService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
