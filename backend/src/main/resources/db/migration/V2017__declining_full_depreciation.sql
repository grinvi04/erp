-- V2017: 정률법 완전상각(#170) — 정률→정액 자동 전환의 잔여내용연수 산정용 경과월수 컬럼.
-- 정률 월상각 = max(정률액, (장부가액−잔존)/잔여내용연수)로 내용연수 말 잔존까지 완전상각.
-- 기존 자산은 상각이력(depreciation_entry) 개수로 depreciated_months 백필.

ALTER TABLE finance.fixed_asset
    ADD COLUMN depreciated_months INT NOT NULL DEFAULT 0;

UPDATE finance.fixed_asset fa
SET depreciated_months = (
    SELECT count(*)
    FROM finance.depreciation_entry de
    WHERE de.fixed_asset_id = fa.id
      AND de.deleted_at IS NULL
);
