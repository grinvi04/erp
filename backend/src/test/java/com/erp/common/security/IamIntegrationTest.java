package com.erp.common.security;

import com.erp.common.AbstractIntegrationTest;
import com.erp.common.security.dto.AccessProfileRequest;
import com.erp.common.security.dto.RoleResponse;
import com.erp.common.security.dto.RoleCreateRequest;
import java.math.BigDecimal;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * IAM 관리 API가 DB 인가 정본을 갱신하고, 그 결과가 AuthorizationResolver(권한 해석)에 그대로
 * 반영되는 전체 루프를 검증한다 — 관리화면 → DB → 런타임 권한.
 */
@Transactional
class IamIntegrationTest extends AbstractIntegrationTest {

    @Autowired private IamService iamService;
    @Autowired private AuthorizationResolver authorizationResolver;

    @BeforeEach
    void authAsAdmin() {
        authenticate("admin", Permission.IAM_READ, Permission.IAM_WRITE);
    }

    @Test
    void createRole_assignToUser_reflectedInPermissionResolution() {
        RoleResponse role = iamService.createRole(new RoleCreateRequest(
                "HR_MGR", "인사관리자", "직원 관리",
                Set.of(Permission.HR_EMPLOYEE_READ, Permission.HR_EMPLOYEE_WRITE)));

        iamService.assignRole("bob", role.id());

        assertThat(authorizationResolver.permissionCodes(TEST_TENANT_ID, "bob"))
                .containsExactlyInAnyOrder(Permission.HR_EMPLOYEE_READ, Permission.HR_EMPLOYEE_WRITE);
    }

    @Test
    void setAccessProfile_reflectedInResolution() {
        iamService.setAccessProfile("bob", new AccessProfileRequest(
                DataScope.DEPARTMENT, 9L, new BigDecimal("3000000")));

        UserAccessProfile profile = authorizationResolver.accessProfile(TEST_TENANT_ID, "bob").orElseThrow();
        assertThat(profile.getDataScope()).isEqualTo(DataScope.DEPARTMENT);
        assertThat(profile.getApprovalLimit()).isEqualByComparingTo("3000000");
    }

    @Test
    void updateRole_revokingPermission_removesItFromResolution() {
        RoleResponse role = iamService.createRole(new RoleCreateRequest(
                "FIN", "재무", null, Set.of(Permission.FINANCE_READ, Permission.FINANCE_WRITE)));
        iamService.assignRole("carol", role.id());

        // 권한 집합을 read만 남기도록 갱신 → write는 해석에서 사라져야 한다.
        iamService.updateRole(role.id(), new com.erp.common.security.dto.RoleUpdateRequest(
                "재무", null, Set.of(Permission.FINANCE_READ)));

        assertThat(authorizationResolver.permissionCodes(TEST_TENANT_ID, "carol"))
                .containsExactly(Permission.FINANCE_READ);
    }

    @Test
    void unassignRole_unknownAssignment_throws() {
        assertThrows(com.erp.common.exception.ErpException.class,
                () -> iamService.unassignRole("nobody", 999L));
    }

    @Test
    void assignRole_userIdWithQuote_doesNotCorruptAuditJsonb() {
        // 따옴표 포함 userId — 수동 JSON 문자열이었다면 jsonb afterData를 깨뜨려 작업이 실패했을 입력.
        // Jackson 직렬화로 안전하게 이스케이프되어 작업이 정상 완료돼야 한다.
        RoleResponse role = iamService.createRole(new RoleCreateRequest(
                "Q", "q", null, Set.of(Permission.CRM_READ)));

        org.junit.jupiter.api.Assertions.assertDoesNotThrow(
                () -> iamService.assignRole("ev\"il", role.id()));

        assertThat(authorizationResolver.permissionCodes(TEST_TENANT_ID, "ev\"il"))
                .containsExactly(Permission.CRM_READ);
    }
}
