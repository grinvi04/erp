package com.erp.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.security.dto.RoleResponse;
import com.erp.common.security.dto.TenantUserInviteRequest;
import com.erp.common.security.dto.TenantUserReinviteRequest;
import com.erp.common.tenant.TenantContext;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TenantUserOnboardingServiceTest {

  @Mock private TenantUserOnboardingStore store;
  @Mock private TenantIdentityAdminPort identityPort;
  @Mock private PermissionChecker permissionChecker;
  @Mock private IamService iamService;

  private TenantUserOnboardingService service;

  @BeforeEach
  void setUp() {
    TenantContext.setTenantId(1L);
    service = new TenantUserOnboardingService(store, identityPort, permissionChecker, iamService);
  }

  @AfterEach
  void clearTenant() {
    TenantContext.clear();
  }

  @Test
  void invite_newUser_sendsInviteBeforeActivatingRoles() {
    var request = request("request-1");
    TenantUser pending = TenantUser.pending(request.email(), request.requestKey());
    TenantUser active = TenantUser.pending(request.email(), request.requestKey());
    active.activate("keycloak-user-1");
    given(permissionChecker.hasPermission(Permission.IAM_WRITE)).willReturn(false);
    given(iamService.getRole(7L))
        .willReturn(new RoleResponse(7L, "FINANCE_USER", "재무 사용자", null, Set.of()));
    given(store.begin(eq(request.email()), eq(request.requestKey()), anyString()))
        .willReturn(pending);
    given(identityPort.findByEmail("admin@example.com")).willReturn(Optional.empty());
    given(identityPort.createUser(any()))
        .willReturn(new TenantIdentityUser("keycloak-user-1", 1L, "request-1", true));
    given(store.activate("request-1", "keycloak-user-1", Set.of(7L))).willReturn(active);

    var result = service.invite(request);

    var order = inOrder(identityPort, store);
    order.verify(identityPort).sendInvite("keycloak-user-1", 1L);
    order.verify(store).activate("request-1", "keycloak-user-1", Set.of(7L));
    assertThat(result.status()).isEqualTo(TenantUserStatus.ACTIVE);
    verify(iamService).getRole(7L);
    verify(permissionChecker).require(Permission.IAM_DELEGATE);
  }

  @Test
  void invite_sameCompletedRequest_returnsExistingResultWithoutExternalMutation() {
    var request = request("request-1");
    TenantUser active = TenantUser.pending(request.email(), request.requestKey());
    active.activate("keycloak-user-1");
    given(permissionChecker.hasPermission(Permission.IAM_WRITE)).willReturn(true);
    given(store.begin(eq(request.email()), eq(request.requestKey()), anyString()))
        .willReturn(active);

    var result = service.invite(request);

    assertThat(result.status()).isEqualTo(TenantUserStatus.ACTIVE);
    verify(iamService, never()).getRole(any());
    verify(identityPort, never()).findByEmail(anyString());
    verify(identityPort, never()).sendInvite(anyString(), any());
    verify(store, never()).activate(anyString(), anyString(), any());
  }

  @Test
  void invite_existingUntrackedIdentity_isRejectedWithoutMutation() {
    var request = request("request-1");
    given(permissionChecker.hasPermission(Permission.IAM_WRITE)).willReturn(true);
    given(iamService.getRole(7L))
        .willReturn(new RoleResponse(7L, "FINANCE_USER", "재무 사용자", null, Set.of()));
    given(store.begin(eq(request.email()), eq(request.requestKey()), anyString()))
        .willReturn(TenantUser.pending(request.email(), request.requestKey()));
    given(identityPort.findByEmail("admin@example.com"))
        .willReturn(Optional.of(new TenantIdentityUser("existing", 1L, "other-request", true)));

    assertThatThrownBy(() -> service.invite(request))
        .isInstanceOfSatisfying(
            ErpException.class,
            exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.IDENTITY_CONFLICT));

    verify(store).markFailed("request-1", "IDENTITY_CONFLICT");
    verify(identityPort, never()).setEnabled(any(), any(), any(Boolean.class));
    verify(store, never()).activate(any(), any(), any());
  }

  @Test
  void invite_matchingIdentityMarker_recoversAmbiguousCreate() {
    var request = request("request-1");
    TenantUser pending = TenantUser.pending(request.email(), request.requestKey());
    TenantUser active = TenantUser.pending(request.email(), request.requestKey());
    active.activate("recovered-user");
    given(permissionChecker.hasPermission(Permission.IAM_WRITE)).willReturn(true);
    given(iamService.getRole(7L))
        .willReturn(new RoleResponse(7L, "FINANCE_USER", "재무 사용자", null, Set.of()));
    given(store.begin(eq(request.email()), eq(request.requestKey()), anyString()))
        .willReturn(pending);
    given(identityPort.findByEmail("admin@example.com"))
        .willReturn(Optional.of(new TenantIdentityUser("recovered-user", 1L, "request-1", false)));
    given(store.activate("request-1", "recovered-user", Set.of(7L))).willReturn(active);

    var result = service.invite(request);

    verify(identityPort).setEnabled("recovered-user", 1L, true);
    verify(identityPort).sendInvite("recovered-user", 1L);
    verify(identityPort, never()).createUser(any());
    assertThat(result.userId()).isEqualTo("recovered-user");
  }

  @Test
  void invite_identityFailure_isDisabledAndRecordedForRetry() {
    var request = request("request-1");
    given(permissionChecker.hasPermission(Permission.IAM_WRITE)).willReturn(true);
    given(iamService.getRole(7L))
        .willReturn(new RoleResponse(7L, "FINANCE_USER", "재무 사용자", null, Set.of()));
    given(store.begin(eq(request.email()), eq(request.requestKey()), anyString()))
        .willReturn(TenantUser.pending(request.email(), request.requestKey()));
    given(identityPort.findByEmail("admin@example.com")).willReturn(Optional.empty());
    given(identityPort.createUser(any()))
        .willReturn(new TenantIdentityUser("keycloak-user-1", 1L, "request-1", true));
    org.mockito.Mockito.doThrow(new TenantIdentityAdminException("smtp unavailable"))
        .when(identityPort)
        .sendInvite("keycloak-user-1", 1L);

    assertThatThrownBy(() -> service.invite(request))
        .isInstanceOfSatisfying(
            ErpException.class,
            exception ->
                assertThat(exception.getErrorCode())
                    .isEqualTo(ErrorCode.IDENTITY_PROVIDER_UNAVAILABLE));

    verify(identityPort).setEnabled("keycloak-user-1", 1L, false);
    verify(store).markFailed("request-1", "IDENTITY_PROVIDER_UNAVAILABLE");
    verify(store, never()).activate(any(), any(), any());
  }

  @Test
  void invite_invalidCreatedIdentity_isDisabledWhenIdIsAvailable() {
    var request = request("request-1");
    given(permissionChecker.hasPermission(Permission.IAM_WRITE)).willReturn(true);
    given(iamService.getRole(7L))
        .willReturn(new RoleResponse(7L, "FINANCE_USER", "재무 사용자", null, Set.of()));
    given(store.begin(eq(request.email()), eq(request.requestKey()), anyString()))
        .willReturn(TenantUser.pending(request.email(), request.requestKey()));
    given(identityPort.findByEmail("admin@example.com")).willReturn(Optional.empty());
    given(identityPort.createUser(any()))
        .willReturn(new TenantIdentityUser("keycloak-user-1", 2L, "request-1", true));

    assertThatThrownBy(() -> service.invite(request))
        .isInstanceOfSatisfying(
            ErpException.class,
            exception ->
                assertThat(exception.getErrorCode())
                    .isEqualTo(ErrorCode.IDENTITY_PROVIDER_UNAVAILABLE));

    verify(identityPort).setEnabled("keycloak-user-1", 1L, false);
    verify(store).markFailed("request-1", "IDENTITY_PROVIDER_UNAVAILABLE");
  }

  @Test
  void reinvite_disabledUser_enablesAndActivatesSameIdentity() {
    TenantUser pending = TenantUser.pending("admin@example.com", "request-1");
    pending.activate("keycloak-user-1");
    pending.disable();
    pending.beginReinvite();
    TenantUser active = TenantUser.pending("admin@example.com", "request-1");
    active.activate("keycloak-user-1");
    given(permissionChecker.hasPermission(Permission.IAM_WRITE)).willReturn(true);
    given(iamService.getRole(7L))
        .willReturn(new RoleResponse(7L, "FINANCE_USER", "재무 사용자", null, Set.of()));
    given(store.beginReinvite(3L)).willReturn(pending);
    given(store.activate("request-1", "keycloak-user-1", Set.of(7L))).willReturn(active);

    var result = service.reinvite(3L, new TenantUserReinviteRequest(Set.of(7L)));

    verify(iamService).requireManageableUser("keycloak-user-1");
    verify(identityPort).setEnabled("keycloak-user-1", 1L, true);
    verify(identityPort).sendInvite("keycloak-user-1", 1L);
    assertThat(result.status()).isEqualTo(TenantUserStatus.ACTIVE);
  }

  @Test
  void reinvite_failedBeforeIdentityCreation_createsIdentityAndActivates() {
    TenantUser pending = TenantUser.pending("admin@example.com", "request-1");
    TenantUser active = TenantUser.pending("admin@example.com", "request-1");
    active.activate("keycloak-user-1");
    given(permissionChecker.hasPermission(Permission.IAM_WRITE)).willReturn(true);
    given(iamService.getRole(7L))
        .willReturn(new RoleResponse(7L, "FINANCE_USER", "재무 사용자", null, Set.of()));
    given(store.beginReinvite(3L)).willReturn(pending);
    given(identityPort.findByEmail("admin@example.com")).willReturn(Optional.empty());
    given(identityPort.createUser(any()))
        .willReturn(new TenantIdentityUser("keycloak-user-1", 1L, "request-1", true));
    given(store.activate("request-1", "keycloak-user-1", Set.of(7L))).willReturn(active);

    var result = service.reinvite(3L, new TenantUserReinviteRequest(Set.of(7L)));

    verify(identityPort).sendInvite("keycloak-user-1", 1L);
    verify(store).activate("request-1", "keycloak-user-1", Set.of(7L));
    assertThat(result.status()).isEqualTo(TenantUserStatus.ACTIVE);
  }

  @Test
  void reinvite_activeUser_isIdempotent() {
    TenantUser active = TenantUser.pending("admin@example.com", "request-1");
    active.activate("keycloak-user-1");
    given(permissionChecker.hasPermission(Permission.IAM_WRITE)).willReturn(true);
    given(iamService.getRole(7L))
        .willReturn(new RoleResponse(7L, "FINANCE_USER", "재무 사용자", null, Set.of()));
    given(store.beginReinvite(3L)).willReturn(active);

    var result = service.reinvite(3L, new TenantUserReinviteRequest(Set.of(7L)));

    assertThat(result.status()).isEqualTo(TenantUserStatus.ACTIVE);
    verify(identityPort, never()).setEnabled(anyString(), any(), any(Boolean.class));
    verify(identityPort, never()).sendInvite(anyString(), any());
  }

  @Test
  void disable_blocksIdentityBeforeRevokingLocalAccess() {
    TenantUser active = TenantUser.pending("admin@example.com", "request-1");
    active.activate("keycloak-user-1");
    given(permissionChecker.hasPermission(Permission.IAM_WRITE)).willReturn(true);
    given(store.find(3L)).willReturn(active);
    given(store.disable(3L)).willReturn(active);

    service.disable(3L);

    verify(iamService).requireManageableUser("keycloak-user-1");
    var order = inOrder(identityPort, store);
    order.verify(identityPort).setEnabled("keycloak-user-1", 1L, false);
    order.verify(store).disable(3L);
  }

  @Test
  void disable_disabledUser_isIdempotent() {
    TenantUser disabled = TenantUser.pending("admin@example.com", "request-1");
    disabled.activate("keycloak-user-1");
    disabled.disable();
    given(permissionChecker.hasPermission(Permission.IAM_WRITE)).willReturn(true);
    given(store.find(3L)).willReturn(disabled);

    service.disable(3L);

    verify(identityPort, never()).setEnabled(anyString(), any(), any(Boolean.class));
    verify(store, never()).disable(any());
  }

  @Test
  void disable_pendingUser_isRejectedWithoutExternalMutation() {
    TenantUser pending = TenantUser.pending("admin@example.com", "request-1");
    given(permissionChecker.hasPermission(Permission.IAM_WRITE)).willReturn(true);
    given(store.find(3L)).willReturn(pending);

    assertThatThrownBy(() -> service.disable(3L))
        .isInstanceOfSatisfying(
            ErpException.class,
            exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.IDENTITY_CONFLICT));

    verify(identityPort, never()).setEnabled(anyString(), any(), any(Boolean.class));
    verify(store, never()).disable(any());
  }

  @Test
  void list_requiresIamRead() {
    given(store.list()).willReturn(List.of());

    assertThat(service.list()).isEmpty();

    verify(permissionChecker).require(Permission.IAM_READ);
  }

  private static TenantUserInviteRequest request(String requestKey) {
    return new TenantUserInviteRequest("admin@example.com", "ERP", "Admin", requestKey, Set.of(7L));
  }
}
