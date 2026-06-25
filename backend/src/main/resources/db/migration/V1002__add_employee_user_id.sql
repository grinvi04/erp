-- 직원 ↔ Keycloak 로그인 계정(sub) 연결. 결재자 식별의 정본 신원.
-- nullable: 모든 직원이 로그인 계정을 갖지 않을 수 있음(연결 전 상태 허용).
ALTER TABLE hr.employee ADD COLUMN user_id VARCHAR(100);

-- 테넌트 내 동일 계정이 여러 직원에 중복 연결되는 것을 방지(소프트삭제 제외).
CREATE UNIQUE INDEX uq_employee_user_id
    ON hr.employee (tenant_id, user_id)
    WHERE user_id IS NOT NULL AND deleted_at IS NULL;
