package com.erp.common.bulkimport;

import java.util.List;

/**
 * 대량 업로드 결과 — 총 데이터 행수·생성 건수·실패 행 목록. all-or-nothing 정책상 실패가 하나라도 있으면 importedCount=0이며 errors에 실패한
 * 모든 행의 [행번호, 사유]가 담긴다.
 */
public record BulkImportResult(int totalRows, int importedCount, List<RowError> errors) {

  /** 실패 행 — 파일 줄번호(헤더=1)·사유. */
  public record RowError(int rowNumber, String message) {}

  public boolean hasErrors() {
    return !errors.isEmpty();
  }

  public static BulkImportResult failed(int totalRows, List<RowError> errors) {
    return new BulkImportResult(totalRows, 0, errors);
  }

  public static BulkImportResult imported(int totalRows) {
    return new BulkImportResult(totalRows, totalRows, List.of());
  }
}
