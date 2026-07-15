package com.erp.common.security;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 현재 사용자의 데이터 스코프 정책·소속을 DB(접근 프로파일)에서 읽는다(auth-standards: DB 관리). 프로파일 미설정·비인증 시 {@link
 * DataScope#SELF}로 제한한다.
 */
@Component
@RequiredArgsConstructor
public class DataScopeProvider {

  private final CurrentUserProvider currentUserProvider;
  private final AuthorizationResolver authorizationResolver;

  public DataScope getDataScope() {
    return profile().map(UserAccessProfile::getDataScope).orElse(DataScope.SELF);
  }

  public Long getDepartmentId() {
    return profile().map(UserAccessProfile::getDepartmentId).orElse(null);
  }

  private Optional<UserAccessProfile> profile() {
    return authorizationResolver.accessProfile(
        currentUserProvider.getCurrentTenantId(), currentUserProvider.getCurrentUserId());
  }
}
