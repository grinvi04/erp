-- V2011: 회사정보(전자세금계산서 공급자) — 테넌트당 1행. 상호·사업자등록번호(필수)·대표자·주소·
-- 업태·종목(선택). 세금계산서 발행 시점에 이 정보를 공급자 스냅샷으로 복사한다(#3 전자세금계산서).
-- tenant_id는 @TenantId로 자동 필터되며 테넌트당 1행을 UNIQUE로 강제한다.

CREATE SEQUENCE IF NOT EXISTS finance.company_profile_id_seq START 1 INCREMENT 10;

CREATE TABLE finance.company_profile (
    id            BIGINT       PRIMARY KEY DEFAULT nextval('finance.company_profile_id_seq'),
    tenant_id     BIGINT       NOT NULL,
    company_name  VARCHAR(200) NOT NULL,
    business_no   VARCHAR(30)  NOT NULL,
    representative VARCHAR(100),
    address       VARCHAR(500),
    business_type VARCHAR(200),
    business_item VARCHAR(200),
    version       BIGINT       NOT NULL DEFAULT 0,
    deleted_at    TIMESTAMP,
    created_at    TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT now(),
    created_by    VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by    VARCHAR(100) NOT NULL DEFAULT 'system',
    UNIQUE (tenant_id)
);
