-- GL 전표 전기 결재선 연결 — journal_entry에 결재 요청(common.approval_request) 참조 컬럼 추가.
-- 모듈 경계 유지: FK 없이 ID만 보관(AP 전표 approval_request_id와 동형). forward-only.
ALTER TABLE finance.journal_entry ADD COLUMN approval_request_id BIGINT;

CREATE INDEX idx_journal_entry_approval_request
    ON finance.journal_entry (tenant_id, approval_request_id)
    WHERE deleted_at IS NULL AND approval_request_id IS NOT NULL;
