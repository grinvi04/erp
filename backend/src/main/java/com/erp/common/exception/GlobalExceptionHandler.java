package com.erp.common.exception;

import com.erp.common.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ErpException.class)
  public ResponseEntity<ApiResponse<Void>> handleErpException(ErpException e) {
    ErrorCode code = e.getErrorCode();
    log.warn("ERP exception [{}] {}", code.getCode(), e.getMessage());
    return ResponseEntity.status(code.getHttpStatus())
        .body(ApiResponse.error(code.getCode(), e.getMessage()));
  }

  @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
  public ResponseEntity<ApiResponse<Void>> handleOptimisticLock(
      ObjectOptimisticLockingFailureException e) {
    ErrorCode code = ErrorCode.OPTIMISTIC_LOCK_CONFLICT;
    log.warn("Optimistic lock conflict: {}", e.getMessage());
    return ResponseEntity.status(code.getHttpStatus())
        .body(ApiResponse.error(code.getCode(), code.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
    String detail =
        e.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining(", "));
    ErrorCode code = ErrorCode.INVALID_INPUT;
    return ResponseEntity.status(code.getHttpStatus())
        .body(ApiResponse.error(code.getCode(), detail));
  }

  @ExceptionHandler({
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class,
    MissingServletRequestParameterException.class,
    ConstraintViolationException.class
  })
  public ResponseEntity<ApiResponse<Void>> handleInvalidInput(Exception e) {
    ErrorCode code = ErrorCode.INVALID_INPUT;
    log.warn("Invalid client input: {}", e.getMessage());
    return ResponseEntity.status(code.getHttpStatus())
        .body(ApiResponse.error(code.getCode(), code.getMessage()));
  }

  /**
   * 존재하지 않는 경로 — 정적 리소스로 폴백하다 매핑 실패한 케이스. 기본 catch-all로 떨어지면 500(C999)으로 잘못 분류되므로 명시적으로 404를 돌려준다.
   */
  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ApiResponse<Void>> handleNoResource(NoResourceFoundException e) {
    ErrorCode code = ErrorCode.RESOURCE_NOT_FOUND;
    log.warn("No resource found: {}", e.getResourcePath());
    return ResponseEntity.status(code.getHttpStatus())
        .body(ApiResponse.error(code.getCode(), code.getMessage()));
  }

  /**
   * DB 무결성 제약 위반 — 주로 동시 요청이 unique 제약을 동시에 통과한 뒤 한쪽 INSERT가 충돌하는 check-then-act 레이스(예: 중복 발행·중복
   * 코드). catch-all로 떨어지면 500(C999)으로 잘못 분류돼 ERROR 알람을 울리므로 명시적으로 409로 매핑한다. 서비스의 사전 존재 검사가 대부분의 경우를
   * 먼저 거르며, 이 핸들러는 레이스의 패자에게 깔끔한 409를 돌려주는 안전망이다.
   */
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException e) {
    ErrorCode code = ErrorCode.DATA_INTEGRITY_CONFLICT;
    log.warn("Data integrity violation: {}", e.getMostSpecificCause().getMessage());
    return ResponseEntity.status(code.getHttpStatus())
        .body(ApiResponse.error(code.getCode(), code.getMessage()));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception e) {
    log.error("Unexpected error", e);
    ErrorCode code = ErrorCode.INTERNAL_ERROR;
    return ResponseEntity.status(code.getHttpStatus())
        .body(ApiResponse.error(code.getCode(), code.getMessage()));
  }
}
