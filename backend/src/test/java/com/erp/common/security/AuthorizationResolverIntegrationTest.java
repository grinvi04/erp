package com.erp.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.erp.common.AbstractIntegrationTest;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * DB 인가 해석 검증 — 역할→권한 매핑·사용자배정·접근프로파일이 실제 Postgres + Flyway 위에서 (tenant,user)별로 올바르게 해석되고 테넌트 격리가
 * 지켜지는지 확인한다.
 */
@Transactional
class AuthorizationResolverIntegrationTest extends AbstractIntegrationTest {

  @Autowired private AuthorizationResolver authorizationResolver;
  @Autowired private RoleRepository roleRepository;
  @Autowired private UserRoleRepository userRoleRepository;
  @Autowired private UserAccessProfileRepository accessProfileRepository;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void permissionCodes_unionOfAssignedRolePermissions() {
    Role hr = roleRepository.save(roleWith("HR_MANAGER", "hr:employee:read", "hr:employee:write"));
    Role fin = roleRepository.save(roleWith("FINANCE_APPROVER", "finance:invoice:approve"));
    userRoleRepository.save(UserRole.of(TEST_TENANT_ID, "alice", hr));
    userRoleRepository.save(UserRole.of(TEST_TENANT_ID, "alice", fin));

    assertThat(authorizationResolver.permissionCodes(TEST_TENANT_ID, "alice"))
        .containsExactlyInAnyOrder(
            "hr:employee:read", "hr:employee:write", "finance:invoice:approve");
  }

  @Test
  void permissionCodes_isolatedByTenant() {
    Role hr = roleRepository.save(roleWith("HR_MANAGER", "hr:employee:read"));
    userRoleRepository.save(UserRole.of(TEST_TENANT_ID, "alice", hr));

    // 다른 테넌트(2)에서 같은 user_id로 조회하면 비어 있어야 한다.
    assertThat(authorizationResolver.permissionCodes(2L, "alice")).isEmpty();
  }

  @Test
  void permissionCodes_crossTenantRoleAssignment_doesNotLeak() {
    // 심층 방어: 테넌트2의 역할을 테넌트1 사용자에게 잘못 배정해도(배정행=테넌트1, 역할=테넌트2)
    // 해석은 역할의 tenant_id까지 검증하므로 권한이 새지 않는다.
    Role foreignRole = roleRepository.save(Role.of(2L, "FOREIGN", "타 테넌트 역할", null));
    foreignRole.grant("finance:invoice:approve");
    roleRepository.save(foreignRole);
    userRoleRepository.save(UserRole.of(TEST_TENANT_ID, "mallory", foreignRole));

    assertThat(authorizationResolver.permissionCodes(TEST_TENANT_ID, "mallory")).isEmpty();
  }

  @Test
  void grant_persistsParentTenantIdOnEachRolePermissionRow() {
    // AC-1: role_permission 행은 부모 역할의 tenant_id를 DB 레벨로 보유해야 한다
    // (앱 쿼리 필터만이 아니라 테이블 자체의 심층 방어).
    Role hr =
        roleRepository.saveAndFlush(
            roleWith("HR_MANAGER", "hr:employee:read", "hr:employee:write"));

    List<Long> tenantIds =
        jdbcTemplate.queryForList(
            "SELECT tenant_id FROM common.role_permission WHERE role_id = ?",
            Long.class,
            hr.getId());

    assertThat(tenantIds).hasSize(2).containsOnly(TEST_TENANT_ID);
  }

  @Test
  void accessProfile_returnsScopeAndLimit() {
    accessProfileRepository.save(
        UserAccessProfile.of(
            TEST_TENANT_ID, "bob", DataScope.DEPARTMENT, 9L, new BigDecimal("5000000")));

    var profile = authorizationResolver.accessProfile(TEST_TENANT_ID, "bob").orElseThrow();
    assertThat(profile.getDataScope()).isEqualTo(DataScope.DEPARTMENT);
    assertThat(profile.getDepartmentId()).isEqualTo(9L);
    assertThat(profile.getApprovalLimit()).isEqualByComparingTo("5000000");
  }

  private Role roleWith(String code, String... permissions) {
    Role role = Role.of(TEST_TENANT_ID, code, code, null);
    for (String p : permissions) {
      role.grant(p);
    }
    return role;
  }
}
