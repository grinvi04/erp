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
    // AP 전표 결재권(전결권). 전표 작성·수정(finance:write)과 분리 — 직무분리(작성자≠결재자) 강제.
    public static final String FINANCE_INVOICE_APPROVE = "finance:invoice:approve";
    // 지급·수금 실행권. 작성(finance:write)과 분리 — 현금이동 직무분리. 기존 write 보유 역할엔 V0004로 백필.
    public static final String FINANCE_INVOICE_PAY = "finance:invoice:pay";
    // GL 전표 전기 결재권(전결권). 전표 작성(finance:write)과 분리 — 작성자≠전기결재자 직무분리. 기존 write 보유 역할엔 V0005로 백필.
    public static final String FINANCE_GL_APPROVE = "finance:gl:approve";

    // Inventory
    public static final String INVENTORY_READ = "inventory:read";
    public static final String INVENTORY_WRITE = "inventory:write";

    // CRM
    public static final String CRM_READ = "crm:read";
    public static final String CRM_WRITE = "crm:write";

    // Audit (감사 로그 — 운영·감사자 권한. 누가 무엇을 변경/결재했는지 조회)
    public static final String AUDIT_READ = "audit:read";

    // IAM (역할·권한·배정 관리 — 관리자 전용)
    public static final String IAM_READ = "iam:read";
    public static final String IAM_WRITE = "iam:write";

    /**
     * 정의된 모든 권한 코드 카탈로그 — 관리 화면의 권한 선택지·부트스트랩 슈퍼관리자 역할에 사용.
     * 상수 추가 시 여기에도 반드시 추가한다(단일 출처). {@code IamPermissionCatalogTest}가 누락을 막는다.
     */
    public static java.util.Set<String> all() {
        return java.util.Set.of(
            HR_EMPLOYEE_READ, HR_EMPLOYEE_WRITE, HR_DEPARTMENT_READ, HR_DEPARTMENT_WRITE,
            HR_LEAVE_READ, HR_LEAVE_WRITE, HR_POSITION_READ, HR_POSITION_WRITE,
            HR_JOBGRADE_READ, HR_JOBGRADE_WRITE,
            FINANCE_READ, FINANCE_WRITE, FINANCE_INVOICE_APPROVE, FINANCE_INVOICE_PAY, FINANCE_GL_APPROVE,
            INVENTORY_READ, INVENTORY_WRITE,
            CRM_READ, CRM_WRITE,
            AUDIT_READ,
            IAM_READ, IAM_WRITE);
    }
}
