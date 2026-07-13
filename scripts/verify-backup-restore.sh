#!/usr/bin/env bash
set -euo pipefail

require_env() {
  if [[ -z "${!1:-}" ]]; then
    echo "필수 환경변수 누락: $1" >&2
    exit 2
  fi
}

require_env SOURCE_DATABASE_URL
require_env RESTORE_DATABASE_URL

if [[ "$SOURCE_DATABASE_URL" == "$RESTORE_DATABASE_URL" ]]; then
  echo "소스와 복원 대상 DB가 같습니다. 복원 검증을 중단합니다." >&2
  exit 2
fi
if [[ "${ALLOW_DESTRUCTIVE_RESTORE:-}" != "true" ]]; then
  echo "복원 대상 초기화를 승인하려면 ALLOW_DESTRUCTIVE_RESTORE=true가 필요합니다." >&2
  exit 2
fi

POSTGRES_IMAGE="${POSTGRES_IMAGE:-postgres:16-alpine}"
WORK_DIR="$(mktemp -d)"
trap 'rm -rf "$WORK_DIR"' EXIT

run_pg() {
  local database_url="$1"
  shift
  docker run --rm \
    -e DATABASE_URL="$database_url" \
    -v "$WORK_DIR:/work" \
    "$POSTGRES_IMAGE" "$@"
}

snapshot_counts() {
  local database_url="$1"
  run_pg "$database_url" psql "$database_url" -v ON_ERROR_STOP=1 -Atc \
    "SELECT (SELECT COUNT(*) FROM public.flyway_schema_history),
            (SELECT COUNT(*) FROM common.tenant),
            (SELECT COUNT(*) FROM hr.employee),
            (SELECT COUNT(*) FROM finance.journal_entry),
            (SELECT COUNT(*) FROM inventory.stock),
            (SELECT COUNT(*) FROM crm.account),
            (SELECT COUNT(*) FROM keycloak.user_entity);"
}

echo "1/4 원본 데이터 스냅샷 집계"
SOURCE_COUNTS="$(snapshot_counts "$SOURCE_DATABASE_URL")"

echo "2/4 PostgreSQL custom-format 백업 생성"
run_pg "$SOURCE_DATABASE_URL" \
  pg_dump "$SOURCE_DATABASE_URL" --format=custom --no-owner --no-acl --file=/work/erp.dump

echo "3/4 격리된 대상 DB에 복원"
run_pg "$RESTORE_DATABASE_URL" \
  pg_restore --dbname="$RESTORE_DATABASE_URL" --clean --if-exists --no-owner --no-acl \
  --exit-on-error /work/erp.dump

echo "4/4 Flyway·테넌트·핵심 모듈·Keycloak 데이터 대조"
RESTORE_COUNTS="$(snapshot_counts "$RESTORE_DATABASE_URL")"
if [[ "$SOURCE_COUNTS" != "$RESTORE_COUNTS" ]]; then
  echo "복원 검증 실패: 핵심 테이블 행 수가 원본과 다릅니다." >&2
  exit 1
fi

echo "복원 검증 성공: 핵심 스키마와 행 수가 원본과 일치합니다."
