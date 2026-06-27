package com.erp.common.security;

import com.erp.common.response.ApiResponse;
import com.erp.common.security.dto.AccessProfileRequest;
import com.erp.common.security.dto.AccessProfileResponse;
import com.erp.common.security.dto.RoleResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 사용자별 역할 배정·접근 프로파일 관리 API(관리자 전용 — iam:read/write). */
@RestController
@RequestMapping("/api/iam/users/{userId}")
@RequiredArgsConstructor
public class IamUserAccessController {

  private final IamService iamService;

  @GetMapping("/roles")
  public ResponseEntity<ApiResponse<List<RoleResponse>>> userRoles(@PathVariable String userId) {
    return ResponseEntity.ok(ApiResponse.ok(iamService.getUserRoles(userId)));
  }

  @PostMapping("/roles/{roleId}")
  public ResponseEntity<Void> assignRole(@PathVariable String userId, @PathVariable Long roleId) {
    iamService.assignRole(userId, roleId);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/roles/{roleId}")
  public ResponseEntity<Void> unassignRole(@PathVariable String userId, @PathVariable Long roleId) {
    iamService.unassignRole(userId, roleId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/access-profile")
  public ResponseEntity<ApiResponse<AccessProfileResponse>> accessProfile(
      @PathVariable String userId) {
    return ResponseEntity.ok(ApiResponse.ok(iamService.getAccessProfile(userId)));
  }

  @PutMapping("/access-profile")
  public ResponseEntity<ApiResponse<AccessProfileResponse>> setAccessProfile(
      @PathVariable String userId, @Valid @RequestBody AccessProfileRequest request) {
    return ResponseEntity.ok(ApiResponse.ok(iamService.setAccessProfile(userId, request)));
  }
}
