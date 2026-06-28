package com.erp.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * 버그3 재현(RED) — 클라이언트 입력오류가 400이 아닌 500으로 응답된다.
 *
 * <p>{@code GlobalExceptionHandler}에 {@code HttpMessageNotReadableException}(잘못된 enum 값·깨진
 * JSON)·{@code MethodArgumentTypeMismatchException}(path var 타입 불일치) 핸들러가 없어 모두 마지막 {@code
 * Exception} 핸들러로 떨어져 500 C999가 된다. 서버 결함이 아니라 클라이언트 입력 오류이므로 4xx여야 한다.
 *
 * <p>web 계층 결함이라 서비스 직접 호출로는 재현 불가 — 실제 엔드포인트로 HTTP 호출(test 프로파일은 security permitAll이라 인증 불필요).
 *
 * <p>계약: 지금은 의도적으로 RED(AssertionError) — 위 두 예외 핸들러를 추가해 INVALID_INPUT(400)으로 매핑하면 GREEN.
 */
class P0InvalidInputStatusIntegrationTest extends AbstractIntegrationTest {

  @LocalServerPort private int port;
  @Autowired private TestRestTemplate restTemplate;

  private ResponseEntity<String> post(String path, String body) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return restTemplate.postForEntity(
        "http://localhost:" + port + path, new HttpEntity<>(body, headers), String.class);
  }

  @Test
  void invalidEnumValue_returns4xx_not500() {
    // accountType:"NOPE" — 정의되지 않은 enum 값 → 역직렬화 실패(HttpMessageNotReadableException).
    String body =
        "{\"code\":\"1000\",\"name\":\"현금\",\"accountType\":\"NOPE\","
            + "\"normalBalance\":\"DEBIT\",\"parentId\":null,\"isSummary\":false}";

    ResponseEntity<String> response = post("/api/finance/accounts", body);

    // 버그 증명: 잘못된 입력은 4xx여야 한다 — 현재는 500이라 FAIL.
    assertThat(response.getStatusCode().value())
        .as("잘못된 enum 값(클라이언트 오류)은 4xx여야 한다 (현재 500)")
        .isBetween(400, 499);
  }

  @Test
  void malformedJson_returns4xx_not500() {
    // 깨진 JSON(닫는 중괄호 없음) → HttpMessageNotReadableException.
    String body = "{\"code\":\"1000\",\"name\":\"현금\"";

    ResponseEntity<String> response = post("/api/finance/accounts", body);

    assertThat(response.getStatusCode().value())
        .as("깨진 JSON(클라이언트 오류)은 4xx여야 한다 (현재 500)")
        .isBetween(400, 499);
  }

  @Test
  void pathVariableTypeMismatch_returns4xx_not500() {
    // /accounts/abc — Long path var에 문자열 → MethodArgumentTypeMismatchException.
    ResponseEntity<String> response =
        restTemplate.exchange(
            "http://localhost:" + port + "/api/finance/accounts/abc",
            HttpMethod.GET,
            null,
            String.class);

    assertThat(response.getStatusCode().value())
        .as("path var 타입 불일치(클라이언트 오류)는 4xx여야 한다 (현재 500)")
        .isBetween(400, 499);
  }
}
