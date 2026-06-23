-- V3001: Inventory 모듈 초기 스키마
-- 품목 마스터 → 창고/로케이션 → 재고 → 입고/출고/이동/조정

CREATE SEQUENCE IF NOT EXISTS inventory.item_id_seq         START 1 INCREMENT 50;
CREATE SEQUENCE IF NOT EXISTS inventory.item_category_id_seq START 1 INCREMENT 50;
CREATE SEQUENCE IF NOT EXISTS inventory.uom_id_seq          START 1 INCREMENT 20;
CREATE SEQUENCE IF NOT EXISTS inventory.warehouse_id_seq    START 1 INCREMENT 20;
CREATE SEQUENCE IF NOT EXISTS inventory.location_id_seq     START 1 INCREMENT 100;
CREATE SEQUENCE IF NOT EXISTS inventory.stock_id_seq        START 1 INCREMENT 100;
CREATE SEQUENCE IF NOT EXISTS inventory.movement_id_seq     START 1 INCREMENT 200;
CREATE SEQUENCE IF NOT EXISTS inventory.movement_line_id_seq START 1 INCREMENT 500;

-- 단위 (EA, KG, L, BOX 등)
CREATE TABLE inventory.unit_of_measure (
    id          BIGINT       PRIMARY KEY DEFAULT nextval('inventory.uom_id_seq'),
    tenant_id   BIGINT       NOT NULL,
    code        VARCHAR(20)  NOT NULL,
    name        VARCHAR(100) NOT NULL,
    version     BIGINT       NOT NULL DEFAULT 0,
    deleted_at  TIMESTAMP,
    created_at  TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT now(),
    created_by  VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by  VARCHAR(100) NOT NULL DEFAULT 'system',
    UNIQUE (tenant_id, code)
);

-- 품목 분류
CREATE TABLE inventory.item_category (
    id          BIGINT       PRIMARY KEY DEFAULT nextval('inventory.item_category_id_seq'),
    tenant_id   BIGINT       NOT NULL,
    code        VARCHAR(30)  NOT NULL,
    name        VARCHAR(100) NOT NULL,
    parent_id   BIGINT       REFERENCES inventory.item_category(id),
    version     BIGINT       NOT NULL DEFAULT 0,
    deleted_at  TIMESTAMP,
    created_at  TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT now(),
    created_by  VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by  VARCHAR(100) NOT NULL DEFAULT 'system',
    UNIQUE (tenant_id, code)
);

-- 품목 마스터
CREATE TABLE inventory.item (
    id              BIGINT          PRIMARY KEY DEFAULT nextval('inventory.item_id_seq'),
    tenant_id       BIGINT          NOT NULL,
    sku             VARCHAR(50)     NOT NULL,
    name            VARCHAR(200)    NOT NULL,
    description     TEXT,
    category_id     BIGINT          REFERENCES inventory.item_category(id),
    uom_id          BIGINT          NOT NULL REFERENCES inventory.unit_of_measure(id),
    cost_method     VARCHAR(20)     NOT NULL DEFAULT 'WEIGHTED_AVG', -- FIFO/LIFO/WEIGHTED_AVG/STANDARD
    standard_cost   NUMERIC(15, 4),
    reorder_point   NUMERIC(12, 2)  NOT NULL DEFAULT 0,
    reorder_qty     NUMERIC(12, 2)  NOT NULL DEFAULT 0,
    min_stock       NUMERIC(12, 2)  NOT NULL DEFAULT 0,
    max_stock       NUMERIC(12, 2),
    is_lot_tracked  BOOLEAN         NOT NULL DEFAULT false,
    is_serial_tracked BOOLEAN       NOT NULL DEFAULT false,
    is_active       BOOLEAN         NOT NULL DEFAULT true,
    version         BIGINT          NOT NULL DEFAULT 0,
    deleted_at      TIMESTAMP,
    created_at      TIMESTAMP       NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT now(),
    created_by      VARCHAR(100)    NOT NULL DEFAULT 'system',
    updated_by      VARCHAR(100)    NOT NULL DEFAULT 'system',
    UNIQUE (tenant_id, sku)
);

-- 창고
CREATE TABLE inventory.warehouse (
    id          BIGINT       PRIMARY KEY DEFAULT nextval('inventory.warehouse_id_seq'),
    tenant_id   BIGINT       NOT NULL,
    code        VARCHAR(30)  NOT NULL,
    name        VARCHAR(100) NOT NULL,
    address     VARCHAR(500),
    is_active   BOOLEAN      NOT NULL DEFAULT true,
    version     BIGINT       NOT NULL DEFAULT 0,
    deleted_at  TIMESTAMP,
    created_at  TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT now(),
    created_by  VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by  VARCHAR(100) NOT NULL DEFAULT 'system',
    UNIQUE (tenant_id, code)
);

