package com.erp.common.audit;

import java.time.LocalDateTime;

/**
 * 감사 로그 조회 응답. 목록 응답이 비대해지지 않도록 before/after JSON 본문은 제외한다.
 */
public record AuditLogResponse(
    Long id,
    String entityType,
    Long entityId,
    AuditLog.AuditAction action,
    String performedBy,
    LocalDateTime performedAt,
    String ipAddress
) {
    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(
            log.getId(), log.getEntityType(), log.getEntityId(), log.getAction(),
            log.getPerformedBy(), log.getPerformedAt(), log.getIpAddress());
    }
}
