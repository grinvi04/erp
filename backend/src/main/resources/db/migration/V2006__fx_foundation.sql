-- V2006: FX 기반 — 테넌트 기준통화 설정 + 환율(수동·외부 수집)
-- 거래 스냅샷(base_amount)·표시는 후속 PR. 이 마이그레이션은 설정·환율 인프라만.

CREATE SEQUENCE IF NOT EXISTS finance.tenant_base_currency_id_seq START 1 INCREMENT 10;
CREATE SEQUENCE IF NOT EXISTS finance.exchange_rate_id_seq        START 1 INCREMENT 50;

-- 테넌트별 기준통화 (테넌트당 1행, 미설정 시 애플리케이션이 KRW 기본 반환)
CREATE TABLE finance.tenant_base_currency (
    id            BIGINT       PRIMARY KEY DEFAULT nextval('finance.tenant_base_currency_id_seq'),
    tenant_id     BIGINT       NOT NULL,
    base_currency VARCHAR(3)   NOT NULL,
    version       BIGINT       NOT NULL DEFAULT 0,
    deleted_at    TIMESTAMP,
    created_at    TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT now(),
    created_by    VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by    VARCHAR(100) NOT NULL DEFAULT 'system',
    UNIQUE (tenant_id)
);

-- 환율 (from_currency → to_currency, 발효일 기준; 조회는 effective_date ≤ 일자 중 최신)
CREATE TABLE finance.exchange_rate (
    id             BIGINT         PRIMARY KEY DEFAULT nextval('finance.exchange_rate_id_seq'),
    tenant_id      BIGINT         NOT NULL,
    from_currency  VARCHAR(3)     NOT NULL,
    to_currency    VARCHAR(3)     NOT NULL,
    effective_date DATE           NOT NULL,
    rate           NUMERIC(18, 8) NOT NULL,
    version        BIGINT         NOT NULL DEFAULT 0,
    deleted_at     TIMESTAMP,
    created_at     TIMESTAMP      NOT NULL DEFAULT now(),
    updated_at     TIMESTAMP      NOT NULL DEFAULT now(),
    created_by     VARCHAR(100)   NOT NULL DEFAULT 'system',
    updated_by     VARCHAR(100)   NOT NULL DEFAULT 'system',
    UNIQUE (tenant_id, from_currency, to_currency, effective_date)
);

CREATE INDEX idx_exchange_rate_lookup
    ON finance.exchange_rate (tenant_id, from_currency, to_currency, effective_date DESC);
