# ERP

> 멀티테넌트 SaaS ERP — HR · Finance · Inventory · CRM

[![ci-gate](https://github.com/grinvi04/erp/actions/workflows/ci-gate.yml/badge.svg)](https://github.com/grinvi04/erp/actions/workflows/ci-gate.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

수백 개 기업(테넌트) × 수백 명 사용자 규모를 가정한 상용형 SaaS ERP. 현재 **v0.4.0**.

| 모듈 | 핵심 기능 |
|------|-----------|
| **HR/HCM** | 조직·직원·직위/직급·계약·휴가(결재)·분석 대시보드 |
| **Finance** | 계정과목·전표(GL, 결재 전기)·AP/AR(직무분리 지급/수금)·예산·회계기간·**재무제표(시산표·손익계산서·재무상태표)**·**멀티커런시/FX(환율·거래 스냅샷·환차손익)** |
| **Inventory** | 품목·창고·재고원장·입출이동(조정 결재)·로트/시리얼·분석 대시보드 |
| **CRM** | 고객사·담당자·리드→영업기회·파이프라인·활동·영업팀 데이터스코프 |

공통: 제네릭 **결재 워크플로**(상신→승인/반려/철회)·**RBAC**(기능권한 + 데이터스코프)·**관측성**(traceId·구조화 로깅·Prometheus)·**감사 로그**·기준정보 **텍스트 검색**.

## 아키텍처

```
backend/   Spring Boot 3.4 · Java 21 · Gradle · PostgreSQL 16 · Flyway · Keycloak(OIDC)
frontend/  Next.js 15 (App Router) · TypeScript · next-auth v5(BFF) · Tailwind · shadcn/ui
```

- **클린 아키텍처** — 모듈 내 `domain`(엔티티·도메인서비스) → `application`(유스케이스·포트) → `adapter`(웹·JPA·이벤트) 3계층. 모듈 간은 `common/` 공유타입·SPI로만 통신(직접 참조 금지).
- **멀티테넌시** — 모든 테이블 `tenant_id` + Hibernate `@TenantId` 자동 필터. JWT `tenant_id` 클레임 → `TenantContext`(ThreadLocal).
- **인증·인가** — Backend는 Resource Server(JWT 검증), Frontend는 next-auth BFF(Keycloak). RBAC = Permission(기능권한) + DataScope(전체/부서/본인). 인가는 **DB 기반**(역할→권한) — 기동 시 `ERP_IAM_BOOTSTRAP_ADMIN_SUB` 미설정이면 권한 보유자 없음(fail-closed).
- **DB 표준** — BIGINT PK + 시퀀스 채번, 공통 감사 컬럼(`version` 낙관적잠금·`deleted_at` 소프트삭제·created/updated), Flyway forward-only(`0xxx` common · `1xxx` hr · `2xxx` finance · `3xxx` inventory · `4xxx` crm).

## 로컬 실행

전제: Docker · JDK 21 · Node 20+.

```bash
# 1) 인프라(PostgreSQL + Keycloak) 기동
docker compose up -d

# 2) Keycloak realm `erp` · client `erp-frontend` · 테스트 계정 생성 (멱등)
./scripts/keycloak-setup.sh
#   → 출력의 AUTH_KEYCLOAK_SECRET, ERP_IAM_BOOTSTRAP_ADMIN_SUB 를 아래에 사용

# 3) 백엔드 — 출력된 sub 로 부트스트랩(그 계정에 모든 권한 부여) + Flyway 마이그레이션 적용
cd backend
ERP_IAM_BOOTSTRAP_ADMIN_SUB=<2단계 출력 sub> ./gradlew bootRun
#   헬스: curl -sf http://localhost:8080/actuator/health   # {"status":"UP"}

# 4) 프론트 — frontend/.env.local 작성 후 dev
cd ../frontend && npm install
cat > .env.local <<'EOF'
AUTH_SECRET=<openssl rand -base64 32 로 생성>
AUTH_URL=http://localhost:3000
NEXTAUTH_URL=http://localhost:3000
KEYCLOAK_ISSUER=http://localhost:8180/realms/erp
AUTH_KEYCLOAK_ID=erp-frontend
AUTH_KEYCLOAK_SECRET=<2단계 출력 secret>
BACKEND_URL=http://localhost:8080
EOF
npm run dev
```

| 구성 | URL |
|------|-----|
| 프론트엔드 | http://localhost:3000 |
| 백엔드 | http://localhost:8080 |
| Keycloak Admin | http://localhost:8180 (admin / admin) |

### 테스트 계정

`scripts/keycloak-setup.sh` 가 생성하는 로그인 계정:

| 사용자 | 비밀번호 | 권한 |
|--------|---------|------|
| **admin** | **Admin123!** | SUPER_ADMIN(전 권한) — 백엔드를 이 계정의 Keycloak `sub`로 `ERP_IAM_BOOTSTRAP_ADMIN_SUB` 부트스트랩했을 때 |

http://localhost:3000 → "Keycloak으로 로그인" → `admin / Admin123!`.

> 인가는 DB 기반이라, 위 계정이 권한을 가지려면 **백엔드 기동 시 그 계정의 sub를 `ERP_IAM_BOOTSTRAP_ADMIN_SUB`로 넘겨야** 한다(2~3단계). 다른 사용자는 관리자가 IAM 화면에서 역할을 부여한다.

## 테스트

```bash
# 백엔드 전체 품질 (checkstyle + 단위/통합테스트, Testcontainers Docker 필요)
cd backend && ./gradlew check

# 프론트 타입체크 · 린트 · 빌드
cd frontend && npm run type-check && npm run lint && npm run build

# 프론트 E2E (Playwright — 인증 게이트·인증 렌더 스모크, 자체완결)
cd frontend && npm run build && npm run test:e2e
```

## 디렉토리

```
backend/   Spring Boot (com.erp: common · hr · finance · inventory · crm)
frontend/  Next.js App Router (src/app · components · lib · types)
docs/      specs(기능 스펙)·milestones·deployment
scripts/   keycloak-setup.sh · init-keycloak-schema.sql
```

- 작업 규약: [`AGENTS.md`](AGENTS.md) (AI 도구 공통) · 팀 표준: `github.com/grinvi04/team-harness/docs`
- 기능 스펙: [`docs/specs/`](docs/specs/) (analytics·결재워크플로·FX·재무제표·환차손익 등)

## 라이선스

MIT © grinvi04
