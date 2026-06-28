package com.erp.common.userdirectory;

import com.erp.common.response.ApiResponse;
import com.erp.common.security.CurrentUserProvider;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * sub 집합 → 표시이름 일괄 해소 API. 감사·결재·CRM 담당자 화면이 UUID(sub)를 사람 이름으로 바꾸는 데 쓴다. 현재 테넌트 범위로만 해소(테넌트 격리).
 * 별도 권한 불필요(인증만) — 표시이름은 테넌트 내 일반 정보다.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserDirectoryController {

  private final UserDirectoryService userDirectoryService;
  private final CurrentUserProvider currentUserProvider;

  @GetMapping("/display-names")
  public ResponseEntity<ApiResponse<List<UserDisplayNameResponse>>> displayNames(
      @RequestParam(required = false) List<String> subs) {
    Long tenantId = currentUserProvider.getCurrentTenantId();
    Map<String, String> resolved =
        userDirectoryService.displayNames(tenantId, subs == null ? List.of() : subs);
    List<UserDisplayNameResponse> result =
        resolved.entrySet().stream()
            .map(e -> new UserDisplayNameResponse(e.getKey(), e.getValue()))
            .toList();
    return ResponseEntity.ok(ApiResponse.ok(result));
  }
}
