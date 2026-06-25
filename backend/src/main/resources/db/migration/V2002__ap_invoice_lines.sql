-- AP 전표 라인(비용/자산·부가세 등 차변 계정) + 공급업체 외상매입금(대변) 통제계정.
-- 실무(SAP reconciliation account / 한국 ERP 거래처별 채무계정): 대변 외상매입금은 전표마다
-- 고르지 않고 공급업체 마스터의 통제계정으로 결정된다(AP 보조원장 ↔ GL 통제계정 일치).
-- 차변(비용/자산·부가세대급금)은 라인별 계정.

-- 공급업체 외상매입금 통제계정(대변) — 미설정 허용(설정된 경우에만 승인 시 자동 분개).
ALTER TABLE finance.vendor ADD COLUMN payables_account_id BIGINT REFERENCES finance.account(id);

CREATE SEQUENCE IF NOT EXISTS finance.ap_invoice_line_id_seq START 1 INCREMENT 50;

CREATE TABLE finance.ap_invoice_line (
    id             BIGINT        PRIMARY KEY DEFAULT nextval('finance.ap_invoice_line_id_seq'),
    tenant_id      BIGINT        NOT NULL,
    ap_invoice_id  BIGINT        NOT NULL REFERENCES finance.ap_invoice(id),
    line_no        INT           NOT NULL,
    account_id     BIGINT        NOT NULL REFERENCES finance.account(id),
    amount         NUMERIC(20,2) NOT NULL,
    description    VARCHAR(500),
    version        BIGINT        NOT NULL DEFAULT 0,
    deleted_at     TIMESTAMP,
    created_at     TIMESTAMP     NOT NULL DEFAULT now(),
    updated_at     TIMESTAMP     NOT NULL DEFAULT now(),
    created_by     VARCHAR(100)  NOT NULL DEFAULT 'system',
    updated_by     VARCHAR(100)  NOT NULL DEFAULT 'system'
);
CREATE INDEX idx_ap_invoice_line_invoice ON finance.ap_invoice_line (ap_invoice_id);
