-- user_directory: Keycloak sub → 표시이름(사람 이름)·이메일 로컬 미러.
-- 인증 요청 시 JWT 클레임(sub·name·preferred_username·email)으로 upsert 누적(이미 본 사용자).
-- 감사·결재·CRM 담당자 등 UUID(sub) 노출 지점을 사람 이름으로 해소하는 표시 전용 캐시.
-- role/audit_log와 동일하게 Hibernate 테넌트 필터 대신 tenant_id 명시 필터링한다.
-- 표시 캐시이므로 version(낙관적잠금)·deleted_at(소프트삭제)은 적용하지 않는다(upsert 덮어쓰기).

CREATE SEQUENCE IF NOT EXISTS common.user_directory_id_seq START 1 INCREMENT 50;

CREATE TABLE common.user_directory (
    id           BIGINT       PRIMARY KEY DEFAULT nextval('common.user_directory_id_seq'),
    tenant_id    BIGINT       NOT NULL,
    sub          VARCHAR(100) NOT NULL,
    display_name VARCHAR(200) NOT NULL,
    email        VARCHAR(320),
    updated_at   TIMESTAMP    NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, sub)
);
CREATE INDEX idx_user_directory_tenant_sub ON common.user_directory (tenant_id, sub);
