package com.erp.common.security;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 기능 권한 검사 — application 유스케이스 진입점에서 호출한다(auth-standards: 검사 위치는 유스케이스 진입점, 컨트롤러 어노테이션은 보조). 현재 인증
 * 사용자의 authority(권한 코드)에 요구 권한이 없으면 {@link ErrorCode#FORBIDDEN}을 던진다.
 */
@Component
public class PermissionChecker {

  /** 요구 권한이 없으면 FORBIDDEN. */
  public void require(String permission) {
    if (!hasPermission(permission)) {
      throw new ErpException(ErrorCode.FORBIDDEN);
    }
  }

  public boolean hasPermission(String permission) {
    return currentAuthorities().contains(permission);
  }

  /** 현재 사용자의 권한 코드 집합(프론트 권한 게이팅용). */
  public Set<String> currentPermissions() {
    return currentAuthorities();
  }

  private Set<String> currentAuthorities() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated()) {
      return Set.of();
    }
    return auth.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .collect(Collectors.toSet());
  }
}
