-- V2001: Finance 모듈 초기 스키마
-- 계정과목 → 전표(GL) → AP(매입채무) / AR(매출채권) → 예산 → 회계기간

CREATE SEQUENCE IF NOT EXISTS finance.fiscal_year_id_seq    START 1 INCREMENT 10;
CREATE SEQUENCE IF NOT EXISTS finance.fiscal_period_id_seq  START 1 INCREMENT 50;
CREATE SEQUENCE IF NOT EXISTS finance.account_id_seq        START 1 INCREMENT 50;
CREATE SEQUENCE IF NOT EXISTS finance.journal_entry_id_seq  START 1 INCREMENT 50;
CREATE SEQUENCE IF NOT EXISTS finance.journal_line_id_seq   START 1 INCREMENT 200;
CREATE SEQUENCE IF NOT EXISTS finance.vendor_id_seq         START 1 INCREMENT 50;
CREATE SEQUENCE IF NOT EXISTS finance.customer_id_seq       START 1 INCREMENT 50;
CREATE SEQUENCE IF NOT EXISTS finance.invoice_id_seq        START 1 INCREMENT 50;
CREATE SEQUENCE IF NOT EXISTS finance.budget_id_seq         START 1 INCREMENT 50;

-- 회계연도
CREATE TABLE finance.fiscal_year (
    id          BIGINT       PRIMARY KEY DEFAULT nextval('finance.fiscal_year_id_seq'),
    tenant_id   BIGINT       NOT NULL,
    year        INT          NOT NULL,
    start_date  DATE         NOT NULL,
    end_date    DATE         NOT NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'OPEN', -- OPEN / CLOSED
    version     BIGINT       NOT NULL DEFAULT 0,
    deleted_at  TIMESTAMP,
    created_at  TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT now(),
    created_by  VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by  VARCHAR(100) NOT NULL DEFAULT 'system',
    UNIQUE (tenant_id, year)
);

-- 회계기간 (월별)
CREATE TABLE finance.fiscal_period (
    id              BIGINT       PRIMARY KEY DEFAULT nextval('finance.fiscal_period_id_seq'),
    tenant_id       BIGINT       NOT NULL,
    fiscal_year_id  BIGINT       NOT NULL REFERENCES finance.fiscal_year(id),
    period_number   INT          NOT NULL, -- 1~12
    start_date      DATE         NOT NULL,
    end_date        DATE         NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'OPEN', -- OPEN / CLOSED / LOCKED
    version         BIGINT       NOT NULL DEFAULT 0,
    deleted_at      TIMESTAMP,
    created_at      TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT now(),
    created_by      VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by      VARCHAR(100) NOT NULL DEFAULT 'system',
    UNIQUE (tenant_id, fiscal_year_id, period_number)
);

-- 계정과목 (트리 구조)
CREATE TABLE finance.account (
    id              BIGINT       PRIMARY KEY DEFAULT nextval('finance.account_id_seq'),
    tenant_id       BIGINT       NOT NULL,
    code            VARCHAR(20)  NOT NULL,
    name            VARCHAR(200) NOT NULL,
    account_type    VARCHAR(30)  NOT NULL, -- ASSET/LIABILITY/EQUITY/REVENUE/EXPENSE
    normal_balance  VARCHAR(10)  NOT NULL, -- DEBIT/CREDIT
    parent_id       BIGINT       REFERENCES finance.account(id),
    is_summary      BOOLEAN      NOT NULL DEFAULT false, -- 집계계정=거래 불가
    is_active       BOOLEAN      NOT NULL DEFAULT true,
    version         BIGINT       NOT NULL DEFAULT 0,
    deleted_at      TIMESTAMP,
    created_at      TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT now(),
    created_by      VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by      VARCHAR(100) NOT NULL DEFAULT 'system',
    UNIQUE (tenant_id, code)
);

-- 전표 헤더 (복식부기)
CREATE TABLE finance.journal_entry (
    id              BIGINT          PRIMARY KEY DEFAULT nextval('finance.journal_entry_id_seq'),
    tenant_id       BIGINT          NOT NULL,
    entry_no        VARCHAR(30)     NOT NULL,          -- 채번: JE-YYYYMM-NNNNN
    entry_date      DATE            NOT NULL,
    fiscal_period_id BIGINT         NOT NULL REFERENCES finance.fiscal_period(id),
    description     VARCHAR(500)    NOT NULL,
    entry_type      VARCHAR(30)     NOT NULL,          -- MANUAL/AP/AR/PAYROLL/ADJUSTMENT
    status          VARCHAR(20)     NOT NULL DEFAULT 'DRAFT', -- DRAFT/POSTED/REVERSED
    total_debit     NUMERIC(20, 2)  NOT NULL DEFAULT 0,
    total_credit    NUMERIC(20, 2)  NOT NULL DEFAULT 0,
    currency        VARCHAR(3)      NOT NULL DEFAULT 'KRW',
    reference_type  VARCHAR(100),   -- 참조 원천 (e.g. AP_INVOICE)
    reference_id    BIGINT,         -- 참조 원천 ID
    posted_at       TIMESTAMP,
    posted_by       VARCHAR(100),
    version         BIGINT          NOT NULL DEFAULT 0,
    deleted_at      TIMESTAMP,
    created_at      TIMESTAMP       NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT now(),
    created_by      VARCHAR(100)    NOT NULL DEFAULT 'system',
    updated_by      VARCHAR(100)    NOT NULL DEFAULT 'system',
    UNIQUE (tenant_id, entry_no),
    CONSTRAINT chk_balanced CHECK (status != 'POSTED' OR total_debit = total_credit)
);

