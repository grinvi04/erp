#!/usr/bin/env bash
# UAT 진입 주소 검증의 네트워크 차단 계약. 합성 파일과 curl stub만 사용한다.
set -euo pipefail

ROOT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
FIXTURE=$(mktemp -d)
trap 'rm -rf "$FIXTURE"' EXIT
mkdir -p "$FIXTURE/scripts" "$FIXTURE/frontend" "$FIXTURE/bin"
cp "$ROOT_DIR/scripts/verify-user-onboarding.sh" "$FIXTURE/scripts/"
printf 'AUTH_KEYCLOAK_SECRET=synthetic-test-value\n' > "$FIXTURE/frontend/.env.local"
cat > "$FIXTURE/bin/curl" <<'STUB'
#!/usr/bin/env bash
printf 'call\n' >> "$UAT_CURL_CALLS"
exit 69
STUB
chmod +x "$FIXTURE/bin/curl"

run_case() {
  local name=$1 variable=$2 url=$3 expected_status=$4 expected_calls=$5
  : > "$FIXTURE/calls"
  local status=0
  env PATH="$FIXTURE/bin:$PATH" UAT_CURL_CALLS="$FIXTURE/calls" \
    E2E_COMMERCIAL=1 E2E_COMMERCIAL_MUTATION=LOCAL_MUTATION_ACCEPTED \
    E2E_COMMERCIAL_KC_ADMIN_USERNAME=synthetic \
    E2E_COMMERCIAL_KC_ADMIN_PASSWORD=synthetic \
    "$variable=$url" bash "$FIXTURE/scripts/verify-user-onboarding.sh" \
    > "$FIXTURE/output" 2>&1 || status=$?
  local calls
  calls=$(wc -l < "$FIXTURE/calls" | tr -d ' ')
  if [[ "$status" != "$expected_status" || "$calls" != "$expected_calls" ]]; then
    echo "$name: exit=$status curl_calls=$calls (expected $expected_status/$expected_calls)" >&2
    exit 1
  fi
  echo "PASS $name"
}

run_case remote-backend BACKEND_URL https://example.invalid 2 0
run_case remote-keycloak KEYCLOAK_URL http://example.invalid:8180 2 0
run_case remote-mailpit MAILPIT_URL http://example.invalid:8025 2 0
run_case credential-url BACKEND_URL http://user:pass@localhost:8080 2 0
run_case host-suffix BACKEND_URL http://localhost.example.invalid:8080 2 0
run_case option-injection BACKEND_URL '--url=http://example.invalid' 2 0
run_case path-url BACKEND_URL http://localhost:8080/other 2 0
run_case malformed-url KEYCLOAK_URL 'http://localhost:8180?next=elsewhere' 2 0
run_case loopback-backend BACKEND_URL http://127.0.0.1:18180 69 1
run_case loopback-keycloak KEYCLOAK_URL 'http://[::1]:18180' 69 1

# 첫 검사 뒤 source되는 로컬 설정 파일의 재정의도 네트워크 전에 막는다.
printf 'AUTH_KEYCLOAK_SECRET=synthetic-test-value\nBACKEND_URL=https://example.invalid\n' \
  > "$FIXTURE/frontend/.env.local"
run_case sourced-remote-backend BACKEND_URL http://localhost:8080 2 0
