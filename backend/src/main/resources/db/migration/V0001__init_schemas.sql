-- V0001: 스키마 초기화 + 공통 테이블
-- 각 모듈은 별도 스키마로 격리 (크로스 스키마 JOIN 금지)

CREATE SCHEMA IF NOT EXISTS common;
CREATE SCHEMA IF NOT EXISTS hr;
CREATE SCHEMA IF NOT EXISTS finance;
CREATE SCHEMA IF NOT EXISTS inventory;
CREATE SCHEMA IF NOT EXISTS crm;

-- ═══════════════════════════════════════════════════════
-- 테넌트 마스터 (common 스키마 — 테넌트 필터 미적용)
-- ═══════════════════════════════════════════════════════
CREATE SEQUENCE IF NOT EXISTS common.tenant_id_seq START 1 INCREMENT 1;

CREATE TABLE common.tenant (
    id          BIGINT      PRIMARY KEY DEFAULT nextval('common.tenant_id_seq'),
    code        VARCHAR(30) NOT NULL UNIQUE,
    name        VARCHAR(200) NOT NULL,
    plan        VARCHAR(30) NOT NULL DEFAULT 'STANDARD', -- TRIAL / STANDARD / ENTERPRISE
    status      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',   -- ACTIVE / SUSPENDED / TERMINATED
    created_at  TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP   NOT NULL DEFAULT now()
);

-- ═══════════════════════════════════════════════════════
-- 감사 로그 (common 스키마 — 테넌트 필터 미적용)
-- ═══════════════════════════════════════════════════════
CREATE SEQUENCE IF NOT EXISTS common.audit_log_id_seq START 1 INCREMENT 50;

CREATE TABLE common.audit_log (
    id           BIGINT       PRIMARY KEY DEFAULT nextval('common.audit_log_id_seq'),
    tenant_id    BIGINT       NOT NULL,
    entity_type  VARCHAR(100) NOT NULL,
    entity_id    BIGINT       NOT NULL,
    action       VARCHAR(20)  NOT NULL,  -- CREATE / UPDATE / DELETE / VIEW
    before_data  JSONB,
    after_data   JSONB,
    performed_by VARCHAR(100) NOT NULL,
    performed_at TIMESTAMP    NOT NULL DEFAULT now(),
    ip_address   VARCHAR(50)
);

CREATE INDEX idx_audit_log_tenant_entity ON common.audit_log (tenant_id, entity_type, entity_id);
CREATE INDEX idx_audit_log_performed_at  ON common.audit_log (performed_at DESC);

-- ═══════════════════════════════════════════════════════
-- 결재 엔진 (common 스키마)
-- ═══════════════════════════════════════════════════════
CREATE SEQUENCE IF NOT EXISTS common.approval_request_id_seq START 1 INCREMENT 50;
CREATE SEQUENCE IF NOT EXISTS common.approval_step_id_seq    START 1 INCREMENT 50;

CREATE TABLE common.approval_request (
    id            BIGINT       PRIMARY KEY DEFAULT nextval('common.approval_request_id_seq'),
    tenant_id     BIGINT       NOT NULL,
    entity_type   VARCHAR(100) NOT NULL,
    entity_id     BIGINT       NOT NULL,
    title         VARCHAR(200) NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING', -- PENDING/APPROVED/REJECTED/CANCELLED/RETURNED
    requester_id  VARCHAR(100) NOT NULL,
    requested_at  TIMESTAMP    NOT NULL DEFAULT now(),
    completed_at  TIMESTAMP,
    current_step  INT          NOT NULL DEFAULT 1,
    total_steps   INT          NOT NULL,
    -- BaseEntity 공통 컬럼
    version       BIGINT       NOT NULL DEFAULT 0,
    deleted_at    TIMESTAMP,
    created_at    TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT now(),
    created_by    VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by    VARCHAR(100) NOT NULL DEFAULT 'system'
);

CREATE TABLE common.approval_step (
    id                  BIGINT       PRIMARY KEY DEFAULT nextval('common.approval_step_id_seq'),
    approval_request_id BIGINT       NOT NULL REFERENCES common.approval_request(id),
    step_order          INT          NOT NULL,
    step_name           VARCHAR(100) NOT NULL,
    approver_id         VARCHAR(100) NOT NULL,
    status              VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    comment             VARCHAR(1000),
    processed_at        TIMESTAMP
);

CREATE INDEX idx_approval_request_tenant ON common.approval_request (tenant_id, status);
CREATE INDEX idx_approval_request_entity ON common.approval_request (tenant_id, entity_type, entity_id);
