-- V4001: CRM 모듈 초기 스키마
-- Account(고객사) → Contact(담당자) → Lead → Opportunity → Quote → Activity

CREATE SEQUENCE IF NOT EXISTS crm.account_id_seq       START 1 INCREMENT 50;
CREATE SEQUENCE IF NOT EXISTS crm.contact_id_seq       START 1 INCREMENT 50;
CREATE SEQUENCE IF NOT EXISTS crm.lead_id_seq          START 1 INCREMENT 50;
CREATE SEQUENCE IF NOT EXISTS crm.opportunity_id_seq   START 1 INCREMENT 50;
CREATE SEQUENCE IF NOT EXISTS crm.activity_id_seq      START 1 INCREMENT 200;
CREATE SEQUENCE IF NOT EXISTS crm.pipeline_stage_id_seq START 1 INCREMENT 20;

-- 영업 파이프라인 스테이지 (테넌트별 설정)
CREATE TABLE crm.pipeline_stage (
    id          BIGINT       PRIMARY KEY DEFAULT nextval('crm.pipeline_stage_id_seq'),
    tenant_id   BIGINT       NOT NULL,
    name        VARCHAR(100) NOT NULL,
    stage_order INT          NOT NULL,
    probability INT          NOT NULL DEFAULT 0, -- 성사 확률 % (예측에 사용)
    is_closed_won  BOOLEAN   NOT NULL DEFAULT false,
    is_closed_lost BOOLEAN   NOT NULL DEFAULT false,
    version     BIGINT       NOT NULL DEFAULT 0,
    deleted_at  TIMESTAMP,
    created_at  TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT now(),
    created_by  VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by  VARCHAR(100) NOT NULL DEFAULT 'system'
);

-- 고객사 (Account)
CREATE TABLE crm.account (
    id              BIGINT       PRIMARY KEY DEFAULT nextval('crm.account_id_seq'),
    tenant_id       BIGINT       NOT NULL,
    code            VARCHAR(30)  NOT NULL,
    name            VARCHAR(200) NOT NULL,
    business_no     VARCHAR(30),
    industry        VARCHAR(100),
    website         VARCHAR(300),
    phone           VARCHAR(30),
    address         VARCHAR(500),
    employee_count  INT,
    annual_revenue  NUMERIC(20, 2),
    account_type    VARCHAR(30)  NOT NULL DEFAULT 'PROSPECT', -- PROSPECT/CUSTOMER/PARTNER/COMPETITOR
    owner_id        VARCHAR(100) NOT NULL, -- 담당 영업 (user sub)
    is_active       BOOLEAN      NOT NULL DEFAULT true,
    version         BIGINT       NOT NULL DEFAULT 0,
    deleted_at      TIMESTAMP,
    created_at      TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT now(),
    created_by      VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by      VARCHAR(100) NOT NULL DEFAULT 'system',
    UNIQUE (tenant_id, code)
);

CREATE INDEX idx_crm_account_tenant_owner ON crm.account (tenant_id, owner_id) WHERE deleted_at IS NULL;

-- 담당자 (Contact)
CREATE TABLE crm.contact (
    id          BIGINT       PRIMARY KEY DEFAULT nextval('crm.contact_id_seq'),
    tenant_id   BIGINT       NOT NULL,
    account_id  BIGINT       NOT NULL REFERENCES crm.account(id),
    last_name   VARCHAR(50)  NOT NULL,
    first_name  VARCHAR(50)  NOT NULL,
    title       VARCHAR(100),
    department  VARCHAR(100),
    email       VARCHAR(200),
    phone       VARCHAR(30),
    mobile      VARCHAR(30),
    is_primary  BOOLEAN      NOT NULL DEFAULT false, -- 주담당자 여부
    version     BIGINT       NOT NULL DEFAULT 0,
    deleted_at  TIMESTAMP,
    created_at  TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT now(),
    created_by  VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by  VARCHAR(100) NOT NULL DEFAULT 'system'
);

CREATE INDEX idx_contact_account ON crm.contact (tenant_id, account_id) WHERE deleted_at IS NULL;

