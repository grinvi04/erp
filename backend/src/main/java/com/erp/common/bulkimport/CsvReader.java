package com.erp.common.bulkimport;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 대량 업로드용 경량 CSV 파서 — RFC4180 부분집합. UTF-8 BOM 제거, 따옴표 필드·따옴표 내 콤마·{@code ""} 이스케이프를 처리한다. 필드 내
 * 개행(다중행 필드)은 미지원(각 줄 = 한 행). 첫 줄을 헤더로 보고 각 데이터 행을 컬럼명→값 맵으로 노출한다. 신규 의존성 없이 flat 표 데이터를 안전히 읽기 위한
 * 소규모 구현.
 */
public final class CsvReader {

  private static final char BOM = '﻿';

  private CsvReader() {}

  /** 헤더 + 데이터 행. rowNumber는 파일 줄번호(헤더=1, 첫 데이터행=2). */
  public record CsvTable(List<String> headers, List<CsvRow> rows) {}

  /** 데이터 행 한 줄 — 컬럼명으로 값 조회(trim). */
  public record CsvRow(int rowNumber, Map<String, String> values) {
    public String get(String column) {
      String v = values.get(column);
      return v != null ? v.trim() : "";
    }
  }

  /** CSV 입력을 파싱한다. 빈 파일(헤더조차 없음)이면 INVALID_INPUT. */
  public static CsvTable parse(InputStream in) {
    List<String> lines = readLines(in);
    if (lines.isEmpty()) {
      throw new ErpException(ErrorCode.INVALID_INPUT, "빈 파일입니다. 헤더와 데이터 행이 필요합니다.");
    }
    List<String> headers = parseLine(lines.get(0), 1);
    List<CsvRow> rows = new ArrayList<>();
    for (int i = 1; i < lines.size(); i++) {
      String line = lines.get(i);
      if (line.isBlank()) {
        continue; // 끝의 빈 줄 무시
      }
      List<String> fields = parseLine(line, i + 1);
      // 필드 수가 헤더보다 많으면 미이스케이프 콤마로 열이 밀린 것 — 조용한 오매핑 대신 거부(부족은 빈값 허용).
      if (fields.size() > headers.size()) {
        throw new ErpException(
            ErrorCode.INVALID_INPUT,
            (i + 1) + "번째 행의 열 수가 헤더(" + headers.size() + ")보다 많습니다. 값에 콤마가 있으면 따옴표로 감싸세요.");
      }
      Map<String, String> map = new LinkedHashMap<>();
      for (int c = 0; c < headers.size(); c++) {
        map.put(headers.get(c), c < fields.size() ? fields.get(c) : "");
      }
      rows.add(new CsvRow(i + 1, map));
    }
    return new CsvTable(headers, rows);
  }

  private static List<String> readLines(InputStream in) {
    List<String> lines = new ArrayList<>();
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
      String line;
      boolean first = true;
      while ((line = reader.readLine()) != null) {
        if (first) {
          if (!line.isEmpty() && line.charAt(0) == BOM) {
            line = line.substring(1); // UTF-8 BOM 제거
          }
          first = false;
        }
        lines.add(line);
      }
    } catch (IOException e) {
      throw new ErpException(ErrorCode.INVALID_INPUT, "파일을 읽을 수 없습니다.");
    }
    return lines;
  }

  /**
   * 한 줄을 필드 목록으로 — 따옴표는 <b>필드 시작에서만</b> 인용을 연다(필드 중간 따옴표는 리터럴). 인용 내 콤마는 보존, {@code ""}는 리터럴 따옴표. 줄
   * 끝까지 인용이 닫히지 않으면(필드 내 줄바꿈=다중행 셀, 미지원) 오류로 거부한다.
   */
  private static List<String> parseLine(String line, int lineNumber) {
    List<String> fields = new ArrayList<>();
    StringBuilder field = new StringBuilder();
    boolean inQuotes = false;
    for (int i = 0; i < line.length(); i++) {
      char ch = line.charAt(i);
      if (inQuotes) {
        if (ch == '"') {
          if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
            field.append('"'); // "" → "
            i++;
          } else {
            inQuotes = false;
          }
        } else {
          field.append(ch);
        }
      } else if (ch == '"' && field.length() == 0) {
        inQuotes = true; // 필드 시작에서만 인용 시작
      } else if (ch == ',') {
        fields.add(field.toString());
        field.setLength(0);
      } else {
        field.append(ch); // 필드 중간 따옴표는 리터럴
      }
    }
    if (inQuotes) {
      throw new ErpException(
          ErrorCode.INVALID_INPUT, lineNumber + "번째 행: 따옴표가 닫히지 않았습니다(필드 내 줄바꿈은 지원하지 않습니다).");
    }
    fields.add(field.toString());
    return fields;
  }
}