-- 로케이션 (창고 내 구역/렉/빈)
CREATE TABLE inventory.location (
    id           BIGINT       PRIMARY KEY DEFAULT nextval('inventory.location_id_seq'),
    tenant_id    BIGINT       NOT NULL,
    warehouse_id BIGINT       NOT NULL REFERENCES inventory.warehouse(id),
    code         VARCHAR(30)  NOT NULL,
    name         VARCHAR(100) NOT NULL,
    parent_id    BIGINT       REFERENCES inventory.location(id),
    location_type VARCHAR(20) NOT NULL DEFAULT 'BIN', -- ZONE/AISLE/RACK/BIN
    is_active    BOOLEAN      NOT NULL DEFAULT true,
    version      BIGINT       NOT NULL DEFAULT 0,
    deleted_at   TIMESTAMP,
    created_at   TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at   TIMESTAMP    NOT NULL DEFAULT now(),
    created_by   VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by   VARCHAR(100) NOT NULL DEFAULT 'system',
    UNIQUE (tenant_id, warehouse_id, code)
);

-- 재고 원장 (품목 × 로케이션)
CREATE TABLE inventory.stock (
    id           BIGINT          PRIMARY KEY DEFAULT nextval('inventory.stock_id_seq'),
    tenant_id    BIGINT          NOT NULL,
    item_id      BIGINT          NOT NULL REFERENCES inventory.item(id),
    location_id  BIGINT          NOT NULL REFERENCES inventory.location(id),
    lot_no       VARCHAR(50),    -- 로트 번호 (is_lot_tracked=true)
    serial_no    VARCHAR(100),   -- 시리얼 번호 (is_serial_tracked=true)
    qty_on_hand  NUMERIC(12, 2)  NOT NULL DEFAULT 0,
    qty_reserved NUMERIC(12, 2)  NOT NULL DEFAULT 0, -- 출고 예약
    unit_cost    NUMERIC(15, 4)  NOT NULL DEFAULT 0,
    version      BIGINT          NOT NULL DEFAULT 0,
    deleted_at   TIMESTAMP,
    created_at   TIMESTAMP       NOT NULL DEFAULT now(),
    updated_at   TIMESTAMP       NOT NULL DEFAULT now(),
    created_by   VARCHAR(100)    NOT NULL DEFAULT 'system',
    updated_by   VARCHAR(100)    NOT NULL DEFAULT 'system',
    UNIQUE (tenant_id, item_id, location_id, lot_no, serial_no),
    CONSTRAINT chk_stock_non_negative CHECK (qty_on_hand >= 0)
);

CREATE INDEX idx_stock_tenant_item     ON inventory.stock (tenant_id, item_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_stock_tenant_location ON inventory.stock (tenant_id, location_id) WHERE deleted_at IS NULL;

-- 재고 이동 헤더 (입고/출고/이동/조정/반품)
CREATE TABLE inventory.movement (
    id              BIGINT       PRIMARY KEY DEFAULT nextval('inventory.movement_id_seq'),
    tenant_id       BIGINT       NOT NULL,
    movement_no     VARCHAR(30)  NOT NULL,
    movement_type   VARCHAR(30)  NOT NULL, -- RECEIPT/ISSUE/TRANSFER/ADJUSTMENT/RETURN
    status          VARCHAR(20)  NOT NULL DEFAULT 'DRAFT', -- DRAFT/CONFIRMED/CANCELLED
    reference_type  VARCHAR(100),-- PO / SO / TRANSFER_ORDER 등
    reference_id    BIGINT,
    movement_date   DATE         NOT NULL,
    note            TEXT,
    version         BIGINT       NOT NULL DEFAULT 0,
    deleted_at      TIMESTAMP,
    created_at      TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT now(),
    created_by      VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by      VARCHAR(100) NOT NULL DEFAULT 'system',
    UNIQUE (tenant_id, movement_no)
);

-- 재고 이동 라인
CREATE TABLE inventory.movement_line (
    id              BIGINT          PRIMARY KEY DEFAULT nextval('inventory.movement_line_id_seq'),
    tenant_id       BIGINT          NOT NULL,
    movement_id     BIGINT          NOT NULL REFERENCES inventory.movement(id),
    line_no         INT             NOT NULL,
    item_id         BIGINT          NOT NULL REFERENCES inventory.item(id),
    from_location_id BIGINT         REFERENCES inventory.location(id), -- NULL=외부(입고)
    to_location_id  BIGINT          REFERENCES inventory.location(id), -- NULL=외부(출고)
    lot_no          VARCHAR(50),
    serial_no       VARCHAR(100),
    qty             NUMERIC(12, 2)  NOT NULL,
    unit_cost       NUMERIC(15, 4)  NOT NULL DEFAULT 0,
    CONSTRAINT chk_movement_direction CHECK (
        from_location_id IS NOT NULL OR to_location_id IS NOT NULL
    )
);

CREATE INDEX idx_movement_tenant_date ON inventory.movement (tenant_id, movement_date DESC) WHERE deleted_at IS NULL;
