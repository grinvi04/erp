ALTER TABLE common.tenant_user
    ADD COLUMN request_fingerprint VARCHAR(64);

UPDATE common.tenant_user
SET request_fingerprint = repeat('0', 64)
WHERE request_fingerprint IS NULL;

ALTER TABLE common.tenant_user
    ALTER COLUMN request_fingerprint SET NOT NULL;
