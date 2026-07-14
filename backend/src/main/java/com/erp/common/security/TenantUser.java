package com.erp.common.security;

import com.erp.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.util.Locale;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "tenant_user", schema = "common")
@SQLDelete(
    sql =
        "UPDATE common.tenant_user SET deleted_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP, "
            + "updated_by = 'system', version = version + 1 WHERE id = ? AND version = ?")
@SQLRestriction("deleted_at IS NULL")
public class TenantUser extends BaseEntity {

  private static final int EMAIL_MAX_LENGTH = 320;
  private static final int REQUEST_KEY_MAX_LENGTH = 100;
  private static final int KEYCLOAK_USER_ID_MAX_LENGTH = 100;
  private static final int FAILURE_CODE_MAX_LENGTH = 100;

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tenant_user_seq")
  @SequenceGenerator(
      name = "tenant_user_seq",
      sequenceName = "common.tenant_user_id_seq",
      allocationSize = 50)
  private Long id;

  @Column(name = "normalized_email", nullable = false, length = EMAIL_MAX_LENGTH)
  private String normalizedEmail;

  @Column(name = "request_key", nullable = false, length = REQUEST_KEY_MAX_LENGTH)
  private String requestKey;

  @Column(name = "keycloak_user_id", length = KEYCLOAK_USER_ID_MAX_LENGTH)
  private String keycloakUserId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private TenantUserStatus status;

  @Column(name = "failure_code", length = FAILURE_CODE_MAX_LENGTH)
  private String failureCode;

  protected TenantUser() {}

  public static TenantUser pending(String email, String requestKey) {
    TenantUser user = new TenantUser();
    user.normalizedEmail = requireText(email, "email", EMAIL_MAX_LENGTH).toLowerCase(Locale.ROOT);
    user.requestKey = requireText(requestKey, "requestKey", REQUEST_KEY_MAX_LENGTH);
    user.status = TenantUserStatus.PENDING;
    return user;
  }

  public void activate(String keycloakUserId) {
    String identity = requireText(keycloakUserId, "keycloakUserId", KEYCLOAK_USER_ID_MAX_LENGTH);
    if (this.keycloakUserId != null && !this.keycloakUserId.equals(identity)) {
      throw new IllegalStateException("tenant user identity cannot be changed");
    }
    if (status == TenantUserStatus.ACTIVE) {
      return;
    }
    requireStatus(TenantUserStatus.PENDING);
    this.keycloakUserId = identity;
    this.status = TenantUserStatus.ACTIVE;
    this.failureCode = null;
  }

  public void fail(String failureCode) {
    if (status != TenantUserStatus.PENDING && status != TenantUserStatus.ACTIVE) {
      throw new IllegalStateException("only a pending or active invitation can fail");
    }
    this.failureCode = requireText(failureCode, "failureCode", FAILURE_CODE_MAX_LENGTH);
    this.status = TenantUserStatus.FAILED;
  }

  public void retry() {
    requireStatus(TenantUserStatus.FAILED);
    this.status = TenantUserStatus.PENDING;
    this.failureCode = null;
  }

  public void disable() {
    requireStatus(TenantUserStatus.ACTIVE);
    this.status = TenantUserStatus.DISABLED;
    this.failureCode = null;
  }

  public void beginReinvite() {
    requireStatus(TenantUserStatus.DISABLED);
    this.status = TenantUserStatus.PENDING;
    this.failureCode = null;
  }

  public String getNormalizedEmail() {
    return normalizedEmail;
  }

  public String getRequestKey() {
    return requestKey;
  }

  public String getKeycloakUserId() {
    return keycloakUserId;
  }

  public TenantUserStatus getStatus() {
    return status;
  }

  public String getFailureCode() {
    return failureCode;
  }

  public Long getId() {
    return id;
  }

  private void requireStatus(TenantUserStatus expected) {
    if (status != expected) {
      throw new IllegalStateException(
          "tenant user status must be " + expected + " but was " + status);
    }
  }

  private static String requireText(String value, String field, int maxLength) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " is required");
    }
    String normalized = value.trim();
    if (normalized.length() > maxLength) {
      throw new IllegalArgumentException(field + " is too long");
    }
    return normalized;
  }
}
