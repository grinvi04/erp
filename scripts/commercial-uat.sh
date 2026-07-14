#!/usr/bin/env bash
# 로컬 전용 상용 UAT 실행기. 시크릿은 환경변수/메모리에서만 사용하고 출력하지 않는다.
set -euo pipefail

ROOT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
MODE=${1:---all}

if [[ ${E2E_COMMERCIAL:-} != "1" ]]; then
  echo "E2E_COMMERCIAL=1 명시적 활성화가 필요합니다." >&2
  exit 2
fi
if [[ ${E2E_COMMERCIAL_MUTATION:-} != "LOCAL_MUTATION_ACCEPTED" ]]; then
  echo "E2E_COMMERCIAL_MUTATION=LOCAL_MUTATION_ACCEPTED 확인값이 필요합니다." >&2
  exit 2
fi
if [[ ! -f "$ROOT_DIR/frontend/.env.local" ]]; then
  echo "frontend/.env.local이 필요합니다." >&2
  exit 2
fi

set -a
# shellcheck disable=SC1091
source "$ROOT_DIR/frontend/.env.local"
set +a

export E2E_CLIENT_SECRET=${E2E_CLIENT_SECRET:-${AUTH_KEYCLOAK_SECRET:-}}
export E2E_COMMERCIAL_KC_ISSUER=${E2E_COMMERCIAL_KC_ISSUER:-${KEYCLOAK_ISSUER:-}}
export E2E_COMMERCIAL_CREATOR_USERNAME=${E2E_COMMERCIAL_CREATOR_USERNAME:-uat-commercial-creator}
export E2E_COMMERCIAL_APPROVER_USERNAME=${E2E_COMMERCIAL_APPROVER_USERNAME:-uat-commercial-approver}
export E2E_COMMERCIAL_TENANT_B_USERNAME=${E2E_COMMERCIAL_TENANT_B_USERNAME:-uat-commercial-tenant-b}
export E2E_COMMERCIAL_CREATOR_PASSWORD=${E2E_COMMERCIAL_CREATOR_PASSWORD:-$(openssl rand -base64 24)}
export E2E_COMMERCIAL_APPROVER_PASSWORD=${E2E_COMMERCIAL_APPROVER_PASSWORD:-$(openssl rand -base64 24)}
export E2E_COMMERCIAL_TENANT_B_PASSWORD=${E2E_COMMERCIAL_TENANT_B_PASSWORD:-$(openssl rand -base64 24)}

compose_environment() {
  local key=$1
  docker compose -f "$ROOT_DIR/docker-compose.yml" config --format json \
    | python3 -c "import json,sys; print(json.load(sys.stdin)['services']['keycloak']['environment']['$key'])"
}

export E2E_COMMERCIAL_KC_ADMIN_USERNAME=${E2E_COMMERCIAL_KC_ADMIN_USERNAME:-$(compose_environment KEYCLOAK_ADMIN)}
export E2E_COMMERCIAL_KC_ADMIN_PASSWORD=${E2E_COMMERCIAL_KC_ADMIN_PASSWORD:-$(compose_environment KEYCLOAK_ADMIN_PASSWORD)}

case "$MODE" in
  --dry-run)
    export E2E_COMMERCIAL_DRY_RUN=1
    (cd "$ROOT_DIR/frontend" && npx playwright test --project=commercial-setup)
    ;;
  --setup-only)
    unset E2E_COMMERCIAL_DRY_RUN
    (cd "$ROOT_DIR/frontend" && npx playwright test --project=commercial-setup)
    ;;
  --all)
    unset E2E_COMMERCIAL_DRY_RUN
    (cd "$ROOT_DIR/frontend" && npx playwright test --project=commercial)
    ;;
  *)
    echo "사용법: $0 [--dry-run|--setup-only|--all]" >&2
    exit 2
    ;;
esac
