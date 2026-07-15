# 배포 가이드 — Vercel + Railway

ERP를 **프론트=Vercel, 백엔드·DB·Keycloak=Railway** 조합으로 배포한다.

```
[브라우저] ──HTTPS──> [Vercel: Next.js BFF] ──HTTPS──> [Railway: Spring Boot]
                                │                              │
                          [Railway: Keycloak] <───────────────┤
                                │                              │
                          [Railway: PostgreSQL] <──────────────┘
```

- 프론트는 next-auth **BFF** 패턴 — 브라우저는 Next.js 서버만 호출하고, 서버가 백엔드를 호출한다(CORS 불필요).
- 마이그레이션(Flyway)은 백엔드 기동 시 자동 적용된다.

---

## ✅ 배포 전 점검 (pre-flight — v0.5.0 코드 기준 검증)

> 코드가 실제로 읽는 환경변수 이름을 v0.5.0 소스로 교차검증한 결과. 이 표 기준으로 플랫폼 변수를 설정하면 "배포는 됐는데 안 됨"을 피한다.

| 대상 | 변수 | 코드 출처 | 주의 |
|---|---|---|---|
| 백엔드 | `SPRING_DATASOURCE_URL`·`_USERNAME`·`_PASSWORD` | application.yml | — |
| 백엔드 | `KEYCLOAK_ISSUER_URI` | application.yml(resource server) | 백엔드용. `.../realms/erp` |
| 프로비저닝 명령 | `ERP_KEYCLOAK_PROVISIONING_CLIENT_ID`·`_SECRET` | TenantProvisioningConfiguration.java | 테넌트 생성 때만 주입. 일반 백엔드 런타임에는 주입하지 않음 |
| 프론트 | `BACKEND_URL` | lib/api.ts | `NEXT_PUBLIC_API_URL` 폴백 |
| 프론트 | `AUTH_KEYCLOAK_ID`·`AUTH_KEYCLOAK_SECRET` | lib/auth.ts | next-auth Keycloak provider |
| 프론트 | **`KEYCLOAK_ISSUER`** | lib/auth.ts:27,56 | ⚠️ `AUTH_KEYCLOAK_ISSUER` **아님**(관례와 다름) — 틀리면 로그인 깨짐 |
| 프론트 | `AUTH_SECRET`·`AUTH_URL` | next-auth(프레임워크 관례) | `AUTH_SECRET`=`openssl rand -base64 32` |

**기타 검증됨**: `backend/railway.json`(헬스 `/actuator/health`·Dockerfile 빌더)·`frontend` `output:'standalone'`·`flyway.out-of-order:true`(필수, 아래 1-4 주석)·`migration-safety` CI 게이트.

**당신이 인터랙티브로 실행할 것(제가 못 하는 것)**: Railway/Vercel **계정 생성·로그인**, 시크릿 값 생성·주입(DB비번·Keycloak 시크릿·`AUTH_SECRET`), Keycloak realm/client/user 최초 셋업(1-3). 그 외 설정 파일·변수 목록·절차는 이 문서가 준비물.

---

## 0. 비용 (2026 기준, 대략)

| 대상 | 플랜 | 월 비용 | 비고 |
|---|---|---|---|
| Vercel (프론트) | Pro 이상 | 사용량·최신 가격 확인 | Hobby는 개인·비상업 용도이므로 유료 파일럿에 사용 금지 |
| Railway (백엔드+DB+Keycloak) | Pro 이상 | 사용량·최신 가격 확인 | Pro 자체에는 계약 SLA가 없으므로 서비스 정책의 내부 SLO와 구분 |

> 데모와 상용 운영의 플랜을 혼동하지 않는다. 유료 파일럿의 백업·지원·가용성 기준은 [서비스 운영 정책](service-policy.md)을 따르며, 결제 전 각 플랫폼의 최신 공식 가격과 약관을 다시 확인한다. 본 가이드는 Railway에 DB까지 두는 구성을 기준으로 한다.

