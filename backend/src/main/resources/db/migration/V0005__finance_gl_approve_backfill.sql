-- finance:write 보유 역할에 finance:gl:approve 백필(비파괴). id는 DEFAULT nextval, tenant_id는 원본 행에서 복사.
-- GL 전표 전기 결재권 — 작성자≠전기결재자 직무분리. forward-only. V0001~V0004(릴리즈됨) 수정 금지.
INSERT INTO common.role_permission (tenant_id, role_id, permission_code)
SELECT rp.tenant_id, rp.role_id, 'finance:gl:approve'
  FROM common.role_permission rp
 WHERE rp.permission_code = 'finance:write'
   AND NOT EXISTS (SELECT 1 FROM common.role_permission x
                   WHERE x.role_id = rp.role_id AND x.permission_code = 'finance:gl:approve');
