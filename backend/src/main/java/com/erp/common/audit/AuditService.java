package com.erp.common.audit;

import com.erp.common.security.CurrentUserProvider;
import com.erp.common.security.Permission;
import com.erp.common.security.PermissionChecker;
import com.erp.common.tenant.TenantContext;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 감사 로그 기록·조회. 결재·중요 변경 등 추적이 필요한 업무 작업의 진입점에서 호출한다. */
@Service
@RequiredArgsConstructor
public class AuditService {

  private final AuditLogRepository auditLogRepository;
  private final CurrentUserProvider currentUserProvider;
  private final PermissionChecker permissionChecker;

  /**
   * 감사 로그를 호출자의 트랜잭션 안에서 기록한다 — 업무 작업이 커밋되면 감사도 함께 커밋되고, 롤백되면 감사도 롤백돼 "실제로 일어나지 않은 일"이 로그에 남지 않는다.
   * (별도 트랜잭션을 열지 않으므로 fail-closed: 감사 저장 실패 시 업무 작업도 롤백.)
   */
  public void record(
      String entityType,
      Long entityId,
      AuditLog.AuditAction action,
      String beforeData,
      String afterData) {
    String performedBy = currentUserProvider.getCurrentUserId();
    if (performedBy == null) {
      performedBy = "system";
    }
    AuditLog log =
        AuditLog.of(
            TenantContext.requireTenantId(),
            entityType,
            entityId,
            action,
            beforeData,
            afterData,
            performedBy,
            null);
    auditLogRepository.save(log);
  }

  /** CSV 내보내기 1회 상한 — 한 번의 다운로드가 메모리·응답을 압도하지 않도록 제한한다. */
  private static final int EXPORT_MAX_ROWS = 10_000;

  @Transactional(readOnly = true)
  public Page<AuditLogResponse> search(
      String entityType,
      Long entityId,
      String performedBy,
      AuditLog.AuditAction action,
      LocalDateTime from,
      LocalDateTime to,
      Pageable pageable) {
    permissionChecker.require(Permission.AUDIT_READ);
    return auditLogRepository
        .search(
            TenantContext.requireTenantId(),
            entityType,
            entityId,
            performedBy,
            action,
            from,
            to,
            pageable)
        .map(AuditLogResponse::from);
  }

  /** 현재 필터 조건의 감사 로그를 CSV 내보내기용으로 반환한다(최신순, {@link #EXPORT_MAX_ROWS} 상한). */
  @Transactional(readOnly = true)
  public List<AuditLogResponse> export(
      String entityType,
      Long entityId,
      String performedBy,
      AuditLog.AuditAction action,
      LocalDateTime from,
      LocalDateTime to) {
    permissionChecker.require(Permission.AUDIT_READ);
    return auditLogRepository
        .search(
            TenantContext.requireTenantId(),
            entityType,
            entityId,
            performedBy,
            action,
            from,
            to,
            PageRequest.of(0, EXPORT_MAX_ROWS))
        .map(AuditLogResponse::from)
        .getContent();
  }
}
