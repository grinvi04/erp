package com.erp.common.security;

/**
 * 기능 권한 코드 — {모듈}:{리소스}:{액션}. (auth-standards: 코드는 권한을 검사하고
 * 역할은 운영이 관리 화면에서 조합한다 — 코드에 역할명 하드코딩 금지)
 *
 * <p>권한 부여는 JWT의 {@code permissions} 클레임으로 전달되며(Keycloak이 역할→권한
 * 매핑을 발급), {@link JwtAuthoritiesConverter}가 authority로 변환한다. 도메인 결재선
 * (승인 권한)은 RBAC가 아니라 도메인 업무 규칙으로 구현한다(auth-standards).
 */
public final class Permission {

    private Permission() {}

    // HR
    public static final String HR_EMPLOYEE_READ = "hr:employee:read";
    public static final String HR_EMPLOYEE_WRITE = "hr:employee:write";
    public static final String HR_DEPARTMENT_READ = "hr:department:read";
    public static final String HR_DEPARTMENT_WRITE = "hr:department:write";
    public static final String HR_LEAVE_READ = "hr:leave:read";
    public static final String HR_LEAVE_WRITE = "hr:leave:write";
    public static final String HR_POSITION_READ = "hr:position:read";
    public static final String HR_POSITION_WRITE = "hr:position:write";
    public static final String HR_JOBGRADE_READ = "hr:jobgrade:read";
    public static final String HR_JOBGRADE_WRITE = "hr:jobgrade:write";

    // Finance
    public static final String FINANCE_READ = "finance:read";
    public static final String FINANCE_WRITE = "finance:write";

    // Inventory
    public static final String INVENTORY_READ = "inventory:read";
    public static final String INVENTORY_WRITE = "inventory:write";

    // CRM
    public static final String CRM_READ = "crm:read";
    public static final String CRM_WRITE = "crm:write";

    // Audit (감사 로그 — 운영·감사자 권한. 누가 무엇을 변경/결재했는지 조회)
    public static final String AUDIT_READ = "audit:read";
}
