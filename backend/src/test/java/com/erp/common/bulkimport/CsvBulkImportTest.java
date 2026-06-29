package com.erp.common.bulkimport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.erp.common.bulkimport.CsvBulkImport.Validated;
import com.erp.common.bulkimport.CsvReader.CsvTable;
import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class CsvBulkImportTest {

  private static final List<String> HEADERS = List.of("code", "name");

  private static CsvTable table(String csv) {
    return CsvReader.parse(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));
  }

  private record Row(String code, String name) {}

  /** code/name 필수, name이 "BAD"면 검증 실패(행 오류 시뮬레이션). */
  private static Row mapper(CsvReader.CsvRow r) {
    String code = r.get("code");
    String name = r.get("name");
    if (code.isBlank() || name.isBlank()) {
      throw new ErpException(ErrorCode.INVALID_INPUT, "코드·이름은 필수입니다");
    }
    if (name.equals("BAD")) {
      throw new ErpException(ErrorCode.INVALID_INPUT, "이름이 올바르지 않습니다");
    }
    return new Row(code, name);
  }

  private static Validated<Row> validate(String csv) {
    return CsvBulkImport.validate(table(csv), HEADERS, 1000, Row::code, CsvBulkImportTest::mapper);
  }

  @Test
  void validate_allValid_noErrors() {
    Validated<Row> v = validate("code,name\nC1,갑\nC2,을\n");
    assertThat(v.errors()).isEmpty();
    assertThat(v.items()).hasSize(2);
  }

  @Test
  void validate_rowFailsValidation_collectedAsRowErrorExcludedFromItems() {
    Validated<Row> v = validate("code,name\nC1,갑\nC2,BAD\nC3,병\n");
    assertThat(v.items()).hasSize(2); // 유효 2건
    assertThat(v.errors()).hasSize(1);
    assertThat(v.errors().get(0).rowNumber()).isEqualTo(3); // BAD 행(파일 3번째 줄)
    assertThat(v.errors().get(0).message()).contains("이름이 올바르지 않습니다");
  }

  @Test
  void validate_missingRequired_rowError() {
    Validated<Row> v = validate("code,name\nC1,\n");
    assertThat(v.errors()).hasSize(1);
    assertThat(v.errors().get(0).message()).contains("필수");
  }

  @Test
  void validate_inFileDuplicateKey_rowError() {
    Validated<Row> v = validate("code,name\nC1,갑\nC1,을\n");
    assertThat(v.items()).hasSize(1); // 첫 C1만 유효
    assertThat(v.errors()).hasSize(1);
    assertThat(v.errors().get(0).rowNumber()).isEqualTo(3);
    assertThat(v.errors().get(0).message()).contains("중복");
  }

  @Test
  void validate_headerMismatch_throwsInvalidInput() {
    ErpException ex =
        (ErpException)
            org.junit.jupiter.api.Assertions.assertThrows(
                ErpException.class, () -> validate("code,wrong\nC1,갑\n"));
    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT);
  }

  @Test
  void validate_emptyData_throwsInvalidInput() {
    assertThatThrownBy(() -> validate("code,name\n")).isInstanceOf(ErpException.class);
  }

  @Test
  void validate_overMaxRows_throwsInvalidInput() {
    StringBuilder sb = new StringBuilder("code,name\n");
    for (int i = 0; i < 3; i++) {
      sb.append("C").append(i).append(",갑\n");
    }
    assertThatThrownBy(
            () ->
                CsvBulkImport.validate(
                    table(sb.toString()), HEADERS, 2, Row::code, CsvBulkImportTest::mapper))
        .isInstanceOf(ErpException.class);
  }
}
