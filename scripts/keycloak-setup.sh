#!/usr/bin/env bash
# 로컬 개발용 Keycloak 셋업 — realm `erp` + client `erp-frontend` + 테스트 계정.
# `docker compose up -d` 로 Keycloak이 뜬 뒤 1회 실행한다(멱등 — 재실행 안전).
#
#   ./scripts/keycloak-setup.sh
#
# 출력: CLIENT_SECRET(프론트 .env.local) · USER_SUB(백엔드 부트스트랩 admin-sub) · 로그인 계정.
set -euo pipefail

KC="${KEYCLOAK_URL:-http://localhost:8180}"
REALM="erp"
CLIENT_ID="erp-frontend"
ADMIN_USER="admin"          # 테스트 로그인 계정
ADMIN_PASS="Admin123!"
TENANT_ID="1"

echo "→ Keycloak admin 토큰 획득 ($KC)"
TOKEN=$(curl -fsS -X POST "$KC/realms/master/protocol/openid-connect/token" \
  -d "username=admin&password=admin&grant_type=password&client_id=admin-cli" \
  | python3 -c "import sys,json;print(json.load(sys.stdin)['access_token'])")
AUTH=(-H "Authorization: Bearer $TOKEN")
JSON=(-H "Content-Type: application/json")

echo "→ realm $REALM (없으면 생성)"
curl -fsS -o /dev/null "$KC/admin/realms/$REALM" "${AUTH[@]}" 2>/dev/null \
  || curl -fsS -o /dev/null -X POST "$KC/admin/realms" "${AUTH[@]}" "${JSON[@]}" \
       -d "{\"realm\":\"$REALM\",\"enabled\":true}"

echo "→ client $CLIENT_ID (없으면 생성)"
CID=$(curl -fsS "$KC/admin/realms/$REALM/clients?clientId=$CLIENT_ID" "${AUTH[@]}" \
  | python3 -c "import sys,json;d=json.load(sys.stdin);print(d[0]['id'] if d else '')")
if [ -z "$CID" ]; then
  curl -fsS -o /dev/null -X POST "$KC/admin/realms/$REALM/clients" "${AUTH[@]}" "${JSON[@]}" -d '{
    "clientId":"'"$CLIENT_ID"'","protocol":"openid-connect","publicClient":false,
    "standardFlowEnabled":true,"directAccessGrantsEnabled":true,
    "redirectUris":["http://localhost:3000/*"],"webOrigins":["http://localhost:3000"]}'
  CID=$(curl -fsS "$KC/admin/realms/$REALM/clients?clientId=$CLIENT_ID" "${AUTH[@]}" \
    | python3 -c "import sys,json;print(json.load(sys.stdin)[0]['id'])")
fi
SECRET=$(curl -fsS "$KC/admin/realms/$REALM/clients/$CID/client-secret" "${AUTH[@]}" \
  | python3 -c "import sys,json;print(json.load(sys.stdin)['value'])")

echo "→ tenant_id 하드코딩 클레임 매퍼(=$TENANT_ID, long) — 앱이 JWT의 tenant_id 클레임으로 테넌트 격리"
HAS_MAPPER=$(curl -fsS "$KC/admin/realms/$REALM/clients/$CID/protocol-mappers/models" "${AUTH[@]}" \
  | python3 -c "import sys,json;print('y' if any(m['name']=='tenant_id' for m in json.load(sys.stdin)) else '')")
if [ -z "$HAS_MAPPER" ]; then
  curl -fsS -o /dev/null -X POST "$KC/admin/realms/$REALM/clients/$CID/protocol-mappers/models" "${AUTH[@]}" "${JSON[@]}" -d '{
    "name":"tenant_id","protocol":"openid-connect","protocolMapper":"oidc-hardcoded-claim-mapper",
    "config":{"claim.name":"tenant_id","claim.value":"'"$TENANT_ID"'","jsonType.label":"long",
              "id.token.claim":"true","access.token.claim":"true","userinfo.token.claim":"true"}}'
fi

echo "→ 테스트 계정 $ADMIN_USER (없으면 생성) + 비밀번호"
USUB=$(curl -fsS "$KC/admin/realms/$REALM/users?username=$ADMIN_USER" "${AUTH[@]}" \
  | python3 -c "import sys,json;d=json.load(sys.stdin);print(d[0]['id'] if d else '')")
if [ -z "$USUB" ]; then
  curl -fsS -o /dev/null -X POST "$KC/admin/realms/$REALM/users" "${AUTH[@]}" "${JSON[@]}" -d '{
    "username":"'"$ADMIN_USER"'","email":"admin@erp.local","emailVerified":true,"enabled":true,
    "firstName":"ERP","lastName":"Admin"}'
  USUB=$(curl -fsS "$KC/admin/realms/$REALM/users?username=$ADMIN_USER" "${AUTH[@]}" \
    | python3 -c "import sys,json;print(json.load(sys.stdin)[0]['id'])")
fi
curl -fsS -o /dev/null -X PUT "$KC/admin/realms/$REALM/users/$USUB/reset-password" "${AUTH[@]}" "${JSON[@]}" \
  -d '{"type":"password","value":"'"$ADMIN_PASS"'","temporary":false}'

cat <<EOF

✅ Keycloak 셋업 완료 (realm=$REALM)

  로그인 계정 :  $ADMIN_USER / $ADMIN_PASS   (http://localhost:3000)

  ── 프론트 frontend/.env.local ──
  AUTH_KEYCLOAK_SECRET=$SECRET

  ── 백엔드 환경변수(이 sub에 모든 권한 부트스트랩) ──
  ERP_IAM_BOOTSTRAP_ADMIN_SUB=$USUB
EOF
