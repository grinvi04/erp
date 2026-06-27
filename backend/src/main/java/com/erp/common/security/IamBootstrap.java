package com.erp.common.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 부트스트랩 — 인가를 DB로 전환하면 초기엔 누구도 iam:write 권한이 없어 관리 API를 쓸 수 없다 (chicken-and-egg). 설정된 관리자(sub)에게 모든
 * 권한을 가진 SUPER_ADMIN 역할을 기동 시 보장한다. 멱등 — 재기동 시 권한 카탈로그가 늘어나면 역할에 반영한다. {@code
 * erp.iam.bootstrap.admin-sub} 미설정이면 아무 것도 하지 않는다. 테스트 프로파일에선 비활성(테스트는 자체 시드).
 */
@Component
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class IamBootstrap implements ApplicationRunner {

  private static final String SUPER_ADMIN = "SUPER_ADMIN";

  private final RoleRepository roleRepository;
  private final UserRoleRepository userRoleRepository;

  @Value("${erp.iam.bootstrap.admin-sub:}")
  private String adminSub;

  @Value("${erp.iam.bootstrap.tenant-id:1}")
  private Long tenantId;

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (adminSub == null || adminSub.isBlank()) {
      return;
    }
    Role role =
        roleRepository
            .findByTenantIdAndCode(tenantId, SUPER_ADMIN)
            .orElseGet(() -> Role.of(tenantId, SUPER_ADMIN, "슈퍼 관리자", "모든 권한(부트스트랩)"));
    Permission.all().forEach(role::grant);
    role = roleRepository.save(role);
    if (!userRoleRepository.existsByTenantIdAndUserIdAndRoleId(tenantId, adminSub, role.getId())) {
      userRoleRepository.save(UserRole.of(tenantId, adminSub, role));
      log.info("IAM 부트스트랩: sub={} 에 SUPER_ADMIN 역할 배정(tenant={})", adminSub, tenantId);
    }
  }
}
