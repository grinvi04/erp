-- 인가(authorization) DB 관리 — 역할·권한매핑·사용자배정·접근프로파일.
-- auth-standards: 역할·권한·매핑은 DB 관리. Keycloak은 인증(신원)만 담당.
-- audit_log와 동일하게 @TenantId(Hibernate 자동필터)를 쓰지 않고 tenant_id를 명시 필터링한다
-- — 권한 해석이 JWT 인증 단계(TenantContext 세팅 전)에서 일어나기 때문.

CREATE SEQUENCE IF NOT EXISTS common.role_id_seq                 START 1 INCREMENT 50;
CREATE SEQUENCE IF NOT EXISTS common.user_role_id_seq           START 1 INCREMENT 50;
CREATE SEQUENCE IF NOT EXISTS common.user_access_profile_id_seq START 1 INCREMENT 50;

-- 역할: 권한의 묶음(운영이 관리 화면에서 조합)
CREATE TABLE common.role (
    id          BIGINT       PRIMARY KEY DEFAULT nextval('common.role_id_seq'),
    tenant_id   BIGINT       NOT NULL,
    code        VARCHAR(50)  NOT NULL,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    created_at  TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, code)
);

-- 역할 ↔ 권한코드 매핑 (권한코드는 com.erp.common.security.Permission 상수가 계약)
CREATE TABLE common.role_permission (
    role_id         BIGINT       NOT NULL REFERENCES common.role(id) ON DELETE CASCADE,
    permission_code VARCHAR(100) NOT NULL,
    PRIMARY KEY (role_id, permission_code)
);

-- 사용자(Keycloak sub) ↔ 역할 배정
CREATE TABLE common.user_role (
    id        BIGINT       PRIMARY KEY DEFAULT nextval('common.user_role_id_seq'),
    tenant_id BIGINT       NOT NULL,
    user_id   VARCHAR(100) NOT NULL,
    role_id   BIGINT       NOT NULL REFERENCES common.role(id) ON DELETE CASCADE,
    UNIQUE (tenant_id, user_id, role_id)
);
CREATE INDEX idx_user_role_tenant_user ON common.user_role (tenant_id, user_id);

-- 사용자별 접근 프로파일: 데이터 스코프·소속부서·전결 한도
CREATE TABLE common.user_access_profile (
    id             BIGINT        PRIMARY KEY DEFAULT nextval('common.user_access_profile_id_seq'),
    tenant_id      BIGINT        NOT NULL,
    user_id        VARCHAR(100)  NOT NULL,
    data_scope     VARCHAR(20)   NOT NULL DEFAULT 'ALL',
    department_id  BIGINT,
    approval_limit NUMERIC(20,2),
    created_at     TIMESTAMP     NOT NULL DEFAULT now(),
    updated_at     TIMESTAMP     NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, user_id)
);