-- 잠재 고객 (Lead)
CREATE TABLE crm.lead (
    id              BIGINT       PRIMARY KEY DEFAULT nextval('crm.lead_id_seq'),
    tenant_id       BIGINT       NOT NULL,
    last_name       VARCHAR(50)  NOT NULL,
    first_name      VARCHAR(50)  NOT NULL,
    company         VARCHAR(200),
    title           VARCHAR(100),
    email           VARCHAR(200),
    phone           VARCHAR(30),
    source          VARCHAR(50),  -- WEB/REFERRAL/EVENT/COLD_CALL 등
    status          VARCHAR(30)   NOT NULL DEFAULT 'NEW', -- NEW/CONTACTED/QUALIFIED/CONVERTED/DISQUALIFIED
    owner_id        VARCHAR(100)  NOT NULL,
    converted_account_id BIGINT  REFERENCES crm.account(id),
    converted_opportunity_id BIGINT,
    converted_at    TIMESTAMP,
    note            TEXT,
    version         BIGINT        NOT NULL DEFAULT 0,
    deleted_at      TIMESTAMP,
    created_at      TIMESTAMP     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP     NOT NULL DEFAULT now(),
    created_by      VARCHAR(100)  NOT NULL DEFAULT 'system',
    updated_by      VARCHAR(100)  NOT NULL DEFAULT 'system'
);

CREATE INDEX idx_lead_tenant_owner ON crm.lead (tenant_id, owner_id, status) WHERE deleted_at IS NULL;

-- 영업 기회 (Opportunity)
CREATE TABLE crm.opportunity (
    id              BIGINT          PRIMARY KEY DEFAULT nextval('crm.opportunity_id_seq'),
    tenant_id       BIGINT          NOT NULL,
    account_id      BIGINT          NOT NULL REFERENCES crm.account(id),
    name            VARCHAR(200)    NOT NULL,
    stage_id        BIGINT          NOT NULL REFERENCES crm.pipeline_stage(id),
    amount          NUMERIC(20, 2),
    currency        VARCHAR(3)      NOT NULL DEFAULT 'KRW',
    close_date      DATE,
    probability     INT             NOT NULL DEFAULT 0,
    owner_id        VARCHAR(100)    NOT NULL,
    source          VARCHAR(50),
    description     TEXT,
    version         BIGINT          NOT NULL DEFAULT 0,
    deleted_at      TIMESTAMP,
    created_at      TIMESTAMP       NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT now(),
    created_by      VARCHAR(100)    NOT NULL DEFAULT 'system',
    updated_by      VARCHAR(100)    NOT NULL DEFAULT 'system'
);

CREATE INDEX idx_opportunity_tenant_stage ON crm.opportunity (tenant_id, stage_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_opportunity_tenant_owner ON crm.opportunity (tenant_id, owner_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_opportunity_close_date   ON crm.opportunity (tenant_id, close_date) WHERE deleted_at IS NULL;

-- 활동 (Activity) — 통화/이메일/미팅/할일
CREATE TABLE crm.activity (
    id              BIGINT       PRIMARY KEY DEFAULT nextval('crm.activity_id_seq'),
    tenant_id       BIGINT       NOT NULL,
    activity_type   VARCHAR(30)  NOT NULL, -- CALL/EMAIL/MEETING/TASK/NOTE
    subject         VARCHAR(300) NOT NULL,
    account_id      BIGINT       REFERENCES crm.account(id),
    contact_id      BIGINT       REFERENCES crm.contact(id),
    opportunity_id  BIGINT       REFERENCES crm.opportunity(id),
    owner_id        VARCHAR(100) NOT NULL,
    due_date        TIMESTAMP,
    completed_at    TIMESTAMP,
    status          VARCHAR(20)  NOT NULL DEFAULT 'OPEN', -- OPEN/COMPLETED/CANCELLED
    description     TEXT,
    version         BIGINT       NOT NULL DEFAULT 0,
    deleted_at      TIMESTAMP,
    created_at      TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT now(),
    created_by      VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by      VARCHAR(100) NOT NULL DEFAULT 'system'
);

CREATE INDEX idx_activity_tenant_owner ON crm.activity (tenant_id, owner_id, status) WHERE deleted_at IS NULL;
CREATE INDEX idx_activity_opportunity  ON crm.activity (tenant_id, opportunity_id) WHERE deleted_at IS NULL;
