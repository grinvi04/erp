package com.erp.common.security;

import com.erp.common.audit.AuditLog;
import com.erp.common.audit.AuditService;
import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.security.dto.AccessProfileRequest;
import com.erp.common.security.dto.AccessProfileResponse;
import com.erp.common.security.dto.RoleCreateRequest;
import com.erp.common.security.dto.RoleResponse;
import com.erp.common.security.dto.RoleUpdateRequest;
import com.erp.common.tenant.TenantContext;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 역할·권한·배정·접근프로파일 관리(IAM) — DB 정본의 운영 관리 진입점. 모든 읽기는 iam:read,
 * 쓰기는 iam:write 권한을 요구하며, 권한 변경은 전부 감사 로그에 남긴다(auth-standards).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IamService {

    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserAccessProfileRepository accessProfileRepository;
    private final PermissionChecker permissionChecker;
    private final AuditService auditService;

    // --- 권한 카탈로그 ---
    public Set<String> permissionCatalog() {
        permissionChecker.require(Permission.IAM_READ);
        return Permission.all();
    }

    // --- 역할 ---
    public List<RoleResponse> listRoles() {
        permissionChecker.require(Permission.IAM_READ);
        return roleRepository.findByTenantIdOrderByCodeAsc(tenant()).stream()
            .map(RoleResponse::from).toList();
    }

    public RoleResponse getRole(Long id) {
        permissionChecker.require(Permission.IAM_READ);
        return RoleResponse.from(getRoleOrThrow(id));
    }

    @Transactional
    public RoleResponse createRole(RoleCreateRequest request) {
        permissionChecker.require(Permission.IAM_WRITE);
        Long tenant = tenant();
        if (roleRepository.existsByTenantIdAndCode(tenant, request.code())) {
            throw new ErpException(ErrorCode.DUPLICATE_CODE);
        }
        Role role = Role.of(tenant, request.code(), request.name(), request.description());
        applyPermissions(role, request.permissions());
        Role saved = roleRepository.save(role);
        auditService.record("ROLE", saved.getId(), AuditLog.AuditAction.CREATE, null,
            "{\"code\":\"" + saved.getCode() + "\"}");
        return RoleResponse.from(saved);
    }

    @Transactional
    public RoleResponse updateRole(Long id, RoleUpdateRequest request) {
        permissionChecker.require(Permission.IAM_WRITE);
        Role role = getRoleOrThrow(id);
        role.rename(request.name(), request.description());
        role.getPermissions().clear();
        applyPermissions(role, request.permissions());
        auditService.record("ROLE", role.getId(), AuditLog.AuditAction.UPDATE, null,
            "{\"code\":\"" + role.getCode() + "\"}");
        return RoleResponse.from(role);
    }

    @Transactional
    public void deleteRole(Long id) {
        permissionChecker.require(Permission.IAM_WRITE);
        Role role = getRoleOrThrow(id);
        roleRepository.delete(role);
        auditService.record("ROLE", id, AuditLog.AuditAction.DELETE, null,
            "{\"code\":\"" + role.getCode() + "\"}");
    }

    // --- 사용자 역할 배정 ---
    public List<RoleResponse> getUserRoles(String userId) {
        permissionChecker.require(Permission.IAM_READ);
        return userRoleRepository.findByTenantIdAndUserId(tenant(), userId).stream()
            .map(ur -> RoleResponse.from(ur.getRole())).toList();
    }

    @Transactional
    public void assignRole(String userId, Long roleId) {
        permissionChecker.require(Permission.IAM_WRITE);
        Long tenant = tenant();
        Role role = getRoleOrThrow(roleId);
        if (userRoleRepository.existsByTenantIdAndUserIdAndRoleId(tenant, userId, roleId)) {
            throw new ErpException(ErrorCode.DUPLICATE_CODE);
        }
        userRoleRepository.save(UserRole.of(tenant, userId, role));
        auditService.record("USER_ROLE", roleId, AuditLog.AuditAction.CREATE, null,
            "{\"userId\":\"" + userId + "\",\"roleCode\":\"" + role.getCode() + "\"}");
    }

    @Transactional
    public void unassignRole(String userId, Long roleId) {
        permissionChecker.require(Permission.IAM_WRITE);
        UserRole userRole = userRoleRepository
            .findByTenantIdAndUserIdAndRoleId(tenant(), userId, roleId)
            .orElseThrow(() -> new ErpException(ErrorCode.RESOURCE_NOT_FOUND));
        userRoleRepository.delete(userRole);
        auditService.record("USER_ROLE", roleId, AuditLog.AuditAction.DELETE, null,
            "{\"userId\":\"" + userId + "\"}");
    }

    // --- 접근 프로파일(데이터 스코프·전결 한도) ---
    public AccessProfileResponse getAccessProfile(String userId) {
        permissionChecker.require(Permission.IAM_READ);
        return accessProfileRepository.findByTenantIdAndUserId(tenant(), userId)
            .map(AccessProfileResponse::from)
            .orElseThrow(() -> new ErpException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Transactional
    public AccessProfileResponse setAccessProfile(String userId, AccessProfileRequest request) {
        permissionChecker.require(Permission.IAM_WRITE);
        Long tenant = tenant();
        UserAccessProfile profile = accessProfileRepository.findByTenantIdAndUserId(tenant, userId)
            .map(existing -> {
                existing.update(request.dataScope(), request.departmentId(), request.approvalLimit());
                return existing;
            })
            .orElseGet(() -> UserAccessProfile.of(tenant, userId, request.dataScope(),
                request.departmentId(), request.approvalLimit()));
        UserAccessProfile saved = accessProfileRepository.save(profile);
        auditService.record("ACCESS_PROFILE", saved.getId(), AuditLog.AuditAction.UPDATE, null,
            "{\"userId\":\"" + userId + "\",\"dataScope\":\"" + saved.getDataScope() + "\"}");
        return AccessProfileResponse.from(saved);
    }

    private void applyPermissions(Role role, Set<String> permissions) {
        if (permissions == null) {
            return;
        }
        Set<String> known = Permission.all();
        for (String code : permissions) {
            if (!known.contains(code)) {
                throw new ErpException(ErrorCode.INVALID_INPUT);
            }
            role.grant(code);
        }
    }

    private Role getRoleOrThrow(Long id) {
        return roleRepository.findByTenantIdAndId(tenant(), id)
            .orElseThrow(() -> new ErpException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private Long tenant() {
        return TenantContext.requireTenantId();
    }
}
