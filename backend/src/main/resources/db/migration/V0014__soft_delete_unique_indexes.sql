ALTER TABLE common.tenant
    DROP CONSTRAINT tenant_code_key;

CREATE UNIQUE INDEX uq_tenant_code
    ON common.tenant (code)
    WHERE deleted_at IS NULL;

ALTER TABLE common.tenant_user
    DROP CONSTRAINT uq_tenant_user_email,
    DROP CONSTRAINT uq_tenant_user_request,
    DROP CONSTRAINT uq_tenant_user_identity;

CREATE UNIQUE INDEX uq_tenant_user_email
    ON common.tenant_user (tenant_id, normalized_email)
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX uq_tenant_user_request
    ON common.tenant_user (tenant_id, request_key)
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX uq_tenant_user_identity
    ON common.tenant_user (tenant_id, keycloak_user_id)
    WHERE deleted_at IS NULL;
