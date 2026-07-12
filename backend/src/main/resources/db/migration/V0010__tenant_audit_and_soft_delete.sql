ALTER TABLE common.tenant
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN deleted_at TIMESTAMP,
    ADD COLUMN created_by VARCHAR(100) NOT NULL DEFAULT 'migration',
    ADD COLUMN updated_by VARCHAR(100) NOT NULL DEFAULT 'migration';
