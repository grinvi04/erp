-- V2010: 매입/매출 인보이스에 부가세 분리 컬럼 추가 — 공급가액(supply_amount)·부가세액(vat_amount)·
-- 과세구분(tax_type 과세/영세율/면세). total_amount = supply_amount + vat_amount.
-- 기존 데이터 백필(확정): 공급가액=총액, 세액=0, 과세구분=과세(TAXABLE) — 과거 전기 분개에 영향 없음.
-- nullable로 추가 → 백필 → NOT NULL(forward-only, 무중단).

ALTER TABLE finance.ap_invoice
    ADD COLUMN supply_amount NUMERIC(20, 2),
    ADD COLUMN vat_amount NUMERIC(20, 2),
    ADD COLUMN tax_type VARCHAR(20);

UPDATE finance.ap_invoice
SET supply_amount = total_amount,
    vat_amount = 0,
    tax_type = 'TAXABLE'
WHERE supply_amount IS NULL;

ALTER TABLE finance.ap_invoice
    ALTER COLUMN supply_amount SET NOT NULL,
    ALTER COLUMN vat_amount SET NOT NULL,
    ALTER COLUMN tax_type SET NOT NULL;

ALTER TABLE finance.ar_invoice
    ADD COLUMN supply_amount NUMERIC(20, 2),
    ADD COLUMN vat_amount NUMERIC(20, 2),
    ADD COLUMN tax_type VARCHAR(20);

UPDATE finance.ar_invoice
SET supply_amount = total_amount,
    vat_amount = 0,
    tax_type = 'TAXABLE'
WHERE supply_amount IS NULL;

ALTER TABLE finance.ar_invoice
    ALTER COLUMN supply_amount SET NOT NULL,
    ALTER COLUMN vat_amount SET NOT NULL,
    ALTER COLUMN tax_type SET NOT NULL;
