package com.erp.common.security;

import com.erp.common.response.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 현재 사용자 정보 — 프론트의 메뉴/버튼 권한 게이팅용. 서버 검사가 항상 최종이며
 * 이 목록은 UI 노출 제어 보조 용도다(auth-standards).
 */
@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MeController {

    private final PermissionChecker permissionChecker;

    @GetMapping("/permissions")
    public ResponseEntity<ApiResponse<List<String>>> permissions() {
        return ResponseEntity.ok(ApiResponse.ok(
                permissionChecker.currentPermissions().stream().sorted().toList()));
    }
}
