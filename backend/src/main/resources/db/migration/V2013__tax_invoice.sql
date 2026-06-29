-- V2013: 전자세금계산서(#3) — 승인/완납 AR 인보이스에서 발행. 공급자(회사정보)·공급받는자(거래처)·
-- 금액·품목을 발행 시점 스냅샷으로 고정(이후 마스터 변경 무관, 법적 증빙). 품목은 헤더 단일.
-- 1 AR : 1 ISSUED는 부분 유니크 인덱스로 DB에서도 강제(CANCELLED는 무관 → 재발행 허용).

CREATE SEQUENCE IF NOT EXISTS finance.tax_invoice_id_seq START 1 INCREMENT 50;

CREATE TABLE finance.tax_invoice (
    id                      BIGINT       PRIMARY KEY DEFAULT nextval('finance.tax_invoice_id_seq'),
    tenant_id               BIGINT       NOT NULL,
    ar_invoice_id           BIGINT       NOT NULL REFERENCES finance.ar_invoice(id),
    issue_no                VARCHAR(40),
    tax_type                VARCHAR(20)  NOT NULL,
    charge_type             VARCHAR(10)  NOT NULL,
    write_date              DATE         NOT NULL,
    supply_amount           NUMERIC(20, 2) NOT NULL,
    vat_amount              NUMERIC(20, 2) NOT NULL,
    total_amount            NUMERIC(20, 2) NOT NULL,
    item_name               VARCHAR(200) NOT NULL,
    -- 공급자(자사) 스냅샷
    supplier_company_name   VARCHAR(200) NOT NULL,
    supplier_business_no    VARCHAR(30),
    supplier_representative  VARCHAR(100),
    supplier_address        VARCHAR(500),
    supplier_business_type  VARCHAR(200),
    supplier_business_item  VARCHAR(200),
    -- 공급받는자(거래처) 스냅샷
    buyer_company_name      VARCHAR(200) NOT NULL,
    buyer_business_no       VARCHAR(30),
    buyer_representative     VARCHAR(100),
    buyer_address           VARCHAR(500),
    buyer_business_type     VARCHAR(200),
    buyer_business_item     VARCHAR(200),
    status                  VARCHAR(20)  NOT NULL,
    note                    TEXT,
    version                 BIGINT       NOT NULL DEFAULT 0,
    deleted_at              TIMESTAMP,
    created_at              TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at              TIMESTAMP    NOT NULL DEFAULT now(),
    created_by              VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by              VARCHAR(100) NOT NULL DEFAULT 'system'
);

-- 1 AR : 1 ISSUED 세금계산서(중복 발행 차단). CANCELLED·소프트삭제는 제외해 재발행을 허용한다.
CREATE UNIQUE INDEX uq_tax_invoice_ar_issued
    ON finance.tax_invoice (tenant_id, ar_invoice_id)
    WHERE status = 'ISSUED' AND deleted_at IS NULL;

-- 목록 필터(상태별).
CREATE INDEX idx_tax_invoice_status ON finance.tax_invoice (tenant_id, status);
