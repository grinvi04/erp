package com.erp.common.security;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.security.dto.TenantUserInviteRequest;
import com.erp.common.security.dto.TenantUserReinviteRequest;
import com.erp.common.security.dto.TenantUserResponse;
import com.erp.common.tenant.TenantContext;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class TenantUserOnboardingService {

  private final TenantUserOnboardingStore store;
  private final TenantIdentityAdminPort identityPort;
  private final PermissionChecker permissionChecker;
  private final IamService iamService;

  public TenantUserOnboardingService(
      TenantUserOnboardingStore store,
      TenantIdentityAdminPort identityPort,
      PermissionChecker permissionChecker,
      IamService iamService) {
    this.store = store;
    this.identityPort = identityPort;
    this.permissionChecker = permissionChecker;
    this.iamService = iamService;
  }

  public TenantUserResponse invite(TenantUserInviteRequest request) {
    requireMutationAccess();
    String email = request.email().trim().toLowerCase(Locale.ROOT);
    String requestKey = request.requestKey().trim();
    validateRoles(request.roleIds());
    TenantUser user = store.begin(email, requestKey, fingerprint(request, email));
    if (user.getStatus() == TenantUserStatus.ACTIVE) {
      return TenantUserResponse.from(user);
    }
    if (user.getStatus() == TenantUserStatus.DISABLED) {
      throw new ErpException(ErrorCode.IDENTITY_CONFLICT);
    }
    boolean identityRecoveryAllowed = user.getStatus() == TenantUserStatus.FAILED;
    if (user.getStatus() == TenantUserStatus.FAILED) {
      user = store.retry(requestKey);
    }

    Long tenantId = TenantContext.requireTenantId();
    String userId = user.getKeycloakUserId();
    try {
      if (userId == null) {
        Optional<TenantIdentityUser> existing = identityPort.findByEmail(email);
        if (existing.isPresent()) {
          TenantIdentityUser identity = existing.orElseThrow();
          if (!identityRecoveryAllowed || !isOwnedIdentity(identity, tenantId, requestKey)) {
            store.markFailed(requestKey, "IDENTITY_CONFLICT");
            throw new ErpException(ErrorCode.IDENTITY_CONFLICT);
          }
          userId = identity.id();
          identityPort.setEnabled(userId, tenantId, true);
        } else {
          TenantIdentityUser created =
              identityPort.createUser(
                  new TenantIdentityCreateRequest(
                      tenantId, email, request.firstName(), request.lastName(), requestKey));
          userId = created == null ? null : created.id();
          if (!isOwnedIdentity(created, tenantId, requestKey)) {
            throw new TenantIdentityAdminException("identity provider returned an invalid user");
          }
        }
      } else {
        identityPort.setEnabled(userId, tenantId, true);
      }
      identityPort.sendInvite(userId, tenantId);
      return TenantUserResponse.from(store.activate(requestKey, userId, request.roleIds()));
    } catch (TenantIdentityAdminException failure) {
      disableAfterFailure(userId, tenantId, failure);
      markFailed(requestKey, "IDENTITY_PROVIDER_UNAVAILABLE", failure);
      throw new ErpException(ErrorCode.IDENTITY_PROVIDER_UNAVAILABLE, failure);
    } catch (ErpException expected) {
      throw expected;
    } catch (RuntimeException failure) {
      disableAfterFailure(userId, tenantId, failure);
      markFailed(requestKey, "LOCAL_ACTIVATION_FAILED", failure);
      throw failure;
    }
  }

  public TenantUserResponse reinvite(Long id, TenantUserReinviteRequest request) {
    requireMutationAccess();
    validateRoles(request.roleIds());
    TenantUser user = store.beginReinvite(id);
    if (user.getStatus() == TenantUserStatus.ACTIVE) {
      try {
        identityPort.sendInvite(requireIdentity(user), TenantContext.requireTenantId());
      } catch (TenantIdentityAdminException failure) {
        throw new ErpException(ErrorCode.IDENTITY_PROVIDER_UNAVAILABLE, failure);
      }
      return TenantUserResponse.from(user);
    }
    Long tenantId = TenantContext.requireTenantId();
    String userId = user.getKeycloakUserId();
    try {
      if (userId == null) {
        Optional<TenantIdentityUser> existing = identityPort.findByEmail(user.getNormalizedEmail());
        if (existing.isPresent()) {
          TenantIdentityUser identity = existing.orElseThrow();
          if (!isOwnedIdentity(identity, tenantId, user.getRequestKey())) {
            store.markFailed(user.getRequestKey(), "IDENTITY_CONFLICT");
            throw new ErpException(ErrorCode.IDENTITY_CONFLICT);
          }
          userId = identity.id();
          identityPort.setEnabled(userId, tenantId, true);
        } else {
          TenantIdentityUser created =
              identityPort.createUser(
                  new TenantIdentityCreateRequest(
                      tenantId, user.getNormalizedEmail(), null, null, user.getRequestKey()));
          userId = created == null ? null : created.id();
          if (!isOwnedIdentity(created, tenantId, user.getRequestKey())) {
            throw new TenantIdentityAdminException("identity provider returned an invalid user");
          }
        }
      } else {
        identityPort.setEnabled(userId, tenantId, true);
      }
      identityPort.sendInvite(userId, tenantId);
      return TenantUserResponse.from(
          store.activate(user.getRequestKey(), userId, request.roleIds()));
    } catch (TenantIdentityAdminException failure) {
      disableAfterFailure(userId, tenantId, failure);
      markFailed(user.getRequestKey(), "IDENTITY_PROVIDER_UNAVAILABLE", failure);
      throw new ErpException(ErrorCode.IDENTITY_PROVIDER_UNAVAILABLE, failure);
    } catch (RuntimeException failure) {
      disableAfterFailure(userId, tenantId, failure);
      markFailed(user.getRequestKey(), "LOCAL_ACTIVATION_FAILED", failure);
      throw failure;
    }
  }

  public void disable(Long id) {
    requireMutationAccess();
    TenantUser user = store.find(id);
    if (user.getStatus() == TenantUserStatus.DISABLED) {
      return;
    }
    if (user.getStatus() != TenantUserStatus.ACTIVE) {
      throw new ErpException(ErrorCode.IDENTITY_CONFLICT);
    }
    String userId = requireIdentity(user);
    iamService.requireManageableUser(userId);
    try {
      identityPort.setEnabled(userId, TenantContext.requireTenantId(), false);
    } catch (TenantIdentityAdminException failure) {
      throw new ErpException(ErrorCode.IDENTITY_PROVIDER_UNAVAILABLE, failure);
    }
    store.disable(id);
  }

  public List<TenantUserResponse> list() {
    permissionChecker.require(Permission.IAM_READ);
    return store.list().stream().map(TenantUserResponse::from).toList();
  }

  private void requireMutationAccess() {
    if (!permissionChecker.hasPermission(Permission.IAM_WRITE)) {
      permissionChecker.require(Permission.IAM_DELEGATE);
    }
  }

  private void validateRoles(Set<Long> roleIds) {
    roleIds.forEach(iamService::getRole);
  }

  private boolean isOwnedIdentity(TenantIdentityUser identity, Long tenantId, String requestKey) {
    return identity != null
        && identity.id() != null
        && !identity.id().isBlank()
        && tenantId.equals(identity.tenantId())
        && requestKey.equals(identity.invitationKey());
  }

  private String requireIdentity(TenantUser user) {
    String userId = user.getKeycloakUserId();
    if (userId == null || userId.isBlank()) {
      throw new ErpException(ErrorCode.IDENTITY_CONFLICT);
    }
    return userId;
  }

  private void disableAfterFailure(String userId, Long tenantId, RuntimeException failure) {
    if (userId == null) {
      return;
    }
    try {
      identityPort.setEnabled(userId, tenantId, false);
    } catch (RuntimeException disableFailure) {
      failure.addSuppressed(disableFailure);
    }
  }

  private void markFailed(String requestKey, String failureCode, RuntimeException failure) {
    try {
      store.markFailed(requestKey, failureCode);
    } catch (RuntimeException stateFailure) {
      failure.addSuppressed(stateFailure);
    }
  }

  private String fingerprint(TenantUserInviteRequest request, String normalizedEmail) {
    String canonical =
        normalizedEmail
            + "\n"
            + normalizeOptional(request.firstName())
            + "\n"
            + normalizeOptional(request.lastName())
            + "\n"
            + request.roleIds().stream()
                .sorted()
                .map(String::valueOf)
                .collect(java.util.stream.Collectors.joining(","));
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException impossible) {
      throw new IllegalStateException("SHA-256 is unavailable", impossible);
    }
  }

  private String normalizeOptional(String value) {
    return value == null ? "" : value.trim();
  }
}
