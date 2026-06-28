package com.erp.common.audit;

import com.erp.common.response.ApiResponse;
import com.erp.common.response.PageResponse;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
      @RequestParam(required = false) AuditLog.AuditAction action,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          LocalDateTime from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          LocalDateTime to,
      @PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(
        ApiResponse.ok(
            PageResponse.from(
                auditService.search(
                    entityType, entityId, performedBy, action, from, to, pageable))));
  }

  /** 감사 로그 단건 상세(변경 내역 before/after 포함). */
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<AuditLogDetailResponse>> findById(@PathVariable Long id) {
    return ResponseEntity.ok(ApiResponse.ok(auditService.findById(id)));
  }

  /** 현재 필터 조건의 감사 로그를 CSV로 내보낸다(text/csv 첨부). */
  @GetMapping(value = "/export", produces = "text/csv")
  public ResponseEntity<String> export(
      @RequestParam(required = false) String entityType,
      @RequestParam(required = false) Long entityId,
      @RequestParam(required = false) String performedBy,
      @RequestParam(required = false) AuditLog.AuditAction action,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          LocalDateTime from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          LocalDateTime to) {
    List<AuditLogResponse> rows =
        auditService.export(entityType, entityId, performedBy, action, from, to);
    String csv = toCsv(rows);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"audit-logs.csv\"")
        .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
        .body(csv);
  }

  private static String toCsv(List<AuditLogResponse> rows) {
    StringBuilder sb = new StringBuilder();
    // Excel 한글 깨짐 방지용 UTF-8 BOM.
    sb.append('﻿');
    sb.append("id,performedAt,entityType,entityId,action,performedBy,ipAddress\n");
    for (AuditLogResponse r : rows) {
      sb.append(r.id())
          .append(',')
          .append(csvField(r.performedAt() == null ? "" : r.performedAt().toString()))
          .append(',')
          .append(csvField(r.entityType()))
          .append(',')
          .append(r.entityId())
          .append(',')
          .append(csvField(r.action() == null ? "" : r.action().name()))
          .append(',')
          .append(csvField(r.performedBy()))
          .append(',')
          .append(csvField(r.ipAddress() == null ? "" : r.ipAddress()))
          .append('\n');
    }
    return sb.toString();
  }

  /** CSV 필드 이스케이프 — 쉼표·따옴표·개행이 있으면 따옴표로 감싸고 내부 따옴표는 두 번 쓴다(RFC 4180). */
  private static String csvField(String value) {
    if (value == null) {
      return "";
    }
    if (value.contains(",")
        || value.contains("\"")
        || value.contains("\n")
        || value.contains("\r")) {
      return "\"" + value.replace("\"", "\"\"") + "\"";
    }
    return value;
  }
}
