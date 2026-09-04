#!/usr/bin/env bash
# 로컬 Keycloak + Mailpit 사용자 초대 실증. 시크릿·토큰은 출력하지 않는다.
set -euo pipefail

ROOT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
BACKEND_URL=${BACKEND_URL:-http://localhost:8080}
KEYCLOAK_URL=${KEYCLOAK_URL:-http://localhost:8180}
MAILPIT_URL=${MAILPIT_URL:-http://localhost:8025}
REALM=${KEYCLOAK_REALM:-erp}
EMAIL=${ONBOARDING_UAT_EMAIL:-onboarding-uat@erp.local}
REQUEST_KEY=${ONBOARDING_UAT_REQUEST_KEY:-onboarding-uat-v1}
FOREIGN_EMAIL=${ONBOARDING_UAT_FOREIGN_EMAIL:-onboarding-cross-tenant-uat@erp.local}
FOREIGN_REQUEST_KEY=${ONBOARDING_UAT_FOREIGN_REQUEST_KEY:-onboarding-cross-tenant-uat-v1}
TRACE_ID=0123456789abcdef0123456789abcdef

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

compose_environment() {
  local key=$1
  docker compose -f "$ROOT_DIR/docker-compose.yml" config --format json \
    | python3 -c "import json,sys; print(json.load(sys.stdin)['services']['keycloak']['environment']['$key'])"
}

KC_ADMIN_USERNAME=${E2E_COMMERCIAL_KC_ADMIN_USERNAME:-$(compose_environment KEYCLOAK_ADMIN)}
KC_ADMIN_PASSWORD=${E2E_COMMERCIAL_KC_ADMIN_PASSWORD:-$(compose_environment KEYCLOAK_ADMIN_PASSWORD)}
LOGIN_USERNAME=${ONBOARDING_UAT_ADMIN_USERNAME:-admin}
LOGIN_PASSWORD=${ONBOARDING_UAT_ADMIN_PASSWORD:-Admin123!}
FRONTEND_CLIENT_ID=${AUTH_KEYCLOAK_ID:-erp-frontend}

echo "→ 로컬 서비스 준비 확인"
curl -fsS "$BACKEND_URL/actuator/health" >/dev/null
curl -fsS "$KEYCLOAK_URL/health/ready" >/dev/null
curl -fsS "$MAILPIT_URL/api/v1/messages" >/dev/null

KC_ADMIN_TOKEN=$(curl -fsS -X POST \
  "$KEYCLOAK_URL/realms/master/protocol/openid-connect/token" \
  -d "username=$KC_ADMIN_USERNAME" -d "password=$KC_ADMIN_PASSWORD" \
  -d "grant_type=password" -d "client_id=admin-cli" \
  | python3 -c "import json,sys; print(json.load(sys.stdin)['access_token'])")

USER_ADMIN_CLIENT_UUID=$(curl -fsS -G \
  "$KEYCLOAK_URL/admin/realms/$REALM/clients" \
  -H "Authorization: Bearer $KC_ADMIN_TOKEN" \
  --data-urlencode "clientId=erp-user-admin" \
  | python3 -c "import json,sys; d=json.load(sys.stdin); print(d[0]['id'] if len(d)==1 else '')")
if [[ -z "$USER_ADMIN_CLIENT_UUID" ]]; then
  echo "erp-user-admin 서비스 계정이 없습니다. scripts/keycloak-setup.sh를 먼저 실행하세요." >&2
  exit 2
fi

LOGIN_TOKEN=$(curl -fsS -X POST \
  "$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token" \
  -d "grant_type=password" -d "client_id=$FRONTEND_CLIENT_ID" \
  -d "client_secret=${AUTH_KEYCLOAK_SECRET:?AUTH_KEYCLOAK_SECRET is required}" \
  -d "username=$LOGIN_USERNAME" -d "password=$LOGIN_PASSWORD" \
  | python3 -c "import json,sys; print(json.load(sys.stdin)['access_token'])")
AUTH_HEADER=(-H "Authorization: Bearer $LOGIN_TOKEN")
TRACE_HEADER=(-H "traceparent: 00-$TRACE_ID-0123456789abcdef-01")
TENANT_ID=$(python3 -c '
import base64,json,sys
payload=sys.argv[1].split(".")[1]
payload += "=" * (-len(payload) % 4)
print(json.loads(base64.urlsafe_b64decode(payload))["tenant_id"])' "$LOGIN_TOKEN")

echo "→ 검증용 역할과 기존 사용자 상태 확인"
ROLES=$(curl -fsS "$BACKEND_URL/api/iam/roles" "${AUTH_HEADER[@]}")
ROLE_ID=$(printf '%s' "$ROLES" | python3 -c '
import json,sys
d=json.load(sys.stdin).get("data",[])
role=next((r for r in d if r.get("code")=="ONBOARDING_UAT"),None)
print(role["id"] if role else "")')
if [[ -z "$ROLE_ID" ]]; then
  ROLE_BODY='{"code":"ONBOARDING_UAT","name":"온보딩 UAT","description":"로컬 사용자 초대 검증 전용","permissions":["finance:read"]}'
  ROLE_ID=$(curl -fsS -X POST "$BACKEND_URL/api/iam/roles" "${AUTH_HEADER[@]}" \
    -H "Content-Type: application/json" -d "$ROLE_BODY" \
    | python3 -c 'import json,sys; print(json.load(sys.stdin)["data"]["id"])')
fi

USERS=$(curl -fsS "$BACKEND_URL/api/iam/tenant-users" "${AUTH_HEADER[@]}")
EXISTING=$(printf '%s' "$USERS" | python3 -c '
import json,sys
email=sys.argv[1]
u=next((u for u in json.load(sys.stdin).get("data",[]) if u.get("email")==email),None)
print((str(u["id"])+"|"+u["status"]) if u else "")' "$EMAIL")

if [[ -n "$EXISTING" ]]; then
  USER_RECORD_ID=${EXISTING%%|*}
  USER_STATUS=${EXISTING#*|}
  if [[ "$USER_STATUS" == "ACTIVE" ]]; then
    curl -fsS -X DELETE "$BACKEND_URL/api/iam/tenant-users/$USER_RECORD_ID" \
      "${AUTH_HEADER[@]}" "${TRACE_HEADER[@]}" >/dev/null
    USER_STATUS=DISABLED
  fi
else
  USER_RECORD_ID=""
  USER_STATUS=""
fi

ROLE_BODY=$(python3 -c 'import json,sys; print(json.dumps({"roleIds":[int(sys.argv[1])]}))' "$ROLE_ID")
if [[ "$USER_STATUS" == "DISABLED" || "$USER_STATUS" == "FAILED" ]]; then
  echo "→ 기존 검증 계정 재초대"
  RESULT=$(curl -fsS -X POST \
    "$BACKEND_URL/api/iam/tenant-users/$USER_RECORD_ID/reinvite" \
    "${AUTH_HEADER[@]}" "${TRACE_HEADER[@]}" -H "Content-Type: application/json" \
    -d "$ROLE_BODY")
else
  echo "→ 새 검증 계정 초대"
  INVITE_BODY=$(python3 -c '
import json,sys
print(json.dumps({"email":sys.argv[1],"firstName":"Onboarding","lastName":"UAT",
                  "requestKey":sys.argv[2],"roleIds":[int(sys.argv[3])]}))' \
    "$EMAIL" "$REQUEST_KEY" "$ROLE_ID")
  RESULT=$(curl -fsS -X POST "$BACKEND_URL/api/iam/tenant-users/invitations" \
    "${AUTH_HEADER[@]}" "${TRACE_HEADER[@]}" -H "Content-Type: application/json" \
    -d "$INVITE_BODY")
fi

RESULT_FIELDS=$(printf '%s' "$RESULT" | python3 -c '
import json,sys
d=json.load(sys.stdin).get("data") or {}
print("|".join([str(d.get("id","")),str(d.get("userId","")),str(d.get("status",""))]))')
USER_RECORD_ID=${RESULT_FIELDS%%|*}
RESULT_REST=${RESULT_FIELDS#*|}
KEYCLOAK_USER_ID=${RESULT_REST%%|*}
USER_STATUS=${RESULT_REST#*|}
if [[ -z "$USER_RECORD_ID" || -z "$KEYCLOAK_USER_ID" || "$USER_STATUS" != "ACTIVE" ]]; then
  echo "초대 결과가 ACTIVE가 아닙니다." >&2
  exit 1
fi

echo "→ Mailpit 초대 메일 수신 확인"
MAIL_RECEIVED=false
for _ in {1..20}; do
  if curl -fsS -G "$MAILPIT_URL/view/latest.txt" \
    --data-urlencode "query=to:$EMAIL" >/dev/null 2>&1; then
    MAIL_RECEIVED=true
    break
  fi
  sleep 0.5
done
if [[ "$MAIL_RECEIVED" != "true" ]]; then
  echo "10초 안에 초대 메일을 수신하지 못했습니다." >&2
  exit 1
fi

echo "→ Keycloak 테넌트 소유권 표식 확인"
IDENTITY=$(curl -fsS -G "$KEYCLOAK_URL/admin/realms/$REALM/users" \
  -H "Authorization: Bearer $KC_ADMIN_TOKEN" \
  --data-urlencode "email=$EMAIL" --data-urlencode "exact=true")
printf '%s' "$IDENTITY" | python3 -c '
import json,sys
users=json.load(sys.stdin)
expected_id,expected_key,expected_tenant=sys.argv[1:4]
if len(users)!=1: raise SystemExit("Keycloak identity count mismatch")
u=users[0]; attrs=u.get("attributes",{})
if u.get("id")!=expected_id or not u.get("enabled"): raise SystemExit("identity state mismatch")
if attrs.get("tenant_id")!=[expected_tenant]: raise SystemExit("tenant marker mismatch")
if attrs.get("erp_invitation_key")!=[expected_key]: raise SystemExit("invitation marker mismatch")' \
  "$KEYCLOAK_USER_ID" "$REQUEST_KEY" "$TENANT_ID"

echo "→ 로그인 중지와 역할 회수 확인"
curl -fsS -X DELETE "$BACKEND_URL/api/iam/tenant-users/$USER_RECORD_ID" \
  "${AUTH_HEADER[@]}" "${TRACE_HEADER[@]}" >/dev/null
IDENTITY_AFTER=$(curl -fsS \
  "$KEYCLOAK_URL/admin/realms/$REALM/users/$KEYCLOAK_USER_ID" \
  -H "Authorization: Bearer $KC_ADMIN_TOKEN")
printf '%s' "$IDENTITY_AFTER" | python3 -c '
import json,sys
if json.load(sys.stdin).get("enabled") is not False: raise SystemExit("identity was not disabled")'
ASSIGNED_ROLES=$(curl -fsS \
  "$BACKEND_URL/api/iam/users/$KEYCLOAK_USER_ID/roles" "${AUTH_HEADER[@]}")
printf '%s' "$ASSIGNED_ROLES" | python3 -c '
import json,sys
if json.load(sys.stdin).get("data") != []: raise SystemExit("roles were not revoked")'

echo "→ 상태 변경 감사 이전·이후 값과 traceId 확인"
AUDIT_ID=$(curl -fsS -G "$BACKEND_URL/api/audit/logs" "${AUTH_HEADER[@]}" \
  --data-urlencode "entityType=TENANT_USER" --data-urlencode "entityId=$USER_RECORD_ID" \
  --data-urlencode "action=UPDATE" --data-urlencode "size=10" \
  | python3 -c 'import json,sys; rows=json.load(sys.stdin)["data"]["content"]; print(rows[0]["id"] if rows else "")')
AUDIT_DETAIL=$(curl -fsS "$BACKEND_URL/api/audit/logs/$AUDIT_ID" "${AUTH_HEADER[@]}")
printf '%s' "$AUDIT_DETAIL" | python3 -c '
import json,sys
d=json.load(sys.stdin).get("data") or {}
if d.get("traceId")!=sys.argv[1]: raise SystemExit("audit traceId mismatch")
before=d.get("beforeData") or ""; after=d.get("afterData") or ""
if "ACTIVE" not in before or "DISABLED" not in after: raise SystemExit("audit before/after mismatch")' \
  "$TRACE_ID"

echo "→ 교차 테넌트 기존 Keycloak 계정 재사용 거부 확인"
FOREIGN_EXISTING_ID=$(curl -fsS -G "$KEYCLOAK_URL/admin/realms/$REALM/users" \
  -H "Authorization: Bearer $KC_ADMIN_TOKEN" --data-urlencode "email=$FOREIGN_EMAIL" \
  --data-urlencode "exact=true" \
  | python3 -c 'import json,sys; d=json.load(sys.stdin); print(d[0]["id"] if d else "")')
if [[ -n "$FOREIGN_EXISTING_ID" ]]; then
  curl -fsS -o /dev/null -X DELETE \
    "$KEYCLOAK_URL/admin/realms/$REALM/users/$FOREIGN_EXISTING_ID" \
    -H "Authorization: Bearer $KC_ADMIN_TOKEN"
fi
FOREIGN_TENANT_ID=$((TENANT_ID + 1000))
FOREIGN_IDENTITY_BODY=$(python3 -c '
import json,sys
print(json.dumps({"username":sys.argv[1],"email":sys.argv[1],"enabled":True,
                  "attributes":{"tenant_id":[sys.argv[2]],
                                "erp_invitation_key":[sys.argv[3]]}}))' \
  "$FOREIGN_EMAIL" "$FOREIGN_TENANT_ID" "$FOREIGN_REQUEST_KEY")
curl -fsS -o /dev/null -X POST "$KEYCLOAK_URL/admin/realms/$REALM/users" \
  -H "Authorization: Bearer $KC_ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d "$FOREIGN_IDENTITY_BODY"
FOREIGN_IDENTITY_ID=$(curl -fsS -G "$KEYCLOAK_URL/admin/realms/$REALM/users" \
  -H "Authorization: Bearer $KC_ADMIN_TOKEN" --data-urlencode "email=$FOREIGN_EMAIL" \
  --data-urlencode "exact=true" \
  | python3 -c 'import json,sys; d=json.load(sys.stdin); print(d[0]["id"] if len(d)==1 else "")')
FOREIGN_INVITE_BODY=$(python3 -c '
import json,sys
print(json.dumps({"email":sys.argv[1],"firstName":"Cross","lastName":"Tenant",
                  "requestKey":sys.argv[2],"roleIds":[int(sys.argv[3])]}))' \
  "$FOREIGN_EMAIL" "$FOREIGN_REQUEST_KEY" "$ROLE_ID")
REJECTION=$(curl -sS -X POST "$BACKEND_URL/api/iam/tenant-users/invitations" \
  "${AUTH_HEADER[@]}" "${TRACE_HEADER[@]}" -H "Content-Type: application/json" \
  -d "$FOREIGN_INVITE_BODY" -w $'\n%{http_code}')
REJECTION_BODY=${REJECTION%$'\n'*}
REJECTION_STATUS=${REJECTION##*$'\n'}
if [[ "$REJECTION_STATUS" != "409" ]]; then
  echo "교차 테넌트 계정 재사용이 HTTP $REJECTION_STATUS 로 처리됐습니다." >&2
  exit 1
fi
printf '%s' "$REJECTION_BODY" | python3 -c '
import json,sys
if (json.load(sys.stdin).get("error") or {}).get("code")!="C008":
    raise SystemExit("identity conflict error code mismatch")'
FOREIGN_AFTER=$(curl -fsS "$KEYCLOAK_URL/admin/realms/$REALM/users/$FOREIGN_IDENTITY_ID" \
  -H "Authorization: Bearer $KC_ADMIN_TOKEN")
printf '%s' "$FOREIGN_AFTER" | python3 -c '
import json,sys
u=json.load(sys.stdin)
if not u.get("enabled"): raise SystemExit("foreign identity was mutated")
if u.get("attributes",{}).get("tenant_id")!=[sys.argv[1]]:
    raise SystemExit("foreign tenant marker was mutated")' "$FOREIGN_TENANT_ID"
curl -fsS -o /dev/null -X DELETE \
  "$KEYCLOAK_URL/admin/realms/$REALM/users/$FOREIGN_IDENTITY_ID" \
  -H "Authorization: Bearer $KC_ADMIN_TOKEN"

echo "✅ 사용자 초대 UAT 통과 — 정상·교차테넌트 거부·메일·감사·중지·역할 회수"
