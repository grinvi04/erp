-- 재고 조정(ADJUSTMENT) 이동 확정 결재선 연결 — movement에 결재 요청(common.approval_request) 참조 컬럼 추가.
-- 모듈 경계 유지: FK 없이 ID만 보관(GL journal_entry approval_request_id와 동형). forward-only.
ALTER TABLE inventory.movement ADD COLUMN approval_request_id BIGINT;

CREATE INDEX idx_movement_approval_request
    ON inventory.movement (tenant_id, approval_request_id)
    WHERE deleted_at IS NULL AND approval_request_id IS NOT NULL;
