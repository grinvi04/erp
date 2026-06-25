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

## 0. 비용 (2026 기준, 대략)

| 대상 | 플랜 | 월 비용 | 비고 |
|---|---|---|---|
| Vercel (프론트) | Hobby(무료) | $0 | 개인/데모 충분. 상용 트래픽·팀은 Pro($20) |
| Railway (백엔드+DB+Keycloak) | 사용량 기반 | **~$5~15** | 무료 크레딧 소진 후 과금. Keycloak이 메모리(~512MB+) 주 비용 |

> 비용 최적화: DB를 **Neon(무료 Postgres)** 로 빼면 Railway는 백엔드+Keycloak만 → 비용 절감. 단 본 가이드는 요청대로 Railway에 DB까지 둔다.

---

## 1. Railway — 백엔드 스택

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
   KC_HOSTNAME=${{RAILWAY_PUBLIC_DOMAIN}}
   KEYCLOAK_ADMIN=admin
   KEYCLOAK_ADMIN_PASSWORD=<강한 비밀번호>
   ```
5. Settings → Networking → **Generate Domain** (공개 URL 발급). 이 URL이 Keycloak 이슈어 베이스가 된다.

### 1-3. Realm + 클라이언트 (최초 1회, Keycloak Admin)
`https://<keycloak-domain>/admin` → admin 로그인 →
1. **Create realm**: `erp`
2. **Clients → Create**: Client ID `erp-frontend`, OpenID Connect, **Client authentication ON**(confidential), Standard flow.
3. **Valid redirect URIs**: `https://<vercel-domain>/api/auth/callback/keycloak`
   **Web origins**: `https://<vercel-domain>`
4. Credentials 탭의 **Client secret** 복사 → Vercel `AUTH_KEYCLOAK_SECRET`.
5. **Users → Create** 로 첫 사용자 생성 → 이 사용자의 **sub(ID)** 를 백엔드 부트스트랩에 사용(아래 1-4).
   - 토큰의 `tenant_id` 클레임: Realm/Client에 **tenant_id(상수 1)** 매퍼 추가(Client scopes → erp-frontend-dedicated → Add mapper → Hardcoded claim `tenant_id`=`1`).

### 1-4. 백엔드 서비스
1. **New → GitHub Repo** → 이 repo 선택 → Settings → **Root Directory**: `backend` (railway.json·Dockerfile 자동 인식).
2. Variables:
   ```
   PORT=8080
   SPRING_DATASOURCE_URL=jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}
   SPRING_DATASOURCE_USERNAME=${{Postgres.PGUSER}}
   SPRING_DATASOURCE_PASSWORD=${{Postgres.PGPASSWORD}}
   KEYCLOAK_ISSUER_URI=https://<keycloak-domain>/realms/erp
   ERP_IAM_BOOTSTRAP_ADMIN_SUB=<1-3에서 만든 사용자 sub>
   ERP_IAM_BOOTSTRAP_TENANT_ID=1
   ```
3. Settings → Networking → **Generate Domain** → 백엔드 공개 URL(헬스체크 `/actuator/health`).
4. 첫 배포 후 Flyway가 V0001~V2002 적용. 부트스트랩이 해당 sub에 SUPER_ADMIN 자동 배정.

> `ERP_IAM_BOOTSTRAP_ADMIN_SUB` 미설정으로 기동하면 **권한 보유자가 없어** 관리 API를 쓸 수 없다(설계상 fail-closed). 반드시 설정.

---

## 2. Vercel — 프론트엔드

1. [vercel.com](https://vercel.com) → **Add New → Project** → 이 repo import.
2. **Root Directory**: `frontend` (Next.js 자동 감지, `output: 'standalone'`).
3. **Environment Variables**:
   ```
   BACKEND_URL=https://<백엔드-railway-domain>
   AUTH_SECRET=<openssl rand -base64 32>
   AUTH_URL=https://<vercel-domain>
   AUTH_KEYCLOAK_ID=erp-frontend
   AUTH_KEYCLOAK_SECRET=<Keycloak 클라이언트 시크릿>
   AUTH_KEYCLOAK_ISSUER=https://<keycloak-domain>/realms/erp
   ```
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

로그인 → `/iam`(SUPER_ADMIN) 에서 역할·배정 관리 → 다른 사용자에게 권한 부여.

---

## 5. 시크릿 관리

- 모든 시크릿(DB 비번·Keycloak 시크릿·AUTH_SECRET)은 **각 플랫폼 Variables/Environment** 에만 둔다. **repo·`.env` 커밋 금지**(secret-scan CI가 차단).
- `ERP_IAM_BOOTSTRAP_ADMIN_SUB` 등 운영 값도 플랫폼 변수로.
