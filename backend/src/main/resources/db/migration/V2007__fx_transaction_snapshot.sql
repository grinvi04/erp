-- V2007: 거래 시점 FX 스냅샷 — ap_invoice·ar_invoice·journal_entry에 base_amount·exchange_rate 추가 + 백필
-- 무중단: nullable 컬럼 추가(IF NOT EXISTS) 후 백필. 컬럼·UPDATE 모두 멱등(재실행 안전).
-- 백필 정책(AC-10): 행 통화 == 테넌트 기준통화면 base=원액·rate=1, 비기준 통화는 null(미산정) 유지.
-- 기준통화는 finance.tenant_base_currency 조인(미설정 테넌트는 KRW 기본).

ALTER TABLE finance.ap_invoice    ADD COLUMN IF NOT EXISTS base_amount   NUMERIC(20, 2);
ALTER TABLE finance.ap_invoice    ADD COLUMN IF NOT EXISTS exchange_rate NUMERIC(18, 8);
ALTER TABLE finance.ar_invoice    ADD COLUMN IF NOT EXISTS base_amount   NUMERIC(20, 2);
ALTER TABLE finance.ar_invoice    ADD COLUMN IF NOT EXISTS exchange_rate NUMERIC(18, 8);
ALTER TABLE finance.journal_entry ADD COLUMN IF NOT EXISTS base_amount   NUMERIC(20, 2);
ALTER TABLE finance.journal_entry ADD COLUMN IF NOT EXISTS exchange_rate NUMERIC(18, 8);

UPDATE finance.ap_invoice t
   SET base_amount = t.total_amount, exchange_rate = 1
 WHERE t.base_amount IS NULL
   AND t.currency = COALESCE(
       (SELECT b.base_currency FROM finance.tenant_base_currency b
         WHERE b.tenant_id = t.tenant_id AND b.deleted_at IS NULL), 'KRW');

UPDATE finance.ar_invoice t
   SET base_amount = t.total_amount, exchange_rate = 1
 WHERE t.base_amount IS NULL
   AND t.currency = COALESCE(
       (SELECT b.base_currency FROM finance.tenant_base_currency b
         WHERE b.tenant_id = t.tenant_id AND b.deleted_at IS NULL), 'KRW');

UPDATE finance.journal_entry t
   SET base_amount = t.total_debit, exchange_rate = 1
 WHERE t.base_amount IS NULL
   AND t.currency = COALESCE(
       (SELECT b.base_currency FROM finance.tenant_base_currency b
         WHERE b.tenant_id = t.tenant_id AND b.deleted_at IS NULL), 'KRW');
