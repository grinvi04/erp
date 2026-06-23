-- V1001: HR 모듈 초기 스키마
-- 조직 구조: Department(트리) → Position → JobGrade → Employee
-- 휴가: LeavePolicy → LeaveBalance, LeaveRequest

-- ═══════════════════════════════════════════════════════
-- Sequences
-- ═══════════════════════════════════════════════════════
CREATE SEQUENCE IF NOT EXISTS hr.department_id_seq    START 1 INCREMENT 50;
CREATE SEQUENCE IF NOT EXISTS hr.position_id_seq      START 1 INCREMENT 50;
CREATE SEQUENCE IF NOT EXISTS hr.job_grade_id_seq     START 1 INCREMENT 50;
CREATE SEQUENCE IF NOT EXISTS hr.employee_id_seq      START 1 INCREMENT 50;
CREATE SEQUENCE IF NOT EXISTS hr.leave_policy_id_seq  START 1 INCREMENT 50;
CREATE SEQUENCE IF NOT EXISTS hr.leave_balance_id_seq START 1 INCREMENT 50;
CREATE SEQUENCE IF NOT EXISTS hr.leave_request_id_seq START 1 INCREMENT 50;
CREATE SEQUENCE IF NOT EXISTS hr.contract_id_seq      START 1 INCREMENT 50;

-- ═══════════════════════════════════════════════════════
-- 공통 컬럼 매크로 (주석 — 실제 반복 작성)
-- tenant_id, version, deleted_at, created_at, updated_at, created_by, updated_by
-- ═══════════════════════════════════════════════════════

-- 부서 (트리 구조)
CREATE TABLE hr.department (
    id              BIGINT       PRIMARY KEY DEFAULT nextval('hr.department_id_seq'),
    tenant_id       BIGINT       NOT NULL,
    code            VARCHAR(30)  NOT NULL,
    name            VARCHAR(100) NOT NULL,
    parent_id       BIGINT       REFERENCES hr.department(id),
    depth           INT          NOT NULL DEFAULT 0,
    sort_order      INT          NOT NULL DEFAULT 0,
    head_employee_id BIGINT,
    is_active       BOOLEAN      NOT NULL DEFAULT true,
    version         BIGINT       NOT NULL DEFAULT 0,
    deleted_at      TIMESTAMP,
    created_at      TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT now(),
    created_by      VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by      VARCHAR(100) NOT NULL DEFAULT 'system',
    UNIQUE (tenant_id, code)
);

CREATE INDEX idx_dept_tenant_parent ON hr.department (tenant_id, parent_id) WHERE deleted_at IS NULL;

-- 직위 (사원/대리/과장 등)
CREATE TABLE hr.position (
    id          BIGINT       PRIMARY KEY DEFAULT nextval('hr.position_id_seq'),
    tenant_id   BIGINT       NOT NULL,
    code        VARCHAR(30)  NOT NULL,
    name        VARCHAR(100) NOT NULL,
    level_order INT          NOT NULL DEFAULT 0,
    version     BIGINT       NOT NULL DEFAULT 0,
    deleted_at  TIMESTAMP,
    created_at  TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT now(),
    created_by  VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by  VARCHAR(100) NOT NULL DEFAULT 'system',
    UNIQUE (tenant_id, code)
);

-- 직급/호봉
CREATE TABLE hr.job_grade (
    id          BIGINT          PRIMARY KEY DEFAULT nextval('hr.job_grade_id_seq'),
    tenant_id   BIGINT          NOT NULL,
    code        VARCHAR(30)     NOT NULL,
    name        VARCHAR(100)    NOT NULL,
    grade_order INT             NOT NULL DEFAULT 0,
    min_salary  NUMERIC(15, 2),
    max_salary  NUMERIC(15, 2),
    version     BIGINT          NOT NULL DEFAULT 0,
    deleted_at  TIMESTAMP,
    created_at  TIMESTAMP       NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP       NOT NULL DEFAULT now(),
    created_by  VARCHAR(100)    NOT NULL DEFAULT 'system',
    updated_by  VARCHAR(100)    NOT NULL DEFAULT 'system',
    UNIQUE (tenant_id, code)
);

-- 직원 마스터
CREATE TABLE hr.employee (
    id              BIGINT          PRIMARY KEY DEFAULT nextval('hr.employee_id_seq'),
    tenant_id       BIGINT          NOT NULL,
    employee_no     VARCHAR(30)     NOT NULL,
    -- 인적사항 (Embedded PersonalInfo)
    last_name       VARCHAR(50)     NOT NULL,
    first_name      VARCHAR(50)     NOT NULL,
    date_of_birth   DATE,
    gender          VARCHAR(10),
    national_id     VARCHAR(50),
    phone           VARCHAR(30),
    personal_email  VARCHAR(200),
    -- 재직 정보
    department_id   BIGINT          NOT NULL REFERENCES hr.department(id),
    position_id     BIGINT          NOT NULL REFERENCES hr.position(id),
    job_grade_id    BIGINT          REFERENCES hr.job_grade(id),
    hire_date       DATE            NOT NULL,
    termination_date DATE,
    employment_type VARCHAR(20)     NOT NULL, -- REGULAR/CONTRACT/PART_TIME/INTERN/DISPATCH
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE', -- ACTIVE/ON_LEAVE/SUSPENDED/TERMINATED
    base_salary     NUMERIC(15, 2),
    work_email      VARCHAR(200)    NOT NULL,
    manager_id      BIGINT          REFERENCES hr.employee(id),
    version         BIGINT          NOT NULL DEFAULT 0,
    deleted_at      TIMESTAMP,
    created_at      TIMESTAMP       NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT now(),
    created_by      VARCHAR(100)    NOT NULL DEFAULT 'system',
    updated_by      VARCHAR(100)    NOT NULL DEFAULT 'system',
    UNIQUE (tenant_id, employee_no),
    UNIQUE (tenant_id, work_email)
);

