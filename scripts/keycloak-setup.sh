#!/usr/bin/env bash
# 로컬 개발용 Keycloak 셋업 — realm `erp` + client `erp-frontend` + 테스트 계정.
# `docker compose up -d` 로 Keycloak이 뜬 뒤 1회 실행한다(멱등 — 재실행 안전).
#
#   ./scripts/keycloak-setup.sh
#
# 출력: 프론트 클라이언트 시크릿 · 프로비저닝 클라이언트 자격증명 · 로컬 로그인 계정.
set -euo pipefail

KC="${KEYCLOAK_URL:-http://localhost:8180}"
REALM="erp"
CLIENT_ID="erp-frontend"
PROVISIONER_CLIENT_ID="erp-provisioner"
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

echo "→ User Profile tenant_id 관리 속성(운영자만 편집)"
USER_PROFILE=$(curl -fsS "$KC/admin/realms/$REALM/users/profile" "${AUTH[@]}")
UPDATED_PROFILE=$(printf '%s' "$USER_PROFILE" | python3 -c '
import sys,json
d=json.load(sys.stdin)
attrs=d.setdefault("attributes",[])
if not any(a.get("name")=="tenant_id" for a in attrs):
    attrs.append({"name":"tenant_id","displayName":"Tenant ID","multivalued":False,
                  "permissions":{"view":["admin"],"edit":["admin"]}})
print(json.dumps(d))')
curl -fsS -o /dev/null -X PUT "$KC/admin/realms/$REALM/users/profile" \
  "${AUTH[@]}" "${JSON[@]}" -d "$UPDATED_PROFILE"

echo "→ tenant_id 사용자 속성 클레임 매퍼(long) — 사용자별 테넌트 격리"
MAPPER_INFO=$(curl -fsS "$KC/admin/realms/$REALM/clients/$CID/protocol-mappers/models" "${AUTH[@]}" \
  | python3 -c "import sys,json;m=next((m for m in json.load(sys.stdin) if m['name']=='tenant_id'),None);print((m['id']+'|'+m['protocolMapper']) if m else '')")
MAPPER_ID=${MAPPER_INFO%%|*}
MAPPER_TYPE=${MAPPER_INFO#*|}
MAPPER_JSON='{
  "name":"tenant_id","protocol":"openid-connect","protocolMapper":"oidc-usermodel-attribute-mapper",
  "config":{"user.attribute":"tenant_id","claim.name":"tenant_id","jsonType.label":"long",
            "multivalued":"false","id.token.claim":"true","access.token.claim":"true",
            "userinfo.token.claim":"true"}}'
if [ -z "$MAPPER_ID" ]; then
  curl -fsS -o /dev/null -X POST "$KC/admin/realms/$REALM/clients/$CID/protocol-mappers/models" "${AUTH[@]}" "${JSON[@]}" -d '{
    "name":"tenant_id","protocol":"openid-connect","protocolMapper":"oidc-usermodel-attribute-mapper",
    "config":{"user.attribute":"tenant_id","claim.name":"tenant_id","jsonType.label":"long",
              "multivalued":"false","id.token.claim":"true","access.token.claim":"true",
              "userinfo.token.claim":"true"}}'
elif [ "$MAPPER_TYPE" = "oidc-usermodel-attribute-mapper" ]; then
  : # 이미 올바른 mapper 타입이면 유지(멱등)
else
  curl -fsS -o /dev/null -X DELETE \
    "$KC/admin/realms/$REALM/clients/$CID/protocol-mappers/models/$MAPPER_ID" "${AUTH[@]}"
  curl -fsS -o /dev/null -X POST "$KC/admin/realms/$REALM/clients/$CID/protocol-mappers/models" \
    "${AUTH[@]}" "${JSON[@]}" -d "$MAPPER_JSON"
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

echo "→ 테스트 계정 tenant_id 속성(=$TENANT_ID)"
USER_JSON=$(curl -fsS "$KC/admin/realms/$REALM/users/$USUB" "${AUTH[@]}")
UPDATED_USER_JSON=$(printf '%s' "$USER_JSON" \
  | python3 -c "import sys,json;d=json.load(sys.stdin);d.setdefault('attributes',{})['tenant_id']=['$TENANT_ID'];print(json.dumps(d))")
curl -fsS -o /dev/null -X PUT "$KC/admin/realms/$REALM/users/$USUB" "${AUTH[@]}" "${JSON[@]}" \
  -d "$UPDATED_USER_JSON"

echo "→ 운영 프로비저닝 서비스 계정 $PROVISIONER_CLIENT_ID (사용자 조회·수정 전용)"
PCID=$(curl -fsS "$KC/admin/realms/$REALM/clients?clientId=$PROVISIONER_CLIENT_ID" "${AUTH[@]}" \
  | python3 -c "import sys,json;d=json.load(sys.stdin);print(d[0]['id'] if d else '')")
if [ -z "$PCID" ]; then
  curl -fsS -o /dev/null -X POST "$KC/admin/realms/$REALM/clients" "${AUTH[@]}" "${JSON[@]}" -d '{
    "clientId":"'"$PROVISIONER_CLIENT_ID"'","protocol":"openid-connect","publicClient":false,
    "standardFlowEnabled":false,"directAccessGrantsEnabled":false,"serviceAccountsEnabled":true}'
  PCID=$(curl -fsS "$KC/admin/realms/$REALM/clients?clientId=$PROVISIONER_CLIENT_ID" "${AUTH[@]}" \
    | python3 -c "import sys,json;print(json.load(sys.stdin)[0]['id'])")
fi
PROVISIONER_SECRET=$(curl -fsS "$KC/admin/realms/$REALM/clients/$PCID/client-secret" "${AUTH[@]}" \
  | python3 -c "import sys,json;print(json.load(sys.stdin)['value'])")
SERVICE_USER_ID=$(curl -fsS "$KC/admin/realms/$REALM/clients/$PCID/service-account-user" "${AUTH[@]}" \
  | python3 -c "import sys,json;print(json.load(sys.stdin)['id'])")
REALM_MGMT_ID=$(curl -fsS "$KC/admin/realms/$REALM/clients?clientId=realm-management" "${AUTH[@]}" \
  | python3 -c "import sys,json;print(json.load(sys.stdin)[0]['id'])")
PROVISIONER_ROLES=$(curl -fsS "$KC/admin/realms/$REALM/clients/$REALM_MGMT_ID/roles" "${AUTH[@]}" \
  | python3 -c "import sys,json;wanted={'manage-users','view-users','query-users'};print(json.dumps([r for r in json.load(sys.stdin) if r['name'] in wanted]))")
curl -fsS -o /dev/null -X POST \
  "$KC/admin/realms/$REALM/users/$SERVICE_USER_ID/role-mappings/clients/$REALM_MGMT_ID" \
  "${AUTH[@]}" "${JSON[@]}" -d "$PROVISIONER_ROLES"

cat <<EOF

✅ Keycloak 셋업 완료 (realm=$REALM)

  로그인 계정 :  $ADMIN_USER / $ADMIN_PASS   (http://localhost:3000)

  ── 프론트 frontend/.env.local ──
  AUTH_KEYCLOAK_SECRET=$SECRET

  ── 테넌트 프로비저닝 명령 전용 ──
  ERP_KEYCLOAK_PROVISIONING_CLIENT_ID=$PROVISIONER_CLIENT_ID
  ERP_KEYCLOAK_PROVISIONING_CLIENT_SECRET=$PROVISIONER_SECRET
  ERP_PROVISION_ADMIN_USER_ID=$USUB
EOF
