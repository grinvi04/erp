-- inventory:write 보유 역할에 inventory:movement:approve 백필(비파괴). id는 DEFAULT nextval, tenant_id는 원본 행에서 복사.
-- 재고 조정(ADJUSTMENT) 이동 확정 결재권 — 작성자≠확정결재자 직무분리. forward-only. V0001~V0005(릴리즈됨) 수정 금지.
INSERT INTO common.role_permission (tenant_id, role_id, permission_code)
SELECT rp.tenant_id, rp.role_id, 'inventory:movement:approve'
  FROM common.role_permission rp
 WHERE rp.permission_code = 'inventory:write'
   AND NOT EXISTS (SELECT 1 FROM common.role_permission x
                   WHERE x.role_id = rp.role_id AND x.permission_code = 'inventory:movement:approve');
