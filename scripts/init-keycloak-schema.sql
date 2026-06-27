-- Keycloak이 KC_DB_SCHEMA=keycloak 를 쓰는데 Keycloak 26은 스키마를 자동 생성하지 않는다.
-- postgres 컨테이너 최초 기동 시(docker-entrypoint-initdb.d) 이 스키마를 만들어 둔다.
-- 앱은 public/common/hr/finance/inventory/crm 스키마를 쓰므로 Keycloak과 충돌하지 않는다.
CREATE SCHEMA IF NOT EXISTS keycloak;
