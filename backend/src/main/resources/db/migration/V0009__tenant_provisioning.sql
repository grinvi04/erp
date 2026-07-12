ALTER TABLE common.tenant
    ADD COLUMN admin_user_id VARCHAR(100),
    ADD COLUMN provisioning_error VARCHAR(1000),
    ADD COLUMN provisioning_attempted_at TIMESTAMP;

ALTER TABLE common.tenant
    ADD CONSTRAINT chk_tenant_plan
        CHECK (plan IN ('TRIAL', 'STANDARD', 'ENTERPRISE')),
    ADD CONSTRAINT chk_tenant_status
        CHECK (status IN ('PROVISIONING', 'ACTIVE', 'SUSPENDED', 'FAILED', 'TERMINATED'));

CREATE INDEX idx_tenant_status ON common.tenant (status);
