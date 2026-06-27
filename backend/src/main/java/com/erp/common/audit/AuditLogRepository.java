package com.erp.common.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 감사 로그 저장소. AuditLog는 BaseEntity를 상속하지 않아 {@code @TenantId} 자동 필터가 적용되지 않는다 — 조회 쿼리에서 tenantId를
 * 명시적으로 필터링한다(테넌트 격리).
 */
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

  @Query(
      """
        SELECT a FROM AuditLog a
        WHERE a.tenantId = :tenantId
          AND (:entityType IS NULL OR a.entityType = :entityType)
          AND (:entityId IS NULL OR a.entityId = :entityId)
          AND (:performedBy IS NULL OR a.performedBy = :performedBy)
        ORDER BY a.performedAt DESC
        """)
  Page<AuditLog> search(
      @Param("tenantId") Long tenantId,
      @Param("entityType") String entityType,
      @Param("entityId") Long entityId,
      @Param("performedBy") String performedBy,
      Pageable pageable);
}
