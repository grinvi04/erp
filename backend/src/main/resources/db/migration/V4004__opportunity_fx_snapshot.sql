-- V4004: 거래 시점 FX 스냅샷 — crm.opportunity에 base_amount·exchange_rate 추가 + 백필
-- 무중단: nullable 컬럼 추가(IF NOT EXISTS) 후 백필. 컬럼·UPDATE 모두 멱등(재실행 안전).
-- 백필 정책(AC-10): amount 보유 + 통화 == 테넌트 기준통화면 base=amount·rate=1, 그 외는 null(미산정).
-- 기준통화 조회는 finance.tenant_base_currency 읽기 조인 — 런타임 코드의 크로스스키마 조인 금지
-- 규칙과 별개로, 마이그레이션 백필 한정의 읽기 SQL이므로 허용한다(미설정 테넌트는 KRW 기본).

ALTER TABLE crm.opportunity ADD COLUMN IF NOT EXISTS base_amount   NUMERIC(20, 2);
ALTER TABLE crm.opportunity ADD COLUMN IF NOT EXISTS exchange_rate NUMERIC(18, 8);

UPDATE crm.opportunity t
   SET base_amount = t.amount, exchange_rate = 1
 WHERE t.base_amount IS NULL
   AND t.amount IS NOT NULL
   AND t.currency = COALESCE(
       (SELECT b.base_currency FROM finance.tenant_base_currency b
         WHERE b.tenant_id = t.tenant_id AND b.deleted_at IS NULL), 'KRW');
