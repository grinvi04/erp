package com.erp.common.tenant.provisioning;

import com.erp.common.audit.AuditLog;
import com.erp.common.audit.AuditLogRepository;
import com.erp.common.security.Permission;
import com.erp.common.security.Role;
import com.erp.common.security.RoleRepository;
import com.erp.common.security.UserRole;
import com.erp.common.security.UserRoleRepository;
import com.erp.common.tenant.Tenant;
import com.erp.common.tenant.TenantRepository;
import com.erp.common.tenant.TenantStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class JpaTenantProvisioningStore implements TenantProvisioningStore {

  private static final String SUPER_ADMIN = "SUPER_ADMIN";
  private static final int ACTOR_MAX_LENGTH = 100;

  private final TenantRepository tenantRepository;
  private final RoleRepository roleRepository;
  private final UserRoleRepository userRoleRepository;
  private final AuditLogRepository auditLogRepository;

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Tenant begin(TenantProvisioningRequest request) {
    String performedBy = requireActor(request.performedBy());
    Tenant tenant =
        Tenant.startProvisioning(
            request.code(), request.name(), request.plan(), request.adminUserId());
    if (tenantRepository.findByCode(tenant.getCode()).isPresent()) {
      throw new TenantAlreadyExistsException(tenant.getCode());
    }
    try {
      tenant = tenantRepository.saveAndFlush(tenant);
    } catch (DataIntegrityViolationException conflict) {
      throw new TenantAlreadyExistsException(tenant.getCode());
    }
    audit(tenant, AuditLog.AuditAction.CREATE, null, statusJson(tenant), performedBy);
    return tenant;
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Tenant beginRetry(String code, String performedBy) {
    Tenant tenant = findByCode(code);
    String before = statusJson(tenant);
    tenant.retry();
    tenantRepository.save(tenant);
    audit(
        tenant, AuditLog.AuditAction.UPDATE, before, statusJson(tenant), requireActor(performedBy));
    return tenant;
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Tenant activate(Long tenantId, String performedBy) {
    Tenant tenant = findById(tenantId);
    String before = statusJson(tenant);
    Role role =
        roleRepository
            .findByTenantIdAndCode(tenantId, SUPER_ADMIN)
            .orElseGet(() -> Role.of(tenantId, SUPER_ADMIN, "슈퍼 관리자", "모든 권한(테넌트 최초 관리자)"));
    Permission.all().forEach(role::grant);
    role = roleRepository.save(role);
    if (!userRoleRepository.existsByTenantIdAndUserIdAndRoleId(
        tenantId, tenant.getAdminUserId(), role.getId())) {
      userRoleRepository.save(UserRole.of(tenantId, tenant.getAdminUserId(), role));
    }
    tenant.activate();
    tenantRepository.save(tenant);
    audit(
        tenant, AuditLog.AuditAction.UPDATE, before, statusJson(tenant), requireActor(performedBy));
    return tenant;
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void fail(Long tenantId, String error, String performedBy) {
    Tenant tenant = findById(tenantId);
    String before = statusJson(tenant);
    tenant.fail(error);
    tenantRepository.save(tenant);
    audit(
        tenant, AuditLog.AuditAction.UPDATE, before, statusJson(tenant), requireActor(performedBy));
  }

  private Tenant findByCode(String code) {
    return tenantRepository
        .findByCode(code)
        .orElseThrow(() -> new TenantProvisioningNotFoundException(code));
  }

  private Tenant findById(Long id) {
    return tenantRepository
        .findById(id)
        .orElseThrow(() -> new TenantProvisioningNotFoundException(String.valueOf(id)));
  }

  private void audit(
      Tenant tenant, AuditLog.AuditAction action, String before, String after, String performedBy) {
    auditLogRepository.save(
        AuditLog.of(
            tenant.getId(), "TENANT", tenant.getId(), action, before, after, performedBy, null));
  }

  private static String statusJson(Tenant tenant) {
    TenantStatus status = tenant.getStatus();
    return "{\"status\":\"" + status.name() + "\"}";
  }

  private static String requireActor(String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("performedBy is required");
    }
    String actor = value.trim();
    if (actor.length() > ACTOR_MAX_LENGTH) {
      throw new IllegalArgumentException("performedBy is too long");
    }
    return actor;
  }
}
