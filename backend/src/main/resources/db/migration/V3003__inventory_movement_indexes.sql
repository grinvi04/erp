-- 목록 엔드포인트 하드닝: 이동(movement) 유형·상태 필터 목록 조회의 인덱스 보강.
-- MovementRepository.findByTypeAndStatus 대응. 소프트삭제 행을 제외하는 부분 인덱스
-- (기존 inventory 인덱스 컨벤션과 동일).

CREATE INDEX idx_movement_tenant_type_status
    ON inventory.movement (tenant_id, movement_type, status) WHERE deleted_at IS NULL;
