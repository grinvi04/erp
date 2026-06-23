-- 고객사당 주 담당자(is_primary=true)는 1명만 허용.
-- 멀티테넌트 격리: (tenant_id, account_id) 기준으로 제약.
-- 소프트 삭제된 행은 제외 (deleted_at IS NULL).
CREATE UNIQUE INDEX uq_crm_contact_primary
    ON crm.contact (tenant_id, account_id)
    WHERE is_primary = true AND deleted_at IS NULL;
