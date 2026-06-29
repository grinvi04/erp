-- V2009: 부가세 통제계정 설정 — 테넌트 FX/재무 설정(tenant_base_currency)에
-- 부가세대급금(매입, vat_receivable)·부가세예수금(매출, vat_payable) 계정 FK를 추가한다.
-- 과세 인보이스 승인 전기 시 공급가액·세액을 분리해 이 통제계정으로 분개한다. 둘 다 nullable —
-- 매입은 대급금, 매출은 예수금만 독립적으로 쓰며, 미설정이면 부가세 라인을 생략하고 기존 분개를 유지한다(폴백).

ALTER TABLE finance.tenant_base_currency
    ADD COLUMN vat_receivable_account_id BIGINT,
    ADD COLUMN vat_payable_account_id BIGINT;

ALTER TABLE finance.tenant_base_currency
    ADD CONSTRAINT fk_tbc_vat_receivable_account
        FOREIGN KEY (vat_receivable_account_id) REFERENCES finance.account(id),
    ADD CONSTRAINT fk_tbc_vat_payable_account
        FOREIGN KEY (vat_payable_account_id) REFERENCES finance.account(id);
