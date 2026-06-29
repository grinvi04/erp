package com.erp.common.bulkimport;

import com.erp.common.bulkimport.BulkImportResult.RowError;
import com.erp.common.bulkimport.CsvReader.CsvRow;
import com.erp.common.bulkimport.CsvReader.CsvTable;
import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * 재사용 대량 업로드 골격 — 헤더 검증·행 상한·행별 매핑/검증·파일 내 중복 검출을 공통화한다. all-or-nothing: 행 하나라도 검증 실패하면 {@link
 * Validated#errors()}에 모아 반환하고, 호출 측(엔티티 서비스)은 errors가 비었을 때만 생성한다. 행 매핑/검증 실패는 {@link
 * ErpException}으로 던지고 골격이 행 오류로 수집한다. 헤더 불일치·빈 데이터·상한 초과는 파일 단위 오류라 {@link
 * ErpException}(INVALID_INPUT, 400)으로 던진다.
 */
public final class CsvBulkImport {

  private CsvBulkImport() {}

  /** CsvRow → 생성요청 매핑(+검증). 실패 시 ErpException을 던지면 골격이 행 오류로 수집한다. */
  @FunctionalInterface
  public interface RowMapper<T> {
    T map(CsvRow row);
  }

  /** 검증 결과 — 유효 항목과 행 오류. errors가 비면 전부 유효(생성 가능). */
  public record Validated<T>(List<T> items, List<RowError> errors) {}

  /**
   * 표를 검증한다. 헤더가 기대와 불일치·데이터 0행·상한 초과면 ErpException(400). 그 외엔 행별로 mapper로 매핑하며, ErpException은 행
   * 오류로, keyExtractor로 추출한 키의 파일 내 중복도 행 오류로 수집한다.
   */
  public static <T> Validated<T> validate(
      CsvTable table,
      List<String> expectedHeaders,
      int maxRows,
      Function<T, String> keyExtractor,
      RowMapper<T> mapper) {
    if (!table.headers().equals(expectedHeaders)) {
      throw new ErpException(
          ErrorCode.INVALID_INPUT,
          "헤더가 템플릿과 일치하지 않습니다. 기대 헤더: " + String.join(", ", expectedHeaders));
    }
    if (table.rows().isEmpty()) {
      throw new ErpException(ErrorCode.INVALID_INPUT, "데이터 행이 없습니다.");
    }
    if (table.rows().size() > maxRows) {
      throw new ErpException(ErrorCode.INVALID_INPUT, "한 번에 업로드할 수 있는 행은 최대 " + maxRows + "행입니다.");
    }

    List<T> items = new ArrayList<>();
    List<RowError> errors = new ArrayList<>();
    Set<String> seenKeys = new HashSet<>();
    for (CsvRow row : table.rows()) {
      try {
        T item = mapper.map(row);
        String key = keyExtractor.apply(item);
        if (key != null && !seenKeys.add(key)) {
          errors.add(new RowError(row.rowNumber(), "파일 내 중복 키: " + key));
          continue;
        }
        items.add(item);
      } catch (ErpException e) {
        errors.add(new RowError(row.rowNumber(), e.getMessage()));
      }
    }
    return new Validated<>(items, errors);
  }
}
