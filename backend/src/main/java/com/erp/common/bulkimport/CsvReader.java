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
    List<String> headers = parseLine(lines.get(0));
    List<CsvRow> rows = new ArrayList<>();
    for (int i = 1; i < lines.size(); i++) {
      String line = lines.get(i);
      if (line.isBlank()) {
        continue; // 끝의 빈 줄 무시
      }
      List<String> fields = parseLine(line);
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

  /** 한 줄을 필드 목록으로 — 따옴표 필드 내 콤마·"" 이스케이프 처리. */
  private static List<String> parseLine(String line) {
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
      } else if (ch == '"') {
        inQuotes = true;
      } else if (ch == ',') {
        fields.add(field.toString());
        field.setLength(0);
      } else {
        field.append(ch);
      }
    }
    fields.add(field.toString());
    return fields;
  }
}
