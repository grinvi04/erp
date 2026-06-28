-- V4005: 리드 전환 시 생성되는 담당자(Contact) 연결 — crm.lead에 converted_contact_id 추가
-- 무중단: nullable 컬럼 추가(IF NOT EXISTS). 멱등(재실행 안전).

ALTER TABLE crm.lead ADD COLUMN IF NOT EXISTS converted_contact_id BIGINT REFERENCES crm.contact(id);
