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

# 실제 curl의 기본 설정 파일은 stub으로 재현할 수 없다. 두 loopback 수신점으로
# 숨은 connect-to 및 307/308 추적이 POST 본문을 다른 수신점에 보내는지 확인한다.
python3 - "$FIXTURE" "$PATH" <<'PY'
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
import os
import subprocess
import sys
import tempfile
import threading

fixture = Path(sys.argv[1])
original_path = sys.argv[2]
(fixture / "frontend/.env.local").write_text("AUTH_KEYCLOAK_SECRET=synthetic-test-value\n")


class Receiver(BaseHTTPRequestHandler):
    def do_GET(self):
        self.server.requests.append(("GET", self.path, False))
        self.send_response(200)
        self.end_headers()
        self.wfile.write(b"{}")

    def do_POST(self):
        body = self.rfile.read(int(self.headers.get("Content-Length", "0")))
        self.server.requests.append(("POST", self.path, b"synthetic-admin-password" in body))
        if self.server.redirect and "/protocol/openid-connect/token" in self.path:
            self.send_response(self.server.redirect_code)
            self.send_header("Location", self.server.redirect)
        else:
            self.send_response(418)
        self.end_headers()

    def log_message(self, *_):
        pass


servers = [ThreadingHTTPServer(("127.0.0.1", 0), Receiver) for _ in range(2)]
for server in servers:
    server.requests = []
    server.redirect = None
    server.redirect_code = 307
    threading.Thread(target=server.serve_forever, daemon=True).start()

try:
    intended, other = servers
    origin = f"http://127.0.0.1:{intended.server_port}"
    other_origin = f"http://127.0.0.1:{other.server_port}"
    config_home = Path(tempfile.mkdtemp(dir=fixture))
    config = config_home / ".curlrc"
    env = os.environ.copy()
    env.update({
        "PATH": original_path,
        "CURL_HOME": str(config_home),
        "BACKEND_URL": origin,
        "KEYCLOAK_URL": origin,
        "MAILPIT_URL": origin,
        "E2E_COMMERCIAL": "1",
        "E2E_COMMERCIAL_MUTATION": "LOCAL_MUTATION_ACCEPTED",
        "E2E_COMMERCIAL_KC_ADMIN_USERNAME": "synthetic-admin",
        "E2E_COMMERCIAL_KC_ADMIN_PASSWORD": "synthetic-admin-password",
    })

    def check(label, config_text):
        config.write_text(config_text)
        for server in servers:
            server.requests.clear()
        result = subprocess.run(
            ["bash", str(fixture / "scripts/verify-user-onboarding.sh")],
            env=env, stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=15,
        )
        intended_posts = [r for r in intended.requests if r[0] == "POST" and r[2]]
        other_posts = [r for r in other.requests if r[0] == "POST" and r[2]]
        if result.returncode == 0 or len(intended_posts) != 1 or other.requests:
            raise SystemExit(
                f"{label}: exit={result.returncode} intended_posts={len(intended_posts)} "
                f"other_requests={len(other.requests)} other_posts={len(other_posts)} "
                "(expected nonzero/1/0/0)"
            )
        print(f"PASS {label}: intended credential POST=1, alternate requests=0")

    check("curlrc-connect-to", f'connect-to = "127.0.0.1:{intended.server_port}:127.0.0.1:{other.server_port}"\n')
    for status in (307, 308):
        intended.redirect = other_origin + "/redirected-token"
        intended.redirect_code = status
        check(f"curlrc-location-{status}", "location\n")
finally:
    for server in servers:
        server.shutdown()
        server.server_close()
PY
