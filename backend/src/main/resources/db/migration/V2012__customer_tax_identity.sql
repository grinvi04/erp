-- V2012: 거래처(공급받는자) 세금계산서 인적사항 보강 — 대표자·주소·업태·종목(모두 선택).
-- 전자세금계산서(#3) 발행 시 공급받는자 스냅샷의 원천. 기존 business_no·contact_*는 유지.
-- 추가 컬럼은 nullable이라 기존 행 백필 불필요.

ALTER TABLE finance.customer
    ADD COLUMN representative_name VARCHAR(100),
    ADD COLUMN address             VARCHAR(500),
    ADD COLUMN business_type       VARCHAR(200),
    ADD COLUMN business_item       VARCHAR(200);
