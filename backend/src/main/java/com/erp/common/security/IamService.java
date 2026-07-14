package com.erp.common.security;

import com.erp.common.audit.AuditLog;
import com.erp.common.audit.AuditLogRepository;
import com.erp.common.audit.AuditService;
import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.security.dto.AccessProfileRequest;
import com.erp.common.security.dto.AccessProfileResponse;
import com.erp.common.security.dto.RoleCreateRequest;
import com.erp.common.security.dto.RoleResponse;
import com.erp.common.security.dto.RoleUpdateRequest;
import com.erp.common.security.dto.UserLookupResponse;
import com.erp.common.tenant.TenantContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 역할·권한·배정·접근프로파일 관리(IAM) — DB 정본의 운영 관리 진입점. 모든 읽기는 iam:read, 쓰기는 iam:write 권한을 요구하며, 권한 변경은 전부 감사
 * 로그에 남긴다(auth-standards).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IamService {

  private final RoleRepository roleRepository;
  private final UserRoleRepository userRoleRepository;
  private final UserAccessProfileRepository accessProfileRepository;
  private final PermissionChecker permissionChecker;
  private final CurrentUserProvider currentUserProvider;
  private final AuditService auditService;
  private final AuditLogRepository auditLogRepository;
  private final ObjectMapper objectMapper;

  // --- 권한 카탈로그 ---
  public Set<String> permissionCatalog() {
    permissionChecker.require(Permission.IAM_READ);
    if (!isRestrictedAdministrator()) {
      return Permission.all();
    }
    return Permission.all().stream()
        .filter(permission -> !isProtectedPermission(permission))
        .collect(java.util.stream.Collectors.toUnmodifiableSet());
  }

  // --- 역할 ---
  public List<RoleResponse> listRoles() {
    permissionChecker.require(Permission.IAM_READ);
    var roles = roleRepository.findByTenantIdWithPermissionsOrderByCodeAsc(tenant()).stream();
    if (isRestrictedAdministrator()) {
      roles = roles.filter(role -> !isProtectedRole(role));
    }
    return roles.map(RoleResponse::from).toList();
  }

  public RoleResponse getRole(Long id) {
    permissionChecker.require(Permission.IAM_READ);
    Role role = getRoleOrThrow(id);
    if (isRestrictedAdministrator() && isProtectedRole(role)) {
      throw forbidden();
    }
    return RoleResponse.from(role);
  }

  @Transactional
  public RoleResponse createRole(RoleCreateRequest request) {
    boolean delegated = requireMutationAccess();
    Long tenant = tenant();
    if (roleRepository.existsByTenantIdAndCode(tenant, request.code())) {
      throw new ErpException(ErrorCode.DUPLICATE_CODE);
    }
    validatePermissions(request.permissions());
    if (delegated) {
      validateDelegatedPermissions(request.permissions());
    }
    Role role = Role.of(tenant, request.code(), request.name(), request.description());
    grantAll(role, request.permissions());
    Role saved = roleRepository.save(role);
    auditService.record(
        "ROLE",
        saved.getId(),
        AuditLog.AuditAction.CREATE,
        null,
        json(Map.of("code", saved.getCode())));
    return RoleResponse.from(saved);
  }

  @Transactional
  public RoleResponse updateRole(Long id, RoleUpdateRequest request) {
    boolean delegated = requireMutationAccess();
    Role role = getRoleOrThrow(id);
    if (delegated) {
      validateDelegatedRoleTarget(role, id);
      validateDelegatedPermissions(request.permissions());
    }
    // 미지 권한 코드는 변경(clear) 전에 거부 — 부분 변경 방지.
    validatePermissions(request.permissions());
    role.rename(request.name(), request.description());
    role.replacePermissions(request.permissions());
    auditService.record(
        "ROLE",
        role.getId(),
        AuditLog.AuditAction.UPDATE,
        null,
        json(Map.of("code", role.getCode())));
    return RoleResponse.from(role);
  }

  @Transactional
  public void deleteRole(Long id) {
    boolean delegated = requireMutationAccess();
    Role role = getRoleOrThrow(id);
    if (delegated) {
      validateDelegatedRoleTarget(role, id);
    }
    roleRepository.delete(role);
    auditService.record(
        "ROLE", id, AuditLog.AuditAction.DELETE, null, json(Map.of("code", role.getCode())));
  }

  // --- 사용자 존재 검증 ---
  /**
   * sub가 실재하는 사용자인지 IAM이 아는 흔적으로 검증한다 — user_directory가 없으므로 감사 기록·역할 배정·접근 프로파일 중 하나라도 있으면 known.
   * 화면이 이 신호로 "알 수 없는 사용자"를 경고·차단해 유령 sub 무단 배정을 막는다. (확정 단정이 아닌 신호이므로 차단이 아닌 검증 정보만 제공한다.)
   */
  public UserLookupResponse lookupUser(String userId) {
    permissionChecker.require(Permission.IAM_READ);
    Long tenant = tenant();
    int roleCount = userRoleRepository.findByTenantIdAndUserId(tenant, userId).size();
    boolean hasProfile =
        accessProfileRepository.findByTenantIdAndUserId(tenant, userId).isPresent();
    boolean audited = auditLogRepository.existsByTenantIdAndPerformedBy(tenant, userId);
    boolean known = roleCount > 0 || hasProfile || audited;
    return new UserLookupResponse(userId, known, roleCount, hasProfile, audited);
  }

  // --- 사용자 역할 배정 ---
  public List<RoleResponse> getUserRoles(String userId) {
    permissionChecker.require(Permission.IAM_READ);
    var roles =
        userRoleRepository.findByTenantIdAndUserIdWithRolePermissions(tenant(), userId).stream();
    if (isRestrictedAdministrator()) {
      roles = roles.filter(userRole -> !isProtectedRole(userRole.getRole()));
    }
    return roles.map(ur -> RoleResponse.from(ur.getRole())).toList();
  }

  public void requireManageableUser(String userId) {
    boolean delegated = requireMutationAccess();
    if (!delegated) {
      return;
    }
    rejectSelfMutation(userId);
    boolean protectedRole =
        userRoleRepository.findByTenantIdAndUserIdWithRolePermissions(tenant(), userId).stream()
            .map(UserRole::getRole)
            .anyMatch(this::isProtectedRole);
    if (protectedRole) {
      throw forbidden();
    }
  }

  @Transactional
  public void assignRole(String userId, Long roleId) {
    boolean delegated = requireMutationAccess();
    if (delegated) {
      rejectSelfMutation(userId);
    }
    Long tenant = tenant();
    Role role = getRoleOrThrow(roleId);
    if (delegated && isProtectedRole(role)) {
      throw forbidden();
    }
    if (userRoleRepository.existsByTenantIdAndUserIdAndRoleId(tenant, userId, roleId)) {
      throw new ErpException(ErrorCode.DUPLICATE_CODE);
    }
    userRoleRepository.save(UserRole.of(tenant, userId, role));
    auditService.record(
        "USER_ROLE",
        roleId,
        AuditLog.AuditAction.CREATE,
        null,
        json(Map.of("userId", userId, "roleCode", role.getCode())));
  }

  @Transactional
  public void unassignRole(String userId, Long roleId) {
    boolean delegated = requireMutationAccess();
    if (delegated) {
      rejectSelfMutation(userId);
    }
    UserRole userRole =
        userRoleRepository
            .findByTenantIdAndUserIdAndRoleId(tenant(), userId, roleId)
            .orElseThrow(() -> new ErpException(ErrorCode.RESOURCE_NOT_FOUND));
    if (delegated && isProtectedRole(userRole.getRole())) {
      throw forbidden();
    }
    userRoleRepository.delete(userRole);
    auditService.record(
        "USER_ROLE", roleId, AuditLog.AuditAction.DELETE, null, json(Map.of("userId", userId)));
  }

  // --- 접근 프로파일(데이터 스코프·전결 한도) ---
  public AccessProfileResponse getAccessProfile(String userId) {
    permissionChecker.require(Permission.IAM_READ);
    return accessProfileRepository
        .findByTenantIdAndUserId(tenant(), userId)
        .map(AccessProfileResponse::from)
        .orElseThrow(() -> new ErpException(ErrorCode.RESOURCE_NOT_FOUND));
  }

  @Transactional
  public AccessProfileResponse setAccessProfile(String userId, AccessProfileRequest request) {
    boolean delegated = requireMutationAccess();
    if (delegated) {
      rejectSelfMutation(userId);
    }
    Long tenant = tenant();
    UserAccessProfile profile =
        accessProfileRepository
            .findByTenantIdAndUserId(tenant, userId)
            .map(
                existing -> {
                  existing.update(
                      request.dataScope(), request.departmentId(), request.approvalLimit());
                  return existing;
                })
            .orElseGet(
                () ->
                    UserAccessProfile.of(
                        tenant,
                        userId,
                        request.dataScope(),
                        request.departmentId(),
                        request.approvalLimit()));
    UserAccessProfile saved = accessProfileRepository.save(profile);
    auditService.record(
        "ACCESS_PROFILE",
        saved.getId(),
        AuditLog.AuditAction.UPDATE,
        null,
        json(Map.of("userId", userId, "dataScope", saved.getDataScope().name())));
    return AccessProfileResponse.from(saved);
  }

  private void validatePermissions(Set<String> permissions) {
    if (permissions == null) {
      return;
    }
    Set<String> known = Permission.all();
    for (String code : permissions) {
      if (!known.contains(code)) {
        throw new ErpException(ErrorCode.INVALID_INPUT);
      }
    }
  }

  private void grantAll(Role role, Set<String> permissions) {
    if (permissions != null) {
      permissions.forEach(role::grant);
    }
  }

  private boolean requireMutationAccess() {
    if (permissionChecker.hasPermission(Permission.IAM_WRITE)) {
      return false;
    }
    if (permissionChecker.hasPermission(Permission.IAM_DELEGATE)) {
      return true;
    }
    permissionChecker.require(Permission.IAM_WRITE);
    return false;
  }

  private boolean isRestrictedAdministrator() {
    return !permissionChecker.hasPermission(Permission.IAM_WRITE);
  }

  private void validateDelegatedPermissions(Set<String> permissions) {
    if (permissions != null && permissions.stream().anyMatch(this::isProtectedPermission)) {
      throw forbidden();
    }
  }

  private void validateDelegatedRoleTarget(Role role, Long roleId) {
    if (isProtectedRole(role)
        || userRoleRepository.existsByTenantIdAndUserIdAndRoleId(
            tenant(), currentUserProvider.getCurrentUserId(), roleId)) {
      throw forbidden();
    }
  }

  private boolean isProtectedRole(Role role) {
    return "SUPER_ADMIN".equals(role.getCode())
        || role.getPermissions().stream().anyMatch(this::isProtectedPermission);
  }

  private boolean isProtectedPermission(String permission) {
    return permission != null
        && (permission.startsWith("hr:")
            || Permission.IAM_WRITE.equals(permission)
            || Permission.IAM_DELEGATE.equals(permission));
  }

  private void rejectSelfMutation(String userId) {
    if (java.util.Objects.equals(currentUserProvider.getCurrentUserId(), userId)) {
      throw forbidden();
    }
  }

  private ErpException forbidden() {
    return new ErpException(ErrorCode.FORBIDDEN);
  }

  /** 감사 afterData JSON 안전 직렬화 — userId 등 외부 입력의 따옴표가 jsonb를 깨지 않도록 Jackson 사용. */
  private String json(Map<String, Object> data) {
    try {
      return objectMapper.writeValueAsString(data);
    } catch (JsonProcessingException e) {
      return null; // afterData는 부가정보 — 직렬화 실패 시 생략(감사 자체는 기록)
    }
  }

  private Role getRoleOrThrow(Long id) {
    return roleRepository
        .findByTenantIdAndId(tenant(), id)
        .orElseThrow(() -> new ErpException(ErrorCode.RESOURCE_NOT_FOUND));
  }

  private Long tenant() {
    return TenantContext.requireTenantId();
  }
}
