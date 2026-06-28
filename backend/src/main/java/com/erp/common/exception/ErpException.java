package com.erp.common.exception;

public class ErpException extends RuntimeException {

  private final ErrorCode errorCode;

  public ErpException(ErrorCode errorCode) {
    super(errorCode.getMessage());
    this.errorCode = errorCode;
  }

  public ErpException(ErrorCode errorCode, String detail) {
    super(detail);
    this.errorCode = errorCode;
  }

  public ErpException(ErrorCode errorCode, Throwable cause) {
    super(errorCode.getMessage(), cause);
    this.errorCode = errorCode;
  }

  public ErrorCode getErrorCode() {
    return errorCode;
  }
}
