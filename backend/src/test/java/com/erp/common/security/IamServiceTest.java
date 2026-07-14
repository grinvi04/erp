package com.erp.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.erp.common.audit.AuditLog;
import com.erp.common.audit.AuditService;
import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.security.dto.RoleCreateRequest;
import com.erp.common.tenant.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IamServiceTest {

  @Mock private RoleRepository roleRepository;
  @Mock private UserRoleRepository userRoleRepository;
  @Mock private UserAccessProfileRepository accessProfileRepository;
  @Mock private PermissionChecker permissionChecker;
  @Mock private CurrentUserProvider currentUserProvider;
  @Mock private AuditService auditService;
  @Mock private com.erp.common.audit.AuditLogRepository auditLogRepository;
  @org.mockito.Spy private ObjectMapper objectMapper = new ObjectMapper();

  @InjectMocks private IamService iamService;

  @BeforeEach
  void setTenant() {
    TenantContext.setTenantId(1L);
  }

  @AfterEach
  void clearTenant() {
    TenantContext.clear();
  }

  @Test
  void createRole_duplicateCode_throws() {
    given(roleRepository.existsByTenantIdAndCode(1L, "HR_MGR")).willReturn(true);

    ErpException ex =
        assertThrows(
            ErpException.class,
            () -> iamService.createRole(new RoleCreateRequest("HR_MGR", "인사관리자", null, Set.of())));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_CODE);
  }

  @Test
  void createRole_unknownPermission_throwsInvalidInput() {
    given(roleRepository.existsByTenantIdAndCode(1L, "HR_MGR")).willReturn(false);

    ErpException ex =
        assertThrows(
            ErpException.class,
            () ->
                iamService.createRole(
                    new RoleCreateRequest(
                        "HR_MGR", "인사관리자", null, Set.of("not:a:real:permission"))));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT);
  }

  @Test
  void createRole_valid_requiresWriteAndAudits() {
    given(roleRepository.existsByTenantIdAndCode(1L, "HR_MGR")).willReturn(false);
    given(roleRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

    iamService.createRole(
        new RoleCreateRequest("HR_MGR", "인사관리자", null, Set.of(Permission.HR_EMPLOYEE_READ)));

    verify(permissionChecker).require(Permission.IAM_WRITE);
    verify(auditService).record(eq("ROLE"), any(), eq(AuditLog.AuditAction.CREATE), any(), any());
  }

  @Test
  void assignRole_alreadyAssigned_throws() {
    Role role = Role.of(1L, "HR_MGR", "인사관리자", null);
    given(roleRepository.findByTenantIdAndId(1L, 7L)).willReturn(java.util.Optional.of(role));
    given(userRoleRepository.existsByTenantIdAndUserIdAndRoleId(1L, "bob", 7L)).willReturn(true);

    ErpException ex = assertThrows(ErpException.class, () -> iamService.assignRole("bob", 7L));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_CODE);
  }

  @Test
  void lookupUser_ghostSub_isUnknown() {
    given(userRoleRepository.findByTenantIdAndUserId(1L, "ghost")).willReturn(java.util.List.of());
    given(accessProfileRepository.findByTenantIdAndUserId(1L, "ghost"))
        .willReturn(java.util.Optional.empty());
    given(auditLogRepository.existsByTenantIdAndPerformedBy(1L, "ghost")).willReturn(false);

    var result = iamService.lookupUser("ghost");

    verify(permissionChecker).require(Permission.IAM_READ);
    assertThat(result.known()).isFalse();
    assertThat(result.roleCount()).isZero();
  }

  @Test
  void lookupUser_auditedSub_isKnown() {
    given(userRoleRepository.findByTenantIdAndUserId(1L, "bob")).willReturn(java.util.List.of());
    given(accessProfileRepository.findByTenantIdAndUserId(1L, "bob"))
        .willReturn(java.util.Optional.empty());
    given(auditLogRepository.existsByTenantIdAndPerformedBy(1L, "bob")).willReturn(true);

    var result = iamService.lookupUser("bob");

    assertThat(result.known()).isTrue();
    assertThat(result.audited()).isTrue();
  }

  @Test
  void listRoles_requiresIamRead() {
    given(roleRepository.findByTenantIdWithPermissionsOrderByCodeAsc(1L))
        .willReturn(java.util.List.of());

    iamService.listRoles();

    verify(permissionChecker).require(Permission.IAM_READ);
  }

  @Test
  void permissionCatalog_delegate_hidesProtectedPermissions() {
    given(permissionChecker.hasPermission(Permission.IAM_WRITE)).willReturn(false);

    Set<String> catalog = iamService.permissionCatalog();

    assertThat(catalog)
        .contains(Permission.FINANCE_READ, Permission.INVENTORY_READ, Permission.CRM_READ)
        .doesNotContain(Permission.HR_EMPLOYEE_READ, Permission.IAM_WRITE, "iam:delegate");
  }

  @Test
  void listRoles_delegate_hidesSuperAdminAndProtectedPermissions() {
    given(permissionChecker.hasPermission(Permission.IAM_WRITE)).willReturn(false);
    Role safe = Role.of(1L, "FINANCE_VIEWER", "재무 조회", null);
    safe.grant(Permission.FINANCE_READ);
    Role superAdmin = Role.of(1L, "SUPER_ADMIN", "슈퍼 관리자", null);
    superAdmin.grant(Permission.FINANCE_READ);
    Role hr = Role.of(1L, "HR_VIEWER", "인사 조회", null);
    hr.grant(Permission.HR_EMPLOYEE_READ);
    Role iam = Role.of(1L, "IAM_OPERATOR", "IAM 운영", null);
    iam.grant(Permission.IAM_WRITE);
    given(roleRepository.findByTenantIdWithPermissionsOrderByCodeAsc(1L))
        .willReturn(java.util.List.of(safe, superAdmin, hr, iam));

    var roles = iamService.listRoles();

    assertThat(roles)
        .extracting(com.erp.common.security.dto.RoleResponse::code)
        .containsExactly("FINANCE_VIEWER");
  }

  @Test
  void createRole_delegate_allowsOnlyBusinessPermissions() {
    given(permissionChecker.hasPermission(Permission.IAM_WRITE)).willReturn(false);
    given(permissionChecker.hasPermission("iam:delegate")).willReturn(true);
    given(roleRepository.existsByTenantIdAndCode(1L, "FINANCE_VIEWER")).willReturn(false);
    given(roleRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

    iamService.createRole(
        new RoleCreateRequest("FINANCE_VIEWER", "재무 조회", null, Set.of(Permission.FINANCE_READ)));

    verify(roleRepository).save(any());
    verify(permissionChecker, never()).require(Permission.IAM_WRITE);
  }

  @Test
  void createRole_delegate_rejectsHrAndIamPermissionsWithoutWriting() {
    given(permissionChecker.hasPermission(Permission.IAM_WRITE)).willReturn(false);
    given(permissionChecker.hasPermission("iam:delegate")).willReturn(true);

    for (String protectedPermission :
        Set.of(Permission.HR_EMPLOYEE_READ, Permission.IAM_WRITE, "iam:delegate")) {
      ErpException ex =
          assertThrows(
              ErpException.class,
              () ->
                  iamService.createRole(
                      new RoleCreateRequest(
                          "ESCALATE", "권한 상승", null, Set.of(protectedPermission))));
      assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
    }

    verify(roleRepository, never()).save(any());
  }

  @Test
  void assignRole_delegate_rejectsProtectedRole() {
    given(permissionChecker.hasPermission(Permission.IAM_WRITE)).willReturn(false);
    given(permissionChecker.hasPermission("iam:delegate")).willReturn(true);
    given(currentUserProvider.getCurrentUserId()).willReturn("delegate-admin");
    Role role = Role.of(1L, "HR_VIEWER", "인사 조회", null);
    role.grant(Permission.HR_EMPLOYEE_READ);
    given(roleRepository.findByTenantIdAndId(1L, 7L)).willReturn(java.util.Optional.of(role));

    ErpException ex =
        assertThrows(ErpException.class, () -> iamService.assignRole("customer-user", 7L));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
    verify(userRoleRepository, never()).save(any());
  }

  @Test
  void assignAndUnassignRole_delegate_rejectsSelfMutation() {
    given(permissionChecker.hasPermission(Permission.IAM_WRITE)).willReturn(false);
    given(permissionChecker.hasPermission("iam:delegate")).willReturn(true);
    given(currentUserProvider.getCurrentUserId()).willReturn("delegate-admin");

    ErpException assign =
        assertThrows(ErpException.class, () -> iamService.assignRole("delegate-admin", 7L));
    ErpException unassign =
        assertThrows(ErpException.class, () -> iamService.unassignRole("delegate-admin", 7L));

    assertThat(assign.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
    assertThat(unassign.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
    verify(userRoleRepository, never()).save(any());
    verify(userRoleRepository, never()).delete(any());
  }

  @Test
  void updateRole_delegate_rejectsRoleAssignedToSelf() {
    given(permissionChecker.hasPermission(Permission.IAM_WRITE)).willReturn(false);
    given(permissionChecker.hasPermission("iam:delegate")).willReturn(true);
    given(currentUserProvider.getCurrentUserId()).willReturn("delegate-admin");
    Role role = Role.of(1L, "BUSINESS_ADMIN", "업무 관리자", null);
    role.grant(Permission.FINANCE_READ);
    given(roleRepository.findByTenantIdAndId(1L, 7L)).willReturn(java.util.Optional.of(role));
    given(userRoleRepository.existsByTenantIdAndUserIdAndRoleId(1L, "delegate-admin", 7L))
        .willReturn(true);

    ErpException ex =
        assertThrows(
            ErpException.class,
            () ->
                iamService.updateRole(
                    7L,
                    new com.erp.common.security.dto.RoleUpdateRequest(
                        "권한상승 관리자", null, Set.of(Permission.FINANCE_READ))));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
    assertThat(role.getName()).isEqualTo("업무 관리자");
  }

  @Test
  void deleteRole_delegate_rejectsRoleAssignedToSelf() {
    given(permissionChecker.hasPermission(Permission.IAM_WRITE)).willReturn(false);
    given(permissionChecker.hasPermission("iam:delegate")).willReturn(true);
    given(currentUserProvider.getCurrentUserId()).willReturn("delegate-admin");
    Role role = Role.of(1L, "BUSINESS_ADMIN", "업무 관리자", null);
    role.grant(Permission.FINANCE_READ);
    given(roleRepository.findByTenantIdAndId(1L, 7L)).willReturn(java.util.Optional.of(role));
    given(userRoleRepository.existsByTenantIdAndUserIdAndRoleId(1L, "delegate-admin", 7L))
        .willReturn(true);

    ErpException ex = assertThrows(ErpException.class, () -> iamService.deleteRole(7L));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
    verify(roleRepository, never()).delete(any());
  }

  @Test
  void createRole_operator_keepsProtectedPermissionManagement() {
    given(permissionChecker.hasPermission(Permission.IAM_WRITE)).willReturn(true);
    given(roleRepository.existsByTenantIdAndCode(1L, "HR_OPERATOR")).willReturn(false);
    given(roleRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

    var result =
        iamService.createRole(
            new RoleCreateRequest(
                "HR_OPERATOR", "인사 운영", null, Set.of(Permission.HR_EMPLOYEE_READ)));

    assertThat(result.permissions()).containsExactly(Permission.HR_EMPLOYEE_READ);
    verify(roleRepository).save(any());
  }

  @Test
  void setAccessProfile_delegate_rejectsSelfMutation() {
    given(permissionChecker.hasPermission(Permission.IAM_WRITE)).willReturn(false);
    given(permissionChecker.hasPermission("iam:delegate")).willReturn(true);
    given(currentUserProvider.getCurrentUserId()).willReturn("delegate-admin");

    ErpException ex =
        assertThrows(
            ErpException.class,
            () ->
                iamService.setAccessProfile(
                    "delegate-admin",
                    new com.erp.common.security.dto.AccessProfileRequest(
                        DataScope.ALL, null, null)));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
    verify(accessProfileRepository, never()).save(any());
  }

  @Test
  void requireManageableUser_delegate_rejectsSelfAndProtectedRoleHolder() {
    given(permissionChecker.hasPermission(Permission.IAM_WRITE)).willReturn(false);
    given(permissionChecker.hasPermission(Permission.IAM_DELEGATE)).willReturn(true);
    given(currentUserProvider.getCurrentUserId()).willReturn("delegate-admin");

    ErpException self =
        assertThrows(ErpException.class, () -> iamService.requireManageableUser("delegate-admin"));

    Role protectedRole = Role.of(1L, "BUSINESS_ADMIN", "업무 관리자", null);
    protectedRole.grant(Permission.IAM_DELEGATE);
    given(userRoleRepository.findByTenantIdAndUserIdWithRolePermissions(1L, "other-admin"))
        .willReturn(java.util.List.of(UserRole.of(1L, "other-admin", protectedRole)));
    ErpException protectedUser =
        assertThrows(ErpException.class, () -> iamService.requireManageableUser("other-admin"));

    assertThat(self.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
    assertThat(protectedUser.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
  }
}
