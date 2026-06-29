-- V2015: 자산 손상차손(impairment) — 고정자산 장부가액이 회수가능액보다 클 때 손상차손 인식.
-- 손상차손비/손상차손누계액 GL 자동분개, 손상 이력(자산,기간 멱등). 손상 후 정액 상각은 잔여내용연수 재배분.
-- fixed_asset에 손상차손누계액·정액 재배분 월상각액 컬럼, tenant_base_currency에 손상 계정 FK 2개 추가.

CREATE SEQUENCE IF NOT EXISTS finance.impairment_entry_id_seq START 1 INCREMENT 50;

CREATE TABLE finance.impairment_entry (
    id                 BIGINT       PRIMARY KEY DEFAULT nextval('finance.impairment_entry_id_seq'),
    tenant_id          BIGINT       NOT NULL,
    fixed_asset_id     BIGINT       NOT NULL REFERENCES finance.fixed_asset(id),
    fiscal_period_id   BIGINT       NOT NULL REFERENCES finance.fiscal_period(id),
    recoverable_amount NUMERIC(20, 2) NOT NULL,
    book_value_before  NUMERIC(20, 2) NOT NULL,
    impairment_loss    NUMERIC(20, 2) NOT NULL,
    journal_entry_id   BIGINT,
    version            BIGINT       NOT NULL DEFAULT 0,
    deleted_at         TIMESTAMP,
    created_at         TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at         TIMESTAMP    NOT NULL DEFAULT now(),
    created_by         VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by         VARCHAR(100) NOT NULL DEFAULT 'system'
);

-- 같은 자산·기간 중복 손상 인식 차단(멱등). 소프트삭제는 제외.
CREATE UNIQUE INDEX uq_impairment_asset_period
    ON finance.impairment_entry (tenant_id, fixed_asset_id, fiscal_period_id)
    WHERE deleted_at IS NULL;

-- 손상차손누계액(장부가액 감액)과 손상 후 정액 재배분 월상각액(손상 시 1회 산정·고정, 정률/비손상은 NULL).
ALTER TABLE finance.fixed_asset
    ADD COLUMN accumulated_impairment          NUMERIC(20, 2) NOT NULL DEFAULT 0,
    ADD COLUMN straight_line_monthly_override  NUMERIC(20, 2);

-- 손상차손 계정 설정(테넌트 1행). 미설정이면 손상 분개를 차단한다.
ALTER TABLE finance.tenant_base_currency
    ADD COLUMN impairment_loss_account_id        BIGINT,
    ADD COLUMN accumulated_impairment_account_id BIGINT;

ALTER TABLE finance.tenant_base_currency
    ADD CONSTRAINT fk_tbc_impairment_loss_account
        FOREIGN KEY (impairment_loss_account_id) REFERENCES finance.account(id),
    ADD CONSTRAINT fk_tbc_accumulated_impairment_account
        FOREIGN KEY (accumulated_impairment_account_id) REFERENCES finance.account(id);
