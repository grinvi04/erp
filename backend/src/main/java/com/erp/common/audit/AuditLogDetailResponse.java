package com.erp.common.audit;

import java.time.LocalDateTime;

/**
 * 감사 로그 상세 조회 응답. 목록 응답({@link AuditLogResponse})과 달리 변경 내역(before/after JSON 본문)을 포함한다 — 상세 화면에서
 * 무엇이 어떻게 바뀌었는지 열람하기 위함.
 */
public record AuditLogDetailResponse(
    Long id,
    String entityType,
    Long entityId,
    AuditLog.AuditAction action,
    String performedBy,
    LocalDateTime performedAt,
    String ipAddress,
    String beforeData,
    String afterData) {
  public static AuditLogDetailResponse from(AuditLog log) {
    return new AuditLogDetailResponse(
        log.getId(),
        log.getEntityType(),
        log.getEntityId(),
        log.getAction(),
        log.getPerformedBy(),
        log.getPerformedAt(),
        log.getIpAddress(),
        log.getBeforeData(),
        log.getAfterData());
  }
}
