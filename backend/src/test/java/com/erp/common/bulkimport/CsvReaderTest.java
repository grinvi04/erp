package com.erp.common.bulkimport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.erp.common.bulkimport.CsvReader.CsvTable;
import com.erp.common.exception.ErpException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class CsvReaderTest {

  private static InputStream in(String csv) {
    return new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
  }

  @Test
  void parse_headerAndRows_mappedByColumn() {
    CsvTable t = CsvReader.parse(in("code,name\nC1,갑상사\nC2,을상사\n"));

    assertThat(t.headers()).containsExactly("code", "name");
    assertThat(t.rows()).hasSize(2);
    assertThat(t.rows().get(0).get("code")).isEqualTo("C1");
    assertThat(t.rows().get(0).get("name")).isEqualTo("갑상사");
    assertThat(t.rows().get(0).rowNumber()).isEqualTo(2); // 헤더=1, 첫 데이터행=2
    assertThat(t.rows().get(1).rowNumber()).isEqualTo(3);
  }

  @Test
  void parse_stripsUtf8Bom() {
    CsvTable t = CsvReader.parse(in("﻿code,name\nC1,갑\n"));
    assertThat(t.headers()).containsExactly("code", "name"); // BOM 제거됨
  }

  @Test
  void parse_quotedFieldWithComma() {
    CsvTable t = CsvReader.parse(in("code,name\nC1,\"갑, 을, 병\"\n"));
    assertThat(t.rows().get(0).get("name")).isEqualTo("갑, 을, 병");
  }

  @Test
  void parse_escapedQuotes() {
    CsvTable t = CsvReader.parse(in("code,name\nC1,\"\"\"따옴표\"\"\"\n"));
    assertThat(t.rows().get(0).get("name")).isEqualTo("\"따옴표\"");
  }

  @Test
  void parse_missingTrailingFields_emptyString() {
    CsvTable t = CsvReader.parse(in("code,name,memo\nC1,갑\n"));
    assertThat(t.rows().get(0).get("memo")).isEqualTo("");
  }

  @Test
  void parse_trailingBlankLineIgnored() {
    CsvTable t = CsvReader.parse(in("code,name\nC1,갑\n\n"));
    assertThat(t.rows()).hasSize(1);
  }

  @Test
  void parse_emptyFile_throwsInvalidInput() {
    assertThatThrownBy(() -> CsvReader.parse(in(""))).isInstanceOf(ErpException.class);
  }

  @Test
  void parse_unterminatedQuote_throwsInvalidInput() {
    // 미닫힘 따옴표(Excel 다중행 셀) → 조용한 흡수 대신 거부.
    assertThatThrownBy(() -> CsvReader.parse(in("code,name\nC1,\"열린따옴표\n")))
        .isInstanceOf(ErpException.class);
  }

  @Test
  void parse_extraFields_throwsInvalidInput() {
    // 미이스케이프 콤마로 열이 헤더보다 많으면 거부(조용한 오매핑 방지).
    assertThatThrownBy(() -> CsvReader.parse(in("code,name\nC1,갑,을,병\n")))
        .isInstanceOf(ErpException.class);
  }

  @Test
  void parse_midFieldQuoteIsLiteral() {
    // 필드 중간 따옴표는 리터럴(인용 시작 아님).
    CsvTable t = CsvReader.parse(in("code,name\nC1,ab\"cd\n"));
    assertThat(t.rows().get(0).get("name")).isEqualTo("ab\"cd");
  }

  @Test
  void get_trimsWhitespace() {
    CsvTable t = CsvReader.parse(in("code,name\n  C1  ,  갑  \n"));
    assertThat(t.rows().get(0).get("code")).isEqualTo("C1");
    assertThat(t.rows().get(0).get("name")).isEqualTo("갑");
  }
}