-- 전표 라인 (차/대변)
CREATE TABLE finance.journal_line (
    id              BIGINT          PRIMARY KEY DEFAULT nextval('finance.journal_line_id_seq'),
    tenant_id       BIGINT          NOT NULL,
    journal_entry_id BIGINT         NOT NULL REFERENCES finance.journal_entry(id),
    line_no         INT             NOT NULL,
    account_id      BIGINT          NOT NULL REFERENCES finance.account(id),
    debit_amount    NUMERIC(20, 2)  NOT NULL DEFAULT 0,
    credit_amount   NUMERIC(20, 2)  NOT NULL DEFAULT 0,
    description     VARCHAR(500),
    department_id   BIGINT,         -- HR 부서 참조 (FK 없음 — 모듈 경계)
    CONSTRAINT chk_debit_or_credit CHECK (
        (debit_amount > 0 AND credit_amount = 0) OR
        (credit_amount > 0 AND debit_amount = 0)
    )
);

CREATE INDEX idx_journal_entry_tenant_date  ON finance.journal_entry (tenant_id, entry_date DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_journal_entry_period       ON finance.journal_entry (tenant_id, fiscal_period_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_journal_line_account       ON finance.journal_line (tenant_id, account_id);

-- 공급업체
CREATE TABLE finance.vendor (
    id              BIGINT       PRIMARY KEY DEFAULT nextval('finance.vendor_id_seq'),
    tenant_id       BIGINT       NOT NULL,
    code            VARCHAR(30)  NOT NULL,
    name            VARCHAR(200) NOT NULL,
    business_no     VARCHAR(30),
    contact_name    VARCHAR(100),
    contact_email   VARCHAR(200),
    contact_phone   VARCHAR(30),
    payment_terms   INT          NOT NULL DEFAULT 30, -- 지급 일수
    is_active       BOOLEAN      NOT NULL DEFAULT true,
    version         BIGINT       NOT NULL DEFAULT 0,
    deleted_at      TIMESTAMP,
    created_at      TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT now(),
    created_by      VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by      VARCHAR(100) NOT NULL DEFAULT 'system',
    UNIQUE (tenant_id, code)
);

-- 매입 인보이스 (AP)
CREATE TABLE finance.ap_invoice (
    id              BIGINT          PRIMARY KEY DEFAULT nextval('finance.invoice_id_seq'),
    tenant_id       BIGINT          NOT NULL,
    invoice_no      VARCHAR(30)     NOT NULL,
    vendor_id       BIGINT          NOT NULL REFERENCES finance.vendor(id),
    invoice_date    DATE            NOT NULL,
    due_date        DATE            NOT NULL,
    total_amount    NUMERIC(20, 2)  NOT NULL,
    paid_amount     NUMERIC(20, 2)  NOT NULL DEFAULT 0,
    currency        VARCHAR(3)      NOT NULL DEFAULT 'KRW',
    status          VARCHAR(20)     NOT NULL DEFAULT 'DRAFT', -- DRAFT/PENDING_APPROVAL/APPROVED/PAID/CANCELLED
    journal_entry_id BIGINT,        -- 전표 연결 (승인 시 생성)
    approval_request_id BIGINT,     -- 결재 연결
    note            TEXT,
    version         BIGINT          NOT NULL DEFAULT 0,
    deleted_at      TIMESTAMP,
    created_at      TIMESTAMP       NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT now(),
    created_by      VARCHAR(100)    NOT NULL DEFAULT 'system',
    updated_by      VARCHAR(100)    NOT NULL DEFAULT 'system',
    UNIQUE (tenant_id, invoice_no)
);

CREATE INDEX idx_ap_invoice_tenant_status ON finance.ap_invoice (tenant_id, status) WHERE deleted_at IS NULL;
CREATE INDEX idx_ap_invoice_due_date      ON finance.ap_invoice (tenant_id, due_date) WHERE deleted_at IS NULL;

-- 예산
CREATE TABLE finance.budget (
    id              BIGINT          PRIMARY KEY DEFAULT nextval('finance.budget_id_seq'),
    tenant_id       BIGINT          NOT NULL,
    fiscal_year_id  BIGINT          NOT NULL REFERENCES finance.fiscal_year(id),
    account_id      BIGINT          NOT NULL REFERENCES finance.account(id),
    department_id   BIGINT,         -- 부서별 예산 (HR 참조, FK 없음)
    budget_amount   NUMERIC(20, 2)  NOT NULL,
    actual_amount   NUMERIC(20, 2)  NOT NULL DEFAULT 0, -- 실적 (전표 포스팅 시 갱신)
    version         BIGINT          NOT NULL DEFAULT 0,
    deleted_at      TIMESTAMP,
    created_at      TIMESTAMP       NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT now(),
    created_by      VARCHAR(100)    NOT NULL DEFAULT 'system',
    updated_by      VARCHAR(100)    NOT NULL DEFAULT 'system',
    UNIQUE (tenant_id, fiscal_year_id, account_id, department_id)
);
