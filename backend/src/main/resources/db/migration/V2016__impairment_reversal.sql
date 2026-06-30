-- V2016: 자산 손상차손 환입(reversal) — 손상 사유 해소 시 한도(손상 없었을 경우 장부금액) 내 환입.
-- impairment_entry에 유형(IMPAIRMENT/REVERSAL) 추가, 멱등 UNIQUE를 유형 포함으로 변경(같은 기간 인식+환입 공존).
-- tenant_base_currency에 손상차손환입(수익) 계정 FK 추가.

ALTER TABLE finance.impairment_entry
    ADD COLUMN entry_type VARCHAR(20) NOT NULL DEFAULT 'IMPAIRMENT';

-- 멱등 키를 (자산,기간,유형)으로 — 같은 기간에 인식과 환입이 각각 1건씩 가능.
DROP INDEX IF EXISTS finance.uq_impairment_asset_period;
CREATE UNIQUE INDEX uq_impairment_asset_period_type
    ON finance.impairment_entry (tenant_id, fixed_asset_id, fiscal_period_id, entry_type)
    WHERE deleted_at IS NULL;

-- 손상차손환입(수익) 계정 설정. 미설정이면 환입 분개를 차단한다.
ALTER TABLE finance.tenant_base_currency
    ADD COLUMN impairment_reversal_account_id BIGINT;

ALTER TABLE finance.tenant_base_currency
    ADD CONSTRAINT fk_tbc_impairment_reversal_account
        FOREIGN KEY (impairment_reversal_account_id) REFERENCES finance.account(id);
