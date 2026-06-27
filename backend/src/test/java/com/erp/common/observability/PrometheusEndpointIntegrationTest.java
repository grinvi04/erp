package com.erp.common.observability;

import static org.assertj.core.api.Assertions.assertThat;

import com.erp.common.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Prometheus 메트릭 엔드포인트 노출 검증. management.endpoints...exposure.include 에 prometheus 추가 +
 * micrometer-registry-prometheus 의존이 있으면 운영에서 200 + Prometheus 텍스트 본문이 나온다. (test 프로파일 = permitAll)
 *
 * <p>Spring Boot 테스트 인프라는 기본적으로 metrics export 를 끄므로(테스트 중 외부 전송 방지), 이 테스트에서만
 * {@code @AutoConfigureObservability} 로 다시 켜서 엔드포인트를 검증한다. 운영 컨텍스트에는 이 비활성화가 적용되지 않는다.
 */
@AutoConfigureObservability
class PrometheusEndpointIntegrationTest extends AbstractIntegrationTest {

  @Autowired private TestRestTemplate restTemplate;

  @Test
  void prometheusEndpointExposesMetricsText() {
    ResponseEntity<String> response =
        restTemplate.getForEntity("/actuator/prometheus", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    // Prometheus 텍스트 노출 포맷: JVM 메모리 메트릭이 항상 한 건 이상 포함된다.
    assertThat(response.getBody()).contains("jvm_memory_used_bytes");
  }
}
