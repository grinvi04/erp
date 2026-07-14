package com.erp.common.security;

import com.erp.common.audit.AuditLog;
import com.erp.common.audit.AuditService;
import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class JpaTenantUserOnboardingStore implements TenantUserOnboardingStore {

  private final TenantUserRepository repository;
  private final IamService iamService;
  private final AuditService auditService;
  private final ObjectMapper objectMapper;

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public TenantUser begin(String email, String requestKey, String requestFingerprint) {
    TenantUser candidate = TenantUser.pending(email, requestKey, requestFingerprint);
    var sameRequest = repository.findByRequestKey(candidate.getRequestKey());
    if (sameRequest.isPresent()) {
      TenantUser existing = sameRequest.orElseThrow();
      if (!existing.getNormalizedEmail().equals(candidate.getNormalizedEmail())
          || !existing.getRequestFingerprint().equals(candidate.getRequestFingerprint())) {
        throw new ErpException(ErrorCode.IDENTITY_CONFLICT);
      }
      return existing;
    }
    if (repository.findByNormalizedEmail(candidate.getNormalizedEmail()).isPresent()) {
      throw new ErpException(ErrorCode.IDENTITY_CONFLICT);
    }
    try {
      TenantUser saved = repository.saveAndFlush(candidate);
      audit(saved, AuditLog.AuditAction.CREATE, null);
      return saved;
    } catch (DataIntegrityViolationException conflict) {
      throw new ErpException(ErrorCode.IDENTITY_CONFLICT, conflict);
    }
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public TenantUser retry(String requestKey) {
    TenantUser user = findByRequestKey(requestKey);
    if (user.getStatus() == TenantUserStatus.FAILED) {
      user.retry();
      audit(user, AuditLog.AuditAction.UPDATE, "RETRY");
    }
    return user;
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public TenantUser activate(String requestKey, String keycloakUserId, Set<Long> roleIds) {
    TenantUser user = findByRequestKey(requestKey);
    user.activate(keycloakUserId);
    roleIds.stream().sorted().forEach(roleId -> iamService.assignRole(keycloakUserId, roleId));
    audit(user, AuditLog.AuditAction.UPDATE, "ACTIVE");
    return user;
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public TenantUser markFailed(String requestKey, String failureCode) {
    TenantUser user = findByRequestKey(requestKey);
    if (user.getStatus() != TenantUserStatus.FAILED) {
      user.fail(failureCode);
      audit(user, AuditLog.AuditAction.UPDATE, failureCode);
    }
    return user;
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public TenantUser beginReinvite(Long id) {
    TenantUser user = find(id);
    if (user.getStatus() == TenantUserStatus.DISABLED) {
      user.beginReinvite();
      audit(user, AuditLog.AuditAction.UPDATE, "REINVITE");
    } else if (user.getStatus() == TenantUserStatus.FAILED) {
      user.retry();
      audit(user, AuditLog.AuditAction.UPDATE, "RETRY");
    } else if (user.getStatus() != TenantUserStatus.ACTIVE) {
      throw new ErpException(ErrorCode.IDENTITY_CONFLICT);
    }
    return user;
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public TenantUser disable(Long id) {
    TenantUser user = find(id);
    String userId = user.getKeycloakUserId();
    iamService.getUserRoles(userId).stream()
        .map(com.erp.common.security.dto.RoleResponse::id)
        .sorted()
        .forEach(roleId -> iamService.unassignRole(userId, roleId));
    user.disable();
    audit(user, AuditLog.AuditAction.UPDATE, "DISABLED");
    return user;
  }

  @Override
  @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
  public TenantUser find(Long id) {
    return repository
        .findById(id)
        .orElseThrow(() -> new ErpException(ErrorCode.RESOURCE_NOT_FOUND));
  }

  @Override
  @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
  public List<TenantUser> list() {
    return repository.findAllByOrderByNormalizedEmailAsc();
  }

  private TenantUser findByRequestKey(String requestKey) {
    return repository
        .findByRequestKey(requestKey)
        .orElseThrow(() -> new ErpException(ErrorCode.RESOURCE_NOT_FOUND));
  }

  private void audit(TenantUser user, AuditLog.AuditAction action, String event) {
    auditService.record(
        "TENANT_USER",
        user.getId(),
        action,
        null,
        json(Map.of("status", user.getStatus().name(), "event", event == null ? "BEGIN" : event)));
  }

  private String json(Map<String, Object> data) {
    try {
      return objectMapper.writeValueAsString(data);
    } catch (JsonProcessingException failure) {
      throw new IllegalStateException("tenant user audit serialization failed", failure);
    }
  }
}
