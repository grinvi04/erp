-- V3002: movement_line 감사 컬럼 추가 (BaseEntity 표준 통일)
ALTER TABLE inventory.movement_line
    ADD COLUMN version     BIGINT       NOT NULL DEFAULT 0,
    ADD COLUMN deleted_at  TIMESTAMP,
    ADD COLUMN created_at  TIMESTAMP    NOT NULL DEFAULT now(),
    ADD COLUMN updated_at  TIMESTAMP    NOT NULL DEFAULT now(),
    ADD COLUMN created_by  VARCHAR(100) NOT NULL DEFAULT 'system',
    ADD COLUMN updated_by  VARCHAR(100) NOT NULL DEFAULT 'system';
