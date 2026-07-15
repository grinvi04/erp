package com.erp.common.security;

import com.erp.common.response.ApiResponse;
import com.erp.common.security.dto.TenantUserInviteRequest;
import com.erp.common.security.dto.TenantUserReinviteRequest;
import com.erp.common.security.dto.TenantUserResponse;
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
@RequestMapping("/api/iam/tenant-users")
@RequiredArgsConstructor
public class TenantUserOnboardingController {

  private final TenantUserOnboardingService service;

  @GetMapping
  public ResponseEntity<ApiResponse<List<TenantUserResponse>>> list() {
    return ResponseEntity.ok(ApiResponse.ok(service.list()));
  }

  @PostMapping("/invitations")
  public ResponseEntity<ApiResponse<TenantUserResponse>> invite(
      @Valid @RequestBody TenantUserInviteRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(service.invite(request)));
  }

  @PostMapping("/{id}/reinvite")
  public ResponseEntity<ApiResponse<TenantUserResponse>> reinvite(
      @PathVariable Long id, @Valid @RequestBody TenantUserReinviteRequest request) {
    return ResponseEntity.ok(ApiResponse.ok(service.reinvite(id, request)));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> disable(@PathVariable Long id) {
    service.disable(id);
    return ResponseEntity.noContent().build();
  }
}
