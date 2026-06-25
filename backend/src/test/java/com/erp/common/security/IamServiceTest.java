package com.erp.common.security;

import com.erp.common.audit.AuditLog;
import com.erp.common.audit.AuditService;
import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.security.dto.RoleCreateRequest;
import com.erp.common.tenant.TenantContext;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class IamServiceTest {

    @Mock private RoleRepository roleRepository;
    @Mock private UserRoleRepository userRoleRepository;
    @Mock private UserAccessProfileRepository accessProfileRepository;
    @Mock private PermissionChecker permissionChecker;
    @Mock private AuditService auditService;

    @InjectMocks
    private IamService iamService;

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

        ErpException ex = assertThrows(ErpException.class, () -> iamService.createRole(
                new RoleCreateRequest("HR_MGR", "인사관리자", null, Set.of())));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_CODE);
    }

    @Test
    void createRole_unknownPermission_throwsInvalidInput() {
        given(roleRepository.existsByTenantIdAndCode(1L, "HR_MGR")).willReturn(false);

        ErpException ex = assertThrows(ErpException.class, () -> iamService.createRole(
                new RoleCreateRequest("HR_MGR", "인사관리자", null, Set.of("not:a:real:permission"))));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    void createRole_valid_requiresWriteAndAudits() {
        given(roleRepository.existsByTenantIdAndCode(1L, "HR_MGR")).willReturn(false);
        given(roleRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        iamService.createRole(new RoleCreateRequest("HR_MGR", "인사관리자", null,
                Set.of(Permission.HR_EMPLOYEE_READ)));

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
    void listRoles_requiresIamRead() {
        given(roleRepository.findByTenantIdOrderByCodeAsc(1L)).willReturn(java.util.List.of());

        iamService.listRoles();

        verify(permissionChecker).require(Permission.IAM_READ);
    }
}
