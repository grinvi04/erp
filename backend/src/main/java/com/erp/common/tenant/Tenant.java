package com.erp.common.tenant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import java.util.Locale;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "tenant", schema = "common")
@EntityListeners(AuditingEntityListener.class)
@SQLDelete(
    sql =
        "UPDATE common.tenant SET deleted_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP, "
            + "updated_by = 'system', version = version + 1 WHERE id = ? AND version = ?")
@SQLRestriction("deleted_at IS NULL")
public class Tenant {

  private static final int CODE_MAX_LENGTH = 30;
  private static final int NAME_MAX_LENGTH = 200;
  private static final int ADMIN_USER_ID_MAX_LENGTH = 100;
  private static final int PROVISIONING_ERROR_MAX_LENGTH = 1000;

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tenant_seq")
  @SequenceGenerator(name = "tenant_seq", sequenceName = "common.tenant_id_seq", allocationSize = 1)
  private Long id;

  @Version
  @Column(nullable = false)
  private Long version;

  @Column(nullable = false, unique = true, length = 30)
  private String code;

  @Column(nullable = false, length = 200)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private TenantPlan plan;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private TenantStatus status;

  @Column(name = "admin_user_id", length = 100)
  private String adminUserId;

  @Column(name = "provisioning_error", length = 1000)
  private String provisioningError;

  @Column(name = "provisioning_attempted_at")
  private LocalDateTime provisioningAttemptedAt;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  @CreatedDate
  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @CreatedBy
  @Column(name = "created_by", nullable = false, updatable = false, length = 100)
  private String createdBy;

  @LastModifiedBy
  @Column(name = "updated_by", nullable = false, length = 100)
  private String updatedBy;

  protected Tenant() {}

  public static Tenant startProvisioning(
      String code, String name, TenantPlan plan, String adminUserId) {
    Tenant tenant = new Tenant();
    tenant.code = normalizeCode(code);
    tenant.name = requireText(name, "name", NAME_MAX_LENGTH);
    tenant.plan = java.util.Objects.requireNonNull(plan, "plan");
    tenant.adminUserId = requireText(adminUserId, "adminUserId", ADMIN_USER_ID_MAX_LENGTH);
    tenant.status = TenantStatus.PROVISIONING;
    tenant.provisioningAttemptedAt = LocalDateTime.now();
    tenant.touch();
    return tenant;
  }

  public void activate() {
    requireStatus(TenantStatus.PROVISIONING);
    status = TenantStatus.ACTIVE;
    provisioningError = null;
    touch();
  }

  public void fail(String error) {
    requireStatus(TenantStatus.PROVISIONING);
    status = TenantStatus.FAILED;
    provisioningError = requireText(error, "error", PROVISIONING_ERROR_MAX_LENGTH);
    touch();
  }

  public void retry() {
    requireStatus(TenantStatus.FAILED);
    status = TenantStatus.PROVISIONING;
    provisioningError = null;
    provisioningAttemptedAt = LocalDateTime.now();
    touch();
  }

  public void suspend() {
    requireStatus(TenantStatus.ACTIVE);
    status = TenantStatus.SUSPENDED;
    touch();
  }

  private void requireStatus(TenantStatus expected) {
    if (status != expected) {
      throw new IllegalStateException("tenant status must be " + expected + " but was " + status);
    }
  }

  private static String normalizeCode(String value) {
    String normalized = requireText(value, "code", CODE_MAX_LENGTH).toUpperCase(Locale.ROOT);
    if (!normalized.matches("[A-Z0-9][A-Z0-9_-]{0,29}")) {
      throw new IllegalArgumentException("invalid tenant code");
    }
    return normalized;
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

  @PrePersist
  private void prePersist() {
    if (createdAt == null) {
      createdAt = LocalDateTime.now();
    }
    if (updatedAt == null) {
      updatedAt = createdAt;
    }
  }

  private void touch() {
    updatedAt = LocalDateTime.now();
  }

  public Long getId() {
    return id;
  }

  public Long getVersion() {
    return version;
  }

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public TenantPlan getPlan() {
    return plan;
  }

  public TenantStatus getStatus() {
    return status;
  }

  public String getAdminUserId() {
    return adminUserId;
  }

  public String getProvisioningError() {
    return provisioningError;
  }

  public LocalDateTime getProvisioningAttemptedAt() {
    return provisioningAttemptedAt;
  }
}
