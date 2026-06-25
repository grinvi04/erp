package com.erp.common.security;

import com.erp.common.response.ApiResponse;
import com.erp.common.security.dto.RoleCreateRequest;
import com.erp.common.security.dto.RoleResponse;
import com.erp.common.security.dto.RoleUpdateRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Set;
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

/** 역할·권한 카탈로그 관리 API(관리자 전용 — iam:read/write). */
@RestController
@RequestMapping("/api/iam")
@RequiredArgsConstructor
public class IamRoleController {

    private final IamService iamService;

    @GetMapping("/permissions")
    public ResponseEntity<ApiResponse<Set<String>>> permissions() {
        return ResponseEntity.ok(ApiResponse.ok(iamService.permissionCatalog()));
    }

    @GetMapping("/roles")
    public ResponseEntity<ApiResponse<List<RoleResponse>>> listRoles() {
        return ResponseEntity.ok(ApiResponse.ok(iamService.listRoles()));
    }

    @GetMapping("/roles/{id}")
    public ResponseEntity<ApiResponse<RoleResponse>> getRole(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(iamService.getRole(id)));
    }

    @PostMapping("/roles")
    public ResponseEntity<ApiResponse<RoleResponse>> createRole(@Valid @RequestBody RoleCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(iamService.createRole(request)));
    }

    @PutMapping("/roles/{id}")
    public ResponseEntity<ApiResponse<RoleResponse>> updateRole(
        @PathVariable Long id, @Valid @RequestBody RoleUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(iamService.updateRole(id, request)));
    }

    @DeleteMapping("/roles/{id}")
    public ResponseEntity<Void> deleteRole(@PathVariable Long id) {
        iamService.deleteRole(id);
        return ResponseEntity.noContent().build();
    }
}
