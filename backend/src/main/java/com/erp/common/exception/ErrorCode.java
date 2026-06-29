package com.erp.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

  // 공통
  INVALID_INPUT(HttpStatus.BAD_REQUEST, "C001", "잘못된 입력입니다"),
  RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "C002", "요청한 리소스를 찾을 수 없습니다"),
  OPTIMISTIC_LOCK_CONFLICT(HttpStatus.CONFLICT, "C003", "다른 사용자가 먼저 수정했습니다. 최신 데이터를 불러와 다시 시도하세요"),
  TENANT_MISMATCH(HttpStatus.FORBIDDEN, "C004", "접근 권한이 없는 테넌트입니다"),
  FORBIDDEN(HttpStatus.FORBIDDEN, "C005", "접근 권한이 없습니다"),
  DATA_INTEGRITY_CONFLICT(HttpStatus.CONFLICT, "C006", "데이터 무결성 제약을 위반했습니다 (중복 또는 참조 위반)"),
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
  EMPLOYEE_STATUS_CONFLICT(HttpStatus.CONFLICT, "H015", "유효하지 않은 직원 상태 전이입니다"),
  LEAVE_BALANCE_NOT_FOUND(HttpStatus.NOT_FOUND, "H016", "휴가 잔여 기록을 찾을 수 없습니다"),
  LEAVE_CROSS_YEAR(HttpStatus.BAD_REQUEST, "H017", "연도를 넘어가는 휴가 신청은 허용되지 않습니다. 연도별로 분리해서 신청하세요"),
  DEPARTMENT_CYCLE(HttpStatus.BAD_REQUEST, "H018", "부서를 자기 자신 또는 하위 부서의 하위로 이동할 수 없습니다"),

  // Finance
  ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "F001", "계정을 찾을 수 없습니다"),
  JOURNAL_ENTRY_NOT_FOUND(HttpStatus.NOT_FOUND, "F002", "전표를 찾을 수 없습니다"),
  JOURNAL_ENTRY_NOT_BALANCED(HttpStatus.BAD_REQUEST, "F003", "전표의 차변·대변 합계가 일치하지 않습니다"),
  FISCAL_PERIOD_CLOSED(HttpStatus.CONFLICT, "F004", "마감된 회계 기간에는 전표를 생성할 수 없습니다"),
  BUDGET_EXCEEDED(HttpStatus.CONFLICT, "F005", "예산을 초과합니다"),
  VENDOR_NOT_FOUND(HttpStatus.NOT_FOUND, "F006", "공급업체를 찾을 수 없습니다"),
  INVOICE_NOT_FOUND(HttpStatus.NOT_FOUND, "F007", "인보이스를 찾을 수 없습니다"),
  FISCAL_YEAR_NOT_FOUND(HttpStatus.NOT_FOUND, "F008", "회계연도를 찾을 수 없습니다"),
  FISCAL_PERIOD_NOT_FOUND(HttpStatus.NOT_FOUND, "F009", "회계기간을 찾을 수 없습니다"),
  ACCOUNT_IS_SUMMARY(HttpStatus.CONFLICT, "F010", "집계 계정에는 전표를 기록할 수 없습니다"),
  JOURNAL_ENTRY_NOT_DRAFT(HttpStatus.CONFLICT, "F011", "임시 상태의 전표만 처리할 수 있습니다"),
  VENDOR_CODE_DUPLICATE(HttpStatus.CONFLICT, "F012", "이미 사용 중인 공급업체 코드입니다"),
  ACCOUNT_CODE_DUPLICATE(HttpStatus.CONFLICT, "F013", "이미 사용 중인 계정과목 코드입니다"),
  INVOICE_ALREADY_PROCESSED(HttpStatus.CONFLICT, "F014", "이미 처리된 인보이스입니다"),
  FISCAL_YEAR_ALREADY_CLOSED(HttpStatus.CONFLICT, "F015", "이미 마감된 회계연도입니다"),
  FISCAL_PERIOD_ALREADY_CLOSED(HttpStatus.CONFLICT, "F016", "이미 마감된 회계기간입니다"),
  JOURNAL_ENTRY_ALREADY_POSTED(HttpStatus.CONFLICT, "F017", "이미 전기된 전표입니다"),
  BUDGET_NOT_FOUND(HttpStatus.NOT_FOUND, "F018", "예산 항목을 찾을 수 없습니다"),
  BUDGET_DUPLICATE(HttpStatus.CONFLICT, "F019", "동일한 회계연도·계정·부서 조합의 예산이 이미 존재합니다"),
  ACCOUNT_HAS_CHILDREN(HttpStatus.CONFLICT, "F020", "하위 계정이 있어 비활성화할 수 없습니다"),
  INVOICE_NO_DUPLICATE(HttpStatus.CONFLICT, "F021", "이미 사용 중인 인보이스 번호입니다"),
  FISCAL_YEAR_DUPLICATE(HttpStatus.CONFLICT, "F022", "이미 존재하는 회계연도입니다"),
  FISCAL_PERIOD_DUPLICATE(HttpStatus.CONFLICT, "F023", "이미 존재하는 회계기간 번호입니다"),
  INVOICE_OVERPAYMENT(HttpStatus.BAD_REQUEST, "F024", "지급 금액이 미지급 잔액을 초과합니다"),
  JOURNAL_ENTRY_DATE_OUT_OF_RANGE(HttpStatus.BAD_REQUEST, "F025", "전표 일자가 회계기간 범위를 벗어납니다"),
  JOURNAL_LINE_AMOUNTS_INVALID(HttpStatus.BAD_REQUEST, "F026", "전표 라인은 차변 또는 대변 중 하나만 0보다 커야 합니다"),
  INVOICE_DUE_DATE_INVALID(HttpStatus.BAD_REQUEST, "F027", "만기일은 인보이스 일자 이후여야 합니다"),
  FISCAL_PERIOD_DATE_OUT_OF_RANGE(HttpStatus.BAD_REQUEST, "F028", "회계기간 날짜가 회계연도 범위를 벗어납니다"),
  CUSTOMER_NOT_FOUND(HttpStatus.NOT_FOUND, "F029", "고객을 찾을 수 없습니다"),
  CUSTOMER_CODE_DUPLICATE(HttpStatus.CONFLICT, "F030", "이미 사용 중인 고객 코드입니다"),
  PAYMENT_SELF_FORBIDDEN(HttpStatus.FORBIDDEN, "F031", "본인이 작성한 전표는 지급·수금 처리할 수 없습니다 (직무분리)"),
  JOURNAL_ENTRY_NOT_PENDING_APPROVAL(HttpStatus.CONFLICT, "F032", "결재 상신된 전표만 전기할 수 있습니다"),
  EXCHANGE_RATE_DUPLICATE(HttpStatus.CONFLICT, "F033", "이미 등록된 통화쌍·일자의 환율입니다"),
  CURRENCY_RATE_NOT_FOUND(
      HttpStatus.UNPROCESSABLE_ENTITY, "F034", "해당 통화·일자의 환율이 없어 기준통화로 환산할 수 없습니다"),
  BASE_CURRENCY_CHANGE_NOT_ALLOWED(HttpStatus.CONFLICT, "F035", "이미 환산된 거래가 있어 기준통화를 변경할 수 없습니다"),
  JOURNAL_ENTRY_NOT_POSTED(HttpStatus.CONFLICT, "F036", "전기된 전표만 역분개할 수 있습니다"),
  BUSINESS_NO_INVALID(HttpStatus.BAD_REQUEST, "F037", "사업자등록번호 형식이 올바르지 않습니다"),
  COMPANY_PROFILE_REQUIRED(HttpStatus.CONFLICT, "F038", "회사정보(공급자)가 설정되지 않아 세금계산서를 발행할 수 없습니다"),
  AR_INVOICE_NOT_ISSUABLE(HttpStatus.CONFLICT, "F039", "승인 또는 완납된 매출 인보이스만 세금계산서를 발행할 수 있습니다"),
  TAX_INVOICE_ALREADY_ISSUED(HttpStatus.CONFLICT, "F040", "해당 매출 인보이스의 세금계산서가 이미 발행되었습니다"),
  TAX_INVOICE_NOT_FOUND(HttpStatus.NOT_FOUND, "F041", "세금계산서를 찾을 수 없습니다"),
  TAX_INVOICE_NOT_CANCELLABLE(HttpStatus.CONFLICT, "F042", "발행 상태의 세금계산서만 취소할 수 있습니다"),
  TAX_INVOICE_XML_REQUIRES_ISSUED(HttpStatus.CONFLICT, "F043", "발행 상태의 세금계산서만 XML을 생성할 수 있습니다"),

  // Inventory
  ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "I001", "품목을 찾을 수 없습니다"),
  WAREHOUSE_NOT_FOUND(HttpStatus.NOT_FOUND, "I002", "창고를 찾을 수 없습니다"),
  INSUFFICIENT_STOCK(HttpStatus.CONFLICT, "I003", "재고가 부족합니다"),
  LOCATION_NOT_FOUND(HttpStatus.NOT_FOUND, "I004", "로케이션을 찾을 수 없습니다"),
  UOM_NOT_FOUND(HttpStatus.NOT_FOUND, "I005", "단위를 찾을 수 없습니다"),
  ITEM_CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "I006", "품목 분류를 찾을 수 없습니다"),
  UOM_CODE_DUPLICATE(HttpStatus.CONFLICT, "I007", "이미 사용 중인 단위 코드입니다"),
  ITEM_CATEGORY_CODE_DUPLICATE(HttpStatus.CONFLICT, "I008", "이미 사용 중인 품목 분류 코드입니다"),
  ITEM_SKU_DUPLICATE(HttpStatus.CONFLICT, "I009", "이미 사용 중인 품목 SKU입니다"),
  WAREHOUSE_CODE_DUPLICATE(HttpStatus.CONFLICT, "I010", "이미 사용 중인 창고 코드입니다"),
  LOCATION_CODE_DUPLICATE(HttpStatus.CONFLICT, "I011", "이미 사용 중인 로케이션 코드입니다"),
  MOVEMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "I012", "재고 이동을 찾을 수 없습니다"),
  MOVEMENT_NOT_DRAFT(HttpStatus.CONFLICT, "I013", "임시 상태의 재고 이동만 처리할 수 있습니다"),
  ITEM_CATEGORY_HAS_CHILDREN(HttpStatus.CONFLICT, "I014", "하위 분류가 있어 삭제할 수 없습니다"),
  ITEM_CATEGORY_HAS_ITEMS(HttpStatus.CONFLICT, "I015", "해당 분류에 품목이 존재해 삭제할 수 없습니다"),
  MOVEMENT_NO_UNIT_COST_ZERO(HttpStatus.BAD_REQUEST, "I016", "단가 0은 입력할 수 없습니다"),
  SERIAL_NO_REQUIRED(HttpStatus.BAD_REQUEST, "I017", "시리얼 추적 품목은 시리얼 번호가 필수입니다"),
  LOT_NO_REQUIRED(HttpStatus.BAD_REQUEST, "I018", "로트 추적 품목은 로트 번호가 필수입니다"),
  LOCATION_REQUIRED(HttpStatus.BAD_REQUEST, "I019", "해당 이동 유형에는 위치 정보가 필수입니다"),
  MOVEMENT_APPROVAL_NOT_APPLICABLE(HttpStatus.CONFLICT, "I020", "재고 조정 이동만 결재 상신할 수 있습니다"),
  MOVEMENT_REQUIRES_APPROVAL(HttpStatus.CONFLICT, "I021", "재고 조정 이동은 결재 승인을 거쳐야 확정됩니다"),
  MOVEMENT_NOT_PENDING_APPROVAL(HttpStatus.CONFLICT, "I022", "결재 상신된 재고 이동만 승인할 수 있습니다"),
  UOM_IN_USE(HttpStatus.CONFLICT, "I023", "사용 중인 단위는 삭제할 수 없습니다"),

  // CRM
  ACCOUNT_COMPANY_NOT_FOUND(HttpStatus.NOT_FOUND, "CR001", "고객사를 찾을 수 없습니다"),
  CONTACT_NOT_FOUND(HttpStatus.NOT_FOUND, "CR002", "담당자를 찾을 수 없습니다"),
  OPPORTUNITY_NOT_FOUND(HttpStatus.NOT_FOUND, "CR003", "영업 기회를 찾을 수 없습니다"),
  PIPELINE_STAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "CR004", "파이프라인 스테이지를 찾을 수 없습니다"),
  LEAD_NOT_FOUND(HttpStatus.NOT_FOUND, "CR005", "리드를 찾을 수 없습니다"),
  ACTIVITY_NOT_FOUND(HttpStatus.NOT_FOUND, "CR006", "활동을 찾을 수 없습니다"),
  CRM_ACCOUNT_CODE_DUPLICATE(HttpStatus.CONFLICT, "CR007", "이미 사용 중인 고객사 코드입니다"),
  LEAD_ALREADY_CONVERTED(HttpStatus.CONFLICT, "CR008", "이미 전환된 리드입니다"),
  ACTIVITY_INVALID_STATUS_TRANSITION(HttpStatus.CONFLICT, "CR009", "유효하지 않은 활동 상태 전이입니다"),
  LEAD_ALREADY_CONVERTED_UPDATE(HttpStatus.CONFLICT, "CR010", "전환된 리드는 수정할 수 없습니다"),
  CONTACT_PRIMARY_DUPLICATE(HttpStatus.CONFLICT, "CR011", "해당 고객사에 이미 주 담당자가 존재합니다"),
  PIPELINE_STAGE_IN_USE(HttpStatus.CONFLICT, "CR012", "해당 단계를 사용 중인 영업 기회가 있어 삭제할 수 없습니다"),
  LEAD_CONVERT_STAGE_REQUIRED(HttpStatus.BAD_REQUEST, "CR013", "영업 기회를 생성하려면 파이프라인 단계가 필요합니다"),

  // Workflow
  APPROVAL_NOT_FOUND(HttpStatus.NOT_FOUND, "W001", "결재 요청을 찾을 수 없습니다"),
  APPROVAL_ALREADY_PROCESSED(HttpStatus.CONFLICT, "W002", "이미 처리된 결재 요청입니다"),
  APPROVER_NOT_AUTHORIZED(HttpStatus.FORBIDDEN, "W003", "결재 권한이 없습니다"),
  APPROVAL_LIMIT_EXCEEDED(HttpStatus.FORBIDDEN, "W004", "전결 한도를 초과하는 금액입니다 — 상위 전결권자의 결재가 필요합니다"),
  APPROVER_NOT_RESOLVED(
      HttpStatus.CONFLICT, "W005", "결재자(매니저)가 지정되지 않았습니다 — 신청자의 매니저와 매니저의 로그인 계정을 먼저 지정하세요");

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;

  ErrorCode(HttpStatus httpStatus, String code, String message) {
    this.httpStatus = httpStatus;
    this.code = code;
    this.message = message;
  }

  public HttpStatus getHttpStatus() {
    return httpStatus;
  }

  public String getCode() {
    return code;
  }

  public String getMessage() {
    return message;
  }
}
