-- V2008: 환차손익(실현) 계정 설정 — 테넌트 FX 설정(tenant_base_currency)에
-- 환차손(fx_loss)·환차이익(fx_gain) 계정 FK를 추가한다. 외화 결제 시 결제환율≠인보이스환율
-- 차액을 이 계정으로 분개해 기준통화 원장의 통제계정을 정확히 청산한다. 둘 다 nullable —
-- 미설정 테넌트는 환차 분개를 생략하고 기존 원통화 결제 분개를 유지한다(폴백).

ALTER TABLE finance.tenant_base_currency
    ADD COLUMN fx_gain_account_id BIGINT,
    ADD COLUMN fx_loss_account_id BIGINT;

ALTER TABLE finance.tenant_base_currency
    ADD CONSTRAINT fk_tbc_fx_gain_account
        FOREIGN KEY (fx_gain_account_id) REFERENCES finance.account(id),
    ADD CONSTRAINT fk_tbc_fx_loss_account
        FOREIGN KEY (fx_loss_account_id) REFERENCES finance.account(id);