---

## 1. Railway — 백엔드 스택

유료 파일럿은 모든 Railway 서비스와 PostgreSQL 볼륨을 승인된 동일 리전(현재 권장: 싱가포르)에 배치하고 실제 리전을 운영 기록에 남긴다. 이는 국내 보관을 의미하지 않으며, 미국 소재 Railway와 하위처리자의 계정·지원 처리를 포함한 국외 처리 기준은 [개인정보·법률 준비 기준](privacy-legal-readiness.md)을 따른다.

### 1-1. 프로젝트 + PostgreSQL
1. [railway.app](https://railway.app) 로그인 → **New Project** → **Deploy PostgreSQL**.
2. 생성된 Postgres의 변수 확인(Variables 탭): `PGHOST`·`PGPORT`·`PGUSER`·`PGPASSWORD`·`PGDATABASE`.

### 1-2. Keycloak 서비스
1. **New → Empty Service** → 이름 `keycloak`.
2. Settings → Source → **Docker Image**: `quay.io/keycloak/keycloak:26.0`.
3. Settings → Deploy → **Start Command**:
   ```
   start --optimized --http-enabled=true --proxy-headers=xforwarded --hostname-strict=false
   ```
   (데모면 `start-dev` 도 가능하나 운영은 `start` 권장)
4. Variables:
   ```
   KC_DB=postgres
   KC_DB_URL=jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}
   KC_DB_USERNAME=${{Postgres.PGUSER}}
   KC_DB_PASSWORD=${{Postgres.PGPASSWORD}}
   KC_DB_SCHEMA=keycloak
   KC_HEALTH_ENABLED=true
   KC_LEGACY_OBSERVABILITY_INTERFACE=true
   KC_HOSTNAME=${{RAILWAY_PUBLIC_DOMAIN}}
   KEYCLOAK_ADMIN=admin
   KEYCLOAK_ADMIN_PASSWORD=<강한 비밀번호>
   ```
   `KC_LEGACY_OBSERVABILITY_INTERFACE`는 현재 고정 이미지 `26.0.x`에서 Railway의 단일 공개 포트로 readiness를 확인하기 위한 호환 설정이다. Keycloak 26.4+로 올릴 때는 이를 제거하고 `KC_HTTP_MANAGEMENT_HEALTH_ENABLED=false` 또는 비공개 관리 포트 9000 프로브로 전환한다.
5. Settings → Networking → **Generate Domain** (공개 URL 발급). 이 URL이 Keycloak 이슈어 베이스가 된다.

### 1-3. Realm + 클라이언트 (최초 1회, Keycloak Admin)
`https://<keycloak-domain>/admin` → admin 로그인 →
1. **Create realm**: `erp`
2. **Clients → Create**: Client ID `erp-frontend`, OpenID Connect, **Client authentication ON**(confidential), Standard flow.
3. **Valid redirect URIs**: `https://<vercel-domain>/api/auth/callback/keycloak`
   **Web origins**: `https://<vercel-domain>`
4. Credentials 탭의 **Client secret** 복사 → Vercel `AUTH_KEYCLOAK_SECRET`.
5. **Users → Create** 로 **해당 테넌트 전용** 프로비저닝 운영자 사용자를 새로 만들고 사용자 ID(`sub`)를 기록한다. 기존 고객의 운영자 계정을 재사용하지 않으며 `tenant_id`가 비어 있는지 확인한다. 이 계정은 고객에게 제공하지 않는 break-glass 계정이다.
6. Realm의 User Profile에 `tenant_id` 속성을 추가하고 일반 사용자는 편집할 수 없게 한다.
7. `erp-frontend` 전용 Client scope에 **User Attribute** 매퍼를 추가한다: 사용자 속성 `tenant_id` → 토큰 클레임 `tenant_id`, JSON 타입 `long`.
8. confidential service-account client `erp-provisioner`를 만들고 `realm-management`의 `manage-users`, `view-users`, `query-users`만 부여한다. 이 클라이언트는 테넌트 생성 명령에만 사용한다.

### 1-4. 백엔드 서비스
1. **New → GitHub Repo** → 이 repo 선택 → Settings → **Root Directory**: `backend` (railway.json·Dockerfile 자동 인식).
2. Variables:
   ```
   PORT=8080
   SPRING_DATASOURCE_URL=jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}
   SPRING_DATASOURCE_USERNAME=${{Postgres.PGUSER}}
   SPRING_DATASOURCE_PASSWORD=${{Postgres.PGPASSWORD}}
   KEYCLOAK_ISSUER_URI=https://<keycloak-domain>/realms/erp
   ```
3. Settings → Networking → **Generate Domain** → 백엔드 공개 URL(헬스체크 `/actuator/health`).
4. 첫 배포 후 Flyway가 전체 마이그레이션을 적용한다. 이 시점에는 테넌트와 권한 보유자가 없는 fail-closed 상태다.
   > ℹ️ 백엔드는 `flyway.out-of-order: true`다(접두사 번호 규약 전제 — 새 common 0xxx가 기존 4xxx보다 낮아도 증분 적용). 기존 DB에 배포할 때 이 설정이 없으면 기동 실패하므로 변경 금지. CI의 `migration-safety` 게이트가 이를 강제한다.

### 1-5. 최초 테넌트 생성

운영자 단말에서 DB와 Keycloak에 접근 가능한 상태로 실행한다. 서비스 계정 시크릿은 명령 실행 시에만 환경변수로 주입하고 저장소나 일반 백엔드 서비스 변수에 넣지 않는다.

```bash
cd backend
ERP_KEYCLOAK_BASE_URL=https://<keycloak-domain> \
ERP_KEYCLOAK_REALM=erp \
ERP_KEYCLOAK_PROVISIONING_CLIENT_ID=erp-provisioner \
ERP_KEYCLOAK_PROVISIONING_CLIENT_SECRET=<service-account-secret> \
ERP_PROVISION_TENANT_CODE=<고유-테넌트-코드> \
ERP_PROVISION_TENANT_NAME=<회사명> \
ERP_PROVISION_TENANT_PLAN=STANDARD \
ERP_PROVISION_ADMIN_USER_ID=<1-3에서 새로 만든 테넌트 전용 break-glass 사용자 sub> \
ERP_PROVISIONED_BY=<운영자 식별자> \
./gradlew provisionTenant
```

명령은 테넌트를 `ACTIVE`로 만들고, 테넌트 전용 break-glass 사용자의 `tenant_id` 속성을 연결하며, ERP에 `SUPER_ADMIN` 역할과 전체 권한을 부여하고 감사 로그를 남긴다. Keycloak 사용자는 단일 `tenant_id`만 가지므로 이 계정을 다른 테넌트에 재사용하지 않는다. 이 계정으로 `/iam`에서 고객 관리자용 비-HR 역할을 생성·배정한 뒤 고객 사용자의 `SUPER_ADMIN`·`hr:*` 권한이 0개인지 확인한다. 일부 단계 실패 시 상태는 `FAILED`로 남으며 같은 코드에 `ERP_PROVISION_RETRY=true`를 더해 재시도한다. 일반 API는 `ACTIVE` 테넌트의 JWT만 허용한다.

프로비저닝 성공 로그의 숫자형 `tenantId`를 승인 기록과 대조한 뒤 고객 관리자를 별도로 개통한다.

1. Keycloak에서 고객 업무 관리자를 새로 만들고 관리자 전용 `tenant_id` 속성에 위 `tenantId`를 설정한다. 고객이 이 속성을 수정할 수 있게 하지 않는다.
2. break-glass 계정으로 ERP `/iam`에 로그인해 Finance·Inventory·CRM에 필요한 권한만 가진 고객 업무 관리자 역할을 만들고 고객 사용자 `sub`에 배정한다. 고객 역할에는 `iam:write`를 부여하지 않는다.
3. 고객 업무 관리자 계정으로 새 로그인해 JWT의 `tenant_id`, 허용 메뉴, 보호 API 접근을 확인하고 HR·IAM 관리 메뉴 미노출과 HR·IAM 쓰기 API 403을 검증한다.
4. 고객 계정에 `SUPER_ADMIN`, `hr:*`, `iam:write`가 하나라도 있거나 `tenant_id`가 다르면 개통을 중단한다.

추가 사용자 `tenant_id` 설정과 역할 변경은 현재 운영자 대행 작업이다. 고객 요청은 승인된 지원 채널로 받고 break-glass 사용 사유와 변경 전후를 감사 기록에 남긴다. #189의 스테이징 리허설에서 이 절차와 증거를 검증하고, 반복 고객 온보딩 전 안전한 위임 제한과 자동화 여부를 별도 평가한다.

---

## 2. Vercel — 프론트엔드

Vercel Pro DPA와 하위처리자 목록을 확인하고 함수의 실제 실행 리전을 운영 기록에 남긴다. Next.js BFF가 로그인 세션과 업무 API 응답을 처리하므로 정적 화면 호스팅으로만 분류하지 않는다.

1. [vercel.com](https://vercel.com) → **Add New → Project** → 이 repo import.
2. **Root Directory**: `frontend` (Next.js 자동 감지, `output: 'standalone'`).
3. **Environment Variables**:
   ```
   BACKEND_URL=https://<백엔드-railway-domain>
   AUTH_SECRET=<openssl rand -base64 32>
   AUTH_URL=https://<vercel-domain>
   AUTH_KEYCLOAK_ID=erp-frontend
   AUTH_KEYCLOAK_SECRET=<Keycloak 클라이언트 시크릿>
   KEYCLOAK_ISSUER=https://<keycloak-domain>/realms/erp
   ```
   > ⚠️ 이슈어 변수명은 **`KEYCLOAK_ISSUER`** 다(`src/lib/auth.ts`가 읽는 이름 — next-auth 관례 `AUTH_KEYCLOAK_ISSUER`가 **아니다**). 잘못 쓰면 issuer가 undefined로 로그인이 깨진다.
4. Deploy. 발급된 도메인을 Keycloak redirect URI(1-3)에 반영.

---

## 3. 자동 배포 (CD)

Railway·Vercel 모두 **GitHub 연동**이라 **main 푸시 시 자동 재배포**된다(별도 GH Actions 워크플로·CI 분 소모 없음 — 비용 최적). 릴리즈(`/release`)가 main에 머지되면 양쪽이 자동 배포.

- 프리뷰: Vercel은 PR마다 프리뷰 URL 생성(선택).
- 마이그레이션은 백엔드 기동 시 자동(무중단·forward-only 원칙 — `db-standards`).

---

## 4. 배포 후 헬스체크

```bash
curl -sf https://<백엔드-domain>/actuator/health      # {"status":"UP"}
curl -sf https://<keycloak-domain>/health/ready        # Keycloak
curl -sf https://<vercel-domain>/api/auth/session      # 프론트(미로그인 시 빈 세션)
```

운영자 break-glass 계정으로 로그인 → `/iam`에서 비-HR 고객 관리자 역할·배정 관리 → 별도 고객 관리자로 권한 경계를 재확인. 운영자 계정은 일상 업무에 사용하지 않는다.

---

## 5. 시크릿 관리

- 모든 시크릿(DB 비번·Keycloak 시크릿·AUTH_SECRET)은 **각 플랫폼 Variables/Environment** 에만 둔다. **repo·`.env` 커밋 금지**(secret-scan CI가 차단).
- 프로비저닝 서비스 계정 시크릿은 테넌트 생성 작업에만 단기 주입하고 정기적으로 회전한다.
