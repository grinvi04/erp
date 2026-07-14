CREATE SEQUENCE common.tenant_user_id_seq START 1 INCREMENT 50;

CREATE TABLE common.tenant_user (
    id                BIGINT       PRIMARY KEY DEFAULT nextval('common.tenant_user_id_seq'),
    tenant_id         BIGINT       NOT NULL,
    normalized_email  VARCHAR(320) NOT NULL,
    request_key       VARCHAR(100) NOT NULL,
    keycloak_user_id  VARCHAR(100),
    status            VARCHAR(20)  NOT NULL,
    failure_code      VARCHAR(100),
    version           BIGINT       NOT NULL DEFAULT 0,
    deleted_at        TIMESTAMP,
    created_at        TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at        TIMESTAMP    NOT NULL DEFAULT now(),
    created_by        VARCHAR(100) NOT NULL,
    updated_by        VARCHAR(100) NOT NULL,
    CONSTRAINT ck_tenant_user_status
        CHECK (status IN ('PENDING', 'ACTIVE', 'FAILED', 'DISABLED')),
    CONSTRAINT uq_tenant_user_email UNIQUE (tenant_id, normalized_email),
    CONSTRAINT uq_tenant_user_request UNIQUE (tenant_id, request_key),
    CONSTRAINT uq_tenant_user_identity UNIQUE (tenant_id, keycloak_user_id)
);

CREATE INDEX idx_tenant_user_tenant_status
    ON common.tenant_user (tenant_id, status);
