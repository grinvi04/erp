package com.erp.hr.adapter.in.web;

import com.erp.common.response.ApiResponse;
import com.erp.hr.application.dto.PositionCreateRequest;
import com.erp.hr.application.dto.PositionResponse;
import com.erp.hr.application.dto.PositionUpdateRequest;
import com.erp.hr.application.service.PositionService;
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
@RequestMapping("/api/hr/positions")
@RequiredArgsConstructor
public class PositionController {

  private final PositionService positionService;

  @GetMapping
  public ResponseEntity<ApiResponse<List<PositionResponse>>> findAll() {
    return ResponseEntity.ok(ApiResponse.ok(positionService.findAll()));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<PositionResponse>> findById(@PathVariable Long id) {
    return ResponseEntity.ok(ApiResponse.ok(positionService.findById(id)));
  }

  @PostMapping
  public ResponseEntity<ApiResponse<PositionResponse>> create(
      @Valid @RequestBody PositionCreateRequest request) {
    PositionResponse response = positionService.create(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
  }

  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<PositionResponse>> update(
      @PathVariable Long id, @Valid @RequestBody PositionUpdateRequest request) {
    return ResponseEntity.ok(ApiResponse.ok(positionService.update(id, request)));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    positionService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
