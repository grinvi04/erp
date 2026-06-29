-- V2014: 고정자산·감가상각(#7) — 자산대장(취득원가·내용연수·상각방법)과 상각 이력.
-- 월별 감가상각비/감가상각누계액 GL 자동분개, 처분 시 처분손익. 회계상 상각만.
-- tenant_base_currency에 감가상각·처분 계정 FK 4개 추가(미설정이면 상각/처분 분개 차단).

CREATE SEQUENCE IF NOT EXISTS finance.fixed_asset_id_seq        START 1 INCREMENT 50;
CREATE SEQUENCE IF NOT EXISTS finance.depreciation_entry_id_seq START 1 INCREMENT 50;

CREATE TABLE finance.fixed_asset (
    id                       BIGINT       PRIMARY KEY DEFAULT nextval('finance.fixed_asset_id_seq'),
    tenant_id                BIGINT       NOT NULL,
    code                     VARCHAR(30)  NOT NULL,
    name                     VARCHAR(200) NOT NULL,
    acquisition_date         DATE         NOT NULL,
    acquisition_cost         NUMERIC(20, 2) NOT NULL,
    residual_value           NUMERIC(20, 2) NOT NULL,
    useful_life_months       INT          NOT NULL,
    method                   VARCHAR(20)  NOT NULL,
    declining_annual_rate    NUMERIC(6, 4),
    asset_account_id         BIGINT       NOT NULL REFERENCES finance.account(id),
    accumulated_depreciation NUMERIC(20, 2) NOT NULL DEFAULT 0,
    status                   VARCHAR(20)  NOT NULL,
    version                  BIGINT       NOT NULL DEFAULT 0,
    deleted_at               TIMESTAMP,
    created_at               TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at               TIMESTAMP    NOT NULL DEFAULT now(),
    created_by               VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by               VARCHAR(100) NOT NULL DEFAULT 'system',
    UNIQUE (tenant_id, code)
);

CREATE TABLE finance.depreciation_entry (
    id               BIGINT       PRIMARY KEY DEFAULT nextval('finance.depreciation_entry_id_seq'),
    tenant_id        BIGINT       NOT NULL,
    fixed_asset_id   BIGINT       NOT NULL REFERENCES finance.fixed_asset(id),
    fiscal_period_id BIGINT       NOT NULL REFERENCES finance.fiscal_period(id),
    amount           NUMERIC(20, 2) NOT NULL,
    journal_entry_id BIGINT,
    version          BIGINT       NOT NULL DEFAULT 0,
    deleted_at       TIMESTAMP,
    created_at       TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP    NOT NULL DEFAULT now(),
    created_by       VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by       VARCHAR(100) NOT NULL DEFAULT 'system'
);

-- 같은 자산·기간 중복 상각 차단(멱등). 소프트삭제는 제외.
CREATE UNIQUE INDEX uq_depreciation_asset_period
    ON finance.depreciation_entry (tenant_id, fixed_asset_id, fiscal_period_id)
    WHERE deleted_at IS NULL;

-- 감가상각·처분 계정 설정(테넌트 1행). 미설정이면 상각/처분 분개를 차단한다.
ALTER TABLE finance.tenant_base_currency
    ADD COLUMN depreciation_expense_account_id     BIGINT,
    ADD COLUMN accumulated_depreciation_account_id BIGINT,
    ADD COLUMN disposal_gain_account_id            BIGINT,
    ADD COLUMN disposal_loss_account_id            BIGINT;

ALTER TABLE finance.tenant_base_currency
    ADD CONSTRAINT fk_tbc_depreciation_expense_account
        FOREIGN KEY (depreciation_expense_account_id) REFERENCES finance.account(id),
    ADD CONSTRAINT fk_tbc_accumulated_depreciation_account
        FOREIGN KEY (accumulated_depreciation_account_id) REFERENCES finance.account(id),
    ADD CONSTRAINT fk_tbc_disposal_gain_account
        FOREIGN KEY (disposal_gain_account_id) REFERENCES finance.account(id),
    ADD CONSTRAINT fk_tbc_disposal_loss_account
        FOREIGN KEY (disposal_loss_account_id) REFERENCES finance.account(id);
