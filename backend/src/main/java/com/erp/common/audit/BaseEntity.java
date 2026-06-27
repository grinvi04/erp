package com.erp.common.audit;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.TenantId;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

/**
 * 모든 ERP 엔티티의 기반 클래스. - @TenantId: Hibernate 6.4+ 멀티테넌트 자동 필터링 (테넌트 격리) - @Version: 낙관적 잠금 (동시 편집
 * 충돌 감지) - deleted_at: 소프트 삭제 (@SQLRestriction으로 자동 필터) - JPA Auditing: 생성/수정 시각·사용자 자동 기록
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@SQLRestriction("deleted_at IS NULL")
public abstract class BaseEntity {

  @TenantId
  @Column(name = "tenant_id", nullable = false, updatable = false)
  private Long tenantId;

  @Version
  @Column(name = "version", nullable = false)
  private Long version;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
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

  public void softDelete() {
    this.deletedAt = LocalDateTime.now();
  }

  /**
   * HTTP read-modify-write 충돌 감지 — 클라이언트가 폼 로드 시 받은 version과 현재 영속 엔티티의 version이 다르면 그 사이 다른 사용자가
   * 수정한 것이므로 거부한다(lost update 방지). {@code @Version}만으로는 같은 트랜잭션 내 충돌만 잡으므로, 갱신 서비스가 이 메서드로 기대
   * version을 명시 검증한다. 불일치 시 {@link ObjectOptimisticLockingFailureException} →
   * GlobalExceptionHandler가 409 OPTIMISTIC_LOCK_CONFLICT로 매핑.
   */
  public void checkVersion(Long expectedVersion) {
    if (expectedVersion == null || !expectedVersion.equals(this.version)) {
      throw new ObjectOptimisticLockingFailureException(getClass().getSimpleName(), null);
    }
  }

  public boolean isDeleted() {
    return this.deletedAt != null;
  }

  public Long getTenantId() {
    return tenantId;
  }

  public Long getVersion() {
    return version;
  }

  public LocalDateTime getDeletedAt() {
    return deletedAt;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public String getUpdatedBy() {
    return updatedBy;
  }
}
