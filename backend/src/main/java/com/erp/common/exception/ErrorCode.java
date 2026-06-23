package com.erp.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    // 공통
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "C001", "잘못된 입력입니다"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "C002", "요청한 리소스를 찾을 수 없습니다"),
    OPTIMISTIC_LOCK_CONFLICT(HttpStatus.CONFLICT, "C003", "다른 사용자가 먼저 수정했습니다. 최신 데이터를 불러와 다시 시도하세요"),
    TENANT_MISMATCH(HttpStatus.FORBIDDEN, "C004", "접근 권한이 없는 테넌트입니다"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "C005", "접근 권한이 없습니다"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C999", "내부 오류가 발생했습니다"),

    // HR
    EMPLOYEE_NOT_FOUND(HttpStatus.NOT_FOUND, "H001", "직원을 찾을 수 없습니다"),
    EMPLOYEE_ALREADY_TERMINATED(HttpStatus.CONFLICT, "H002", "이미 퇴직 처리된 직원입니다"),
    DEPARTMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "H003", "부서를 찾을 수 없습니다"),
    DEPARTMENT_HAS_MEMBERS(HttpStatus.CONFLICT, "H004", "소속 직원이 있어 부서를 삭제할 수 없습니다"),
    DEPARTMENT_HAS_CHILDREN(HttpStatus.CONFLICT, "H005", "하위 부서가 있어 삭제할 수 없습니다"),
    POSITION_NOT_FOUND(HttpStatus.NOT_FOUND, "H006", "직위를 찾을 수 없습니다"),
    LEAVE_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "H007", "휴가 신청을 찾을 수 없습니다"),
    LEAVE_BALANCE_INSUFFICIENT(HttpStatus.CONFLICT, "H008", "잔여 휴가 일수가 부족합니다"),
    LEAVE_OVERLAP(HttpStatus.CONFLICT, "H009", "동일 기간에 이미 승인된 휴가가 있습니다"),
    CONTRACT_NOT_FOUND(HttpStatus.NOT_FOUND, "H010", "계약 이력을 찾을 수 없습니다"),
    DUPLICATE_CODE(HttpStatus.CONFLICT, "H011", "이미 사용 중인 코드입니다"),
    JOB_GRADE_NOT_FOUND(HttpStatus.NOT_FOUND, "H012", "직급을 찾을 수 없습니다"),
    POSITION_IN_USE(HttpStatus.CONFLICT, "H013", "사용 중인 직위는 삭제할 수 없습니다"),
    JOB_GRADE_IN_USE(HttpStatus.CONFLICT, "H014", "사용 중인 직급은 삭제할 수 없습니다"),

    // Finance
    ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "F001", "계정을 찾을 수 없습니다"),
    JOURNAL_ENTRY_NOT_FOUND(HttpStatus.NOT_FOUND, "F002", "전표를 찾을 수 없습니다"),
    JOURNAL_ENTRY_NOT_BALANCED(HttpStatus.BAD_REQUEST, "F003", "전표의 차변·대변 합계가 일치하지 않습니다"),
    FISCAL_PERIOD_CLOSED(HttpStatus.CONFLICT, "F004", "마감된 회계 기간에는 전표를 생성할 수 없습니다"),
    BUDGET_EXCEEDED(HttpStatus.CONFLICT, "F005", "예산을 초과합니다"),
    VENDOR_NOT_FOUND(HttpStatus.NOT_FOUND, "F006", "공급업체를 찾을 수 없습니다"),
    INVOICE_NOT_FOUND(HttpStatus.NOT_FOUND, "F007", "인보이스를 찾을 수 없습니다"),

    // Inventory
    ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "I001", "품목을 찾을 수 없습니다"),
    WAREHOUSE_NOT_FOUND(HttpStatus.NOT_FOUND, "I002", "창고를 찾을 수 없습니다"),
    INSUFFICIENT_STOCK(HttpStatus.CONFLICT, "I003", "재고가 부족합니다"),
    LOCATION_NOT_FOUND(HttpStatus.NOT_FOUND, "I004", "로케이션을 찾을 수 없습니다"),

    // CRM
    ACCOUNT_COMPANY_NOT_FOUND(HttpStatus.NOT_FOUND, "CR001", "고객사를 찾을 수 없습니다"),
    CONTACT_NOT_FOUND(HttpStatus.NOT_FOUND, "CR002", "담당자를 찾을 수 없습니다"),
    OPPORTUNITY_NOT_FOUND(HttpStatus.NOT_FOUND, "CR003", "영업 기회를 찾을 수 없습니다"),

    // Workflow
    APPROVAL_NOT_FOUND(HttpStatus.NOT_FOUND, "W001", "결재 요청을 찾을 수 없습니다"),
    APPROVAL_ALREADY_PROCESSED(HttpStatus.CONFLICT, "W002", "이미 처리된 결재 요청입니다"),
    APPROVER_NOT_AUTHORIZED(HttpStatus.FORBIDDEN, "W003", "결재 권한이 없습니다");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getHttpStatus() { return httpStatus; }
    public String getCode() { return code; }
    public String getMessage() { return message; }
}
