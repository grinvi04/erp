package com.erp.common.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * 감사 로그 — 엔티티 변경 이력 추적.
 * BaseEntity를 상속하지 않음 (테넌트 필터·삭제 필터 적용 제외).
 */
@Entity
@Table(name = "audit_log", schema = "common")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "audit_log_seq")
    @SequenceGenerator(name = "audit_log_seq", sequenceName = "common.audit_log_id_seq", allocationSize = 50)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 20)
    private AuditAction action;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before_data", columnDefinition = "jsonb")
    private String beforeData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_data", columnDefinition = "jsonb")
    private String afterData;

    @Column(name = "performed_by", nullable = false, length = 100)
    private String performedBy;

    @Column(name = "performed_at", nullable = false)
    private LocalDateTime performedAt;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    protected AuditLog() {}

    public static AuditLog of(Long tenantId, String entityType, Long entityId,
                               AuditAction action, String before, String after,
                               String performedBy, String ipAddress) {
        AuditLog log = new AuditLog();
        log.tenantId = tenantId;
        log.entityType = entityType;
        log.entityId = entityId;
        log.action = action;
        log.beforeData = before;
        log.afterData = after;
        log.performedBy = performedBy;
        log.performedAt = LocalDateTime.now();
        log.ipAddress = ipAddress;
        return log;
    }

    public enum AuditAction { CREATE, UPDATE, DELETE, VIEW }

    public Long getId() { return id; }
    public Long getTenantId() { return tenantId; }
    public String getEntityType() { return entityType; }
    public Long getEntityId() { return entityId; }
    public AuditAction getAction() { return action; }
    public String getBeforeData() { return beforeData; }
    public String getAfterData() { return afterData; }
    public String getPerformedBy() { return performedBy; }
    public LocalDateTime getPerformedAt() { return performedAt; }
    public String getIpAddress() { return ipAddress; }
}
