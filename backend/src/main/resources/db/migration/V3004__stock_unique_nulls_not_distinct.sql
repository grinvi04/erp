-- stock 자연키 UNIQUE 수정 — 기존 UNIQUE는 lot_no/serial_no의 NULL을 서로 distinct로 취급해
-- 비-로트/시리얼 품목(lot/serial = NULL)의 중복 stock 행을 막지 못했다. 동시 입고 확정 시
-- 같은 (item, location)에 중복 행이 생겨 이후 단건 조회가 깨지고(500) 재고 수량이 분산됐다.
-- PG16 NULLS NOT DISTINCT로 교체해 NULL도 동일하게 취급 + 소프트삭제 인지(부분 인덱스).
-- forward-only.

ALTER TABLE inventory.stock
    DROP CONSTRAINT stock_tenant_id_item_id_location_id_lot_no_serial_no_key;

CREATE UNIQUE INDEX uq_stock_natural
    ON inventory.stock (tenant_id, item_id, location_id, lot_no, serial_no)
    NULLS NOT DISTINCT
    WHERE deleted_at IS NULL;
