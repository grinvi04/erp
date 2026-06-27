package com.erp.common.audit;

import com.erp.common.response.ApiResponse;
import com.erp.common.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 감사 로그 조회 API. 운영·감사자 전용({@code audit:read}) — 누가 무엇을 언제 결재/변경했는지. */
@RestController
@RequestMapping("/api/audit/logs")
@RequiredArgsConstructor
public class AuditLogController {

  private final AuditService auditService;

  @GetMapping
  public ResponseEntity<ApiResponse<PageResponse<AuditLogResponse>>> search(
      @RequestParam(required = false) String entityType,
      @RequestParam(required = false) Long entityId,
      @RequestParam(required = false) String performedBy,
      @PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(
        ApiResponse.ok(
            PageResponse.from(auditService.search(entityType, entityId, performedBy, pageable))));
  }
}
