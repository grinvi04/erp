package com.erp.common.observability;

import static org.assertj.core.api.Assertions.assertThat;

import com.erp.common.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

/**
 * TraceIdFilter end-to-end: permitAll 엔드포인트(/actuator/health)에서 X-Trace-Id 응답 헤더가 생성/전파되는지 검증.
 * (test 프로파일 = spring.security.enabled=false)
 */
class TraceIdIntegrationTest extends AbstractIntegrationTest {

  @Autowired private TestRestTemplate restTemplate;

  @Test
  void responseCarriesGeneratedTraceIdHeader() {
    ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);

    String traceId = response.getHeaders().getFirst(TraceIdFilter.TRACE_ID_HEADER);
    assertThat(traceId).isNotNull();
    assertThat(traceId).matches("[0-9a-f]{32}");
  }

  @Test
  void inboundTraceparentIsReflectedInResponseHeader() {
    String expectedTraceId = "4bf92f3577b34da6a3ce929d0e0e4736";
    HttpHeaders headers = new HttpHeaders();
    headers.set(TraceIdFilter.TRACEPARENT_HEADER, "00-" + expectedTraceId + "-00f067aa0ba902b7-01");

    ResponseEntity<String> response =
        restTemplate.exchange(
            "/actuator/health", HttpMethod.GET, new HttpEntity<>(headers), String.class);

    assertThat(response.getHeaders().getFirst(TraceIdFilter.TRACE_ID_HEADER))
        .isEqualTo(expectedTraceId);
  }
}
