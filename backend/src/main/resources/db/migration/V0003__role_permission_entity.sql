-- role_permission: @ElementCollection<String> → tenant_id를 보유한 RolePermission 엔티티로 전환.
-- 권한 해석이 테이블 자체의 tenant_id로 cross-tenant 격리를 검증하도록 DB 레벨 심층 방어 추가.
-- forward-only · 비파괴(기존 매핑 행 보존). V0002(릴리즈됨) 수정 금지.

CREATE SEQUENCE IF NOT EXISTS common.role_permission_id_seq START 1 INCREMENT 50;

-- 1) 신규 컬럼을 우선 nullable로 추가(기존 행이 있으므로 즉시 NOT NULL 불가)
ALTER TABLE common.role_permission ADD COLUMN id        BIGINT;
ALTER TABLE common.role_permission ADD COLUMN tenant_id BIGINT;

-- 2) backfill: id 채번 + tenant_id는 부모 role에서 복사
UPDATE common.role_permission rp
   SET id        = nextval('common.role_permission_id_seq'),
       tenant_id = r.tenant_id
  FROM common.role r
 WHERE rp.role_id = r.id;

-- 3) NOT NULL 확정 + 이후 INSERT 기본 채번(role 테이블과 동일 패턴)
ALTER TABLE common.role_permission ALTER COLUMN id        SET NOT NULL;
ALTER TABLE common.role_permission ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE common.role_permission ALTER COLUMN id        SET DEFAULT nextval('common.role_permission_id_seq');

-- 4) 복합 PK(role_id, permission_code) → 단일 id PK, (role_id, permission_code) UNIQUE로 보존
ALTER TABLE common.role_permission DROP CONSTRAINT role_permission_pkey;
ALTER TABLE common.role_permission ADD PRIMARY KEY (id);
ALTER TABLE common.role_permission
    ADD CONSTRAINT uq_role_permission_role_code UNIQUE (role_id, permission_code);

CREATE INDEX idx_role_permission_role ON common.role_permission (role_id);
