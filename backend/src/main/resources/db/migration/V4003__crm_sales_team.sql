-- 영업팀(CRM 영업조직) — DataScope DEPARTMENT(팀) 스코프 기준. 부서(HR)와 독립한 CRM 평면 팀.
-- forward-only.

CREATE SEQUENCE IF NOT EXISTS crm.sales_team_id_seq        START 1 INCREMENT 50;
CREATE SEQUENCE IF NOT EXISTS crm.sales_team_member_id_seq START 1 INCREMENT 50;

CREATE TABLE crm.sales_team (
    id          BIGINT       PRIMARY KEY DEFAULT nextval('crm.sales_team_id_seq'),
    tenant_id   BIGINT       NOT NULL,
    code        VARCHAR(30)  NOT NULL,
    name        VARCHAR(100) NOT NULL,
    version     BIGINT       NOT NULL DEFAULT 0,
    deleted_at  TIMESTAMP,
    created_at  TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT now(),
    created_by  VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by  VARCHAR(100) NOT NULL DEFAULT 'system',
    UNIQUE (tenant_id, code)
);

CREATE TABLE crm.sales_team_member (
    id          BIGINT       PRIMARY KEY DEFAULT nextval('crm.sales_team_member_id_seq'),
    tenant_id   BIGINT       NOT NULL,
    team_id     BIGINT       NOT NULL REFERENCES crm.sales_team(id) ON DELETE CASCADE,
    user_id     VARCHAR(100) NOT NULL,
    version     BIGINT       NOT NULL DEFAULT 0,
    deleted_at  TIMESTAMP,
    created_at  TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT now(),
    created_by  VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by  VARCHAR(100) NOT NULL DEFAULT 'system',
    UNIQUE (tenant_id, team_id, user_id)
);
CREATE INDEX idx_sales_team_member_user ON crm.sales_team_member (tenant_id, user_id);
