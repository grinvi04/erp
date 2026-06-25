-- V2003: AR 모듈 (매출채권/외상매출금)
-- finance.customer, finance.ar_invoice, finance.ar_invoice_line
-- AP 모듈(V2001/V2002)과 대칭 구조 — 차/대변 반전.

-- customer_id_seq는 V2001에서 이미 생성됨
CREATE SEQUENCE IF NOT EXISTS finance.ar_invoice_id_seq        START 1 INCREMENT 50;
CREATE SEQUENCE IF NOT EXISTS finance.ar_invoice_line_id_seq   START 1 INCREMENT 50;

-- 고객 마스터
CREATE TABLE finance.customer (
    id                      BIGINT       PRIMARY KEY DEFAULT nextval('finance.customer_id_seq'),
    tenant_id               BIGINT       NOT NULL,
    code                    VARCHAR(30)  NOT NULL,
    name                    VARCHAR(200) NOT NULL,
    business_no             VARCHAR(30),
    contact_name            VARCHAR(100),
    contact_email           VARCHAR(200),
    contact_phone           VARCHAR(30),
    payment_terms           INT          NOT NULL DEFAULT 30,
    is_active               BOOLEAN      NOT NULL DEFAULT true,
    -- 외상매출금 통제계정(차변) — AR 보조원장이 자동 전기되는 GL 통제계정.
    receivables_account_id  BIGINT       REFERENCES finance.account(id),
    version                 BIGINT       NOT NULL DEFAULT 0,
    deleted_at              TIMESTAMP,
    created_at              TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at              TIMESTAMP    NOT NULL DEFAULT now(),
    created_by              VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by              VARCHAR(100) NOT NULL DEFAULT 'system',
    UNIQUE (tenant_id, code)
);

-- 매출 인보이스 (AR)
CREATE TABLE finance.ar_invoice (
    id                  BIGINT          PRIMARY KEY DEFAULT nextval('finance.ar_invoice_id_seq'),
    tenant_id           BIGINT          NOT NULL,
    invoice_no          VARCHAR(30)     NOT NULL,
    customer_id         BIGINT          NOT NULL REFERENCES finance.customer(id),
    invoice_date        DATE            NOT NULL,
    due_date            DATE            NOT NULL,
    total_amount        NUMERIC(20, 2)  NOT NULL,
    paid_amount         NUMERIC(20, 2)  NOT NULL DEFAULT 0,
    currency            VARCHAR(3)      NOT NULL DEFAULT 'KRW',
    status              VARCHAR(20)     NOT NULL DEFAULT 'DRAFT', -- DRAFT/PENDING_APPROVAL/APPROVED/PAID/CANCELLED
    journal_entry_id    BIGINT,         -- 전표 연결 (승인 시 생성)
    approval_request_id BIGINT,         -- 결재 연결
    note                TEXT,
    version             BIGINT          NOT NULL DEFAULT 0,
    deleted_at          TIMESTAMP,
    created_at          TIMESTAMP       NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP       NOT NULL DEFAULT now(),
    created_by          VARCHAR(100)    NOT NULL DEFAULT 'system',
    updated_by          VARCHAR(100)    NOT NULL DEFAULT 'system',
    UNIQUE (tenant_id, invoice_no)
);

CREATE INDEX idx_ar_invoice_tenant_status ON finance.ar_invoice (tenant_id, status) WHERE deleted_at IS NULL;
CREATE INDEX idx_ar_invoice_due_date      ON finance.ar_invoice (tenant_id, due_date) WHERE deleted_at IS NULL;

-- AR 전표 라인(매출·부가세예수금 등 대변 계정)
CREATE TABLE finance.ar_invoice_line (
    id              BIGINT        PRIMARY KEY DEFAULT nextval('finance.ar_invoice_line_id_seq'),
    tenant_id       BIGINT        NOT NULL,
    ar_invoice_id   BIGINT        NOT NULL REFERENCES finance.ar_invoice(id),
    line_no         INT           NOT NULL,
    account_id      BIGINT        NOT NULL REFERENCES finance.account(id),
    amount          NUMERIC(20,2) NOT NULL,
    description     VARCHAR(500),
    version         BIGINT        NOT NULL DEFAULT 0,
    deleted_at      TIMESTAMP,
    created_at      TIMESTAMP     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP     NOT NULL DEFAULT now(),
    created_by      VARCHAR(100)  NOT NULL DEFAULT 'system',
    updated_by      VARCHAR(100)  NOT NULL DEFAULT 'system'
);
CREATE INDEX idx_ar_invoice_line_invoice ON finance.ar_invoice_line (ar_invoice_id);
