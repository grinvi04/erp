-- finance:write 보유 역할에 finance:setting:write 백필(비파괴). id는 DEFAULT nextval, tenant_id는 원본 행에서 복사.
-- 기준통화·환율 변경권을 기존 재무작성 역할에 부여(멱등). forward-only. V0001~V0006(릴리즈됨) 수정 금지.
INSERT INTO common.role_permission (tenant_id, role_id, permission_code)
SELECT rp.tenant_id, rp.role_id, 'finance:setting:write'
  FROM common.role_permission rp
 WHERE rp.permission_code = 'finance:write'
   AND NOT EXISTS (SELECT 1 FROM common.role_permission x
                   WHERE x.role_id = rp.role_id AND x.permission_code = 'finance:setting:write');