CREATE INDEX idx_employee_tenant_dept   ON hr.employee (tenant_id, department_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_employee_tenant_status ON hr.employee (tenant_id, status)        WHERE deleted_at IS NULL;
CREATE INDEX idx_employee_tenant_name   ON hr.employee (tenant_id, last_name, first_name) WHERE deleted_at IS NULL;

-- 계약 이력
CREATE TABLE hr.contract (
    id              BIGINT          PRIMARY KEY DEFAULT nextval('hr.contract_id_seq'),
    tenant_id       BIGINT          NOT NULL,
    employee_id     BIGINT          NOT NULL REFERENCES hr.employee(id),
    contract_type   VARCHAR(20)     NOT NULL, -- REGULAR/CONTRACT/PART_TIME/INTERN/DISPATCH
    start_date      DATE            NOT NULL,
    end_date        DATE,           -- NULL = 무기한
    base_salary     NUMERIC(15, 2),
    position_id     BIGINT          NOT NULL REFERENCES hr.position(id),
    job_grade_id    BIGINT          REFERENCES hr.job_grade(id),
    note            TEXT,
    version         BIGINT          NOT NULL DEFAULT 0,
    deleted_at      TIMESTAMP,
    created_at      TIMESTAMP       NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT now(),
    created_by      VARCHAR(100)    NOT NULL DEFAULT 'system',
    updated_by      VARCHAR(100)    NOT NULL DEFAULT 'system'
);

CREATE INDEX idx_contract_employee ON hr.contract (tenant_id, employee_id) WHERE deleted_at IS NULL;

-- 휴가 정책 (테넌트별 설정)
CREATE TABLE hr.leave_policy (
    id               BIGINT       PRIMARY KEY DEFAULT nextval('hr.leave_policy_id_seq'),
    tenant_id        BIGINT       NOT NULL,
    code             VARCHAR(30)  NOT NULL,
    name             VARCHAR(100) NOT NULL,
    leave_type       VARCHAR(30)  NOT NULL, -- ANNUAL/SICK/PARENTAL/BEREAVEMENT/UNPAID/COMPENSATORY
    annual_days      INT          NOT NULL DEFAULT 0,
    carry_over_days  INT          NOT NULL DEFAULT 0,
    requires_approval BOOLEAN     NOT NULL DEFAULT true,
    min_notice_days  INT          NOT NULL DEFAULT 0,
    version          BIGINT       NOT NULL DEFAULT 0,
    deleted_at       TIMESTAMP,
    created_at       TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP    NOT NULL DEFAULT now(),
    created_by       VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by       VARCHAR(100) NOT NULL DEFAULT 'system',
    UNIQUE (tenant_id, code)
);

-- 직원별 휴가 잔여일 (연도별)
CREATE TABLE hr.leave_balance (
    id              BIGINT          PRIMARY KEY DEFAULT nextval('hr.leave_balance_id_seq'),
    tenant_id       BIGINT          NOT NULL,
    employee_id     BIGINT          NOT NULL REFERENCES hr.employee(id),
    leave_policy_id BIGINT          NOT NULL REFERENCES hr.leave_policy(id),
    year            INT             NOT NULL,
    entitled_days   NUMERIC(5, 1)   NOT NULL DEFAULT 0,
    used_days       NUMERIC(5, 1)   NOT NULL DEFAULT 0,
    carry_over_days NUMERIC(5, 1)   NOT NULL DEFAULT 0,
    version         BIGINT          NOT NULL DEFAULT 0,
    deleted_at      TIMESTAMP,
    created_at      TIMESTAMP       NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT now(),
    created_by      VARCHAR(100)    NOT NULL DEFAULT 'system',
    updated_by      VARCHAR(100)    NOT NULL DEFAULT 'system',
    UNIQUE (tenant_id, employee_id, leave_policy_id, year)
);

-- 휴가 신청
CREATE TABLE hr.leave_request (
    id                  BIGINT          PRIMARY KEY DEFAULT nextval('hr.leave_request_id_seq'),
    tenant_id           BIGINT          NOT NULL,
    employee_id         BIGINT          NOT NULL REFERENCES hr.employee(id),
    leave_policy_id     BIGINT          NOT NULL REFERENCES hr.leave_policy(id),
    start_date          DATE            NOT NULL,
    end_date            DATE            NOT NULL,
    requested_days      NUMERIC(5, 1)   NOT NULL,
    approval_status     VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    reason              VARCHAR(500),
    approval_request_id BIGINT,         -- common.approval_request.id 참조 (FK 없음 — 경계 유지)
    version             BIGINT          NOT NULL DEFAULT 0,
    deleted_at          TIMESTAMP,
    created_at          TIMESTAMP       NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP       NOT NULL DEFAULT now(),
    created_by          VARCHAR(100)    NOT NULL DEFAULT 'system',
    updated_by          VARCHAR(100)    NOT NULL DEFAULT 'system',
    CONSTRAINT chk_leave_dates CHECK (end_date >= start_date)
);

CREATE INDEX idx_leave_request_employee ON hr.leave_request (tenant_id, employee_id, approval_status) WHERE deleted_at IS NULL;
