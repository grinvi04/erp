package com.erp.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.erp.common.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  void handleDataIntegrity_returns409Conflict() {
    // 동시 unique 위반(레이스 패자)이 500 C999가 아니라 409로 매핑되어야 한다.
    ResponseEntity<ApiResponse<Void>> response =
        handler.handleDataIntegrity(
            new DataIntegrityViolationException("duplicate key value violates unique constraint"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().success()).isFalse();
    assertThat(response.getBody().error().code()).isEqualTo("C006");
  }

  @Test
  void handleUnexpected_returns500() {
    // 분류되지 않은 예외는 여전히 500.
    ResponseEntity<ApiResponse<Void>> response =
        handler.handleUnexpected(new RuntimeException("boom"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody().error().code()).isEqualTo("C999");
  }
}
