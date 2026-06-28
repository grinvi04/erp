package com.erp.common.exception;

import com.erp.common.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
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

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception e) {
    log.error("Unexpected error", e);
    ErrorCode code = ErrorCode.INTERNAL_ERROR;
    return ResponseEntity.status(code.getHttpStatus())
        .body(ApiResponse.error(code.getCode(), code.getMessage()));
  }
}
