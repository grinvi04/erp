# AGENTS.md — 프로젝트 작업 규약 (AI 도구 공통)

> 이 파일은 **모든 AI 코딩 도구의 단일 규약 출처**다.
> Claude Code는 CLAUDE.md의 `@AGENTS.md` import로, Codex는 네이티브로,
> Gemini CLI는 contextFileName 설정으로 이 파일을 읽는다.
> 도구별 전용 지침은 각 도구의 파일(CLAUDE.md 등)에만 쓴다.

## 프로젝트 개요

**멀티테넌트 SaaS ERP** — 수백 개 기업(테넌트) × 수백 명 사용자 규모의 실제 상용 제품.
4개 모듈: HR/HCM(인사), Finance(재무), Inventory(재고·물류), CRM(영업).

**기술 스택**
- Backend: Spring Boot 3.4.x · Java 21 · Gradle · PostgreSQL 16 · Flyway · Keycloak OIDC
- Frontend: Next.js 15 (App Router) · TypeScript · next-auth v5 · Tailwind CSS · shadcn/ui
- Auth: Keycloak (단일 realm, tenant_id JWT claim) — Backend: Resource Server, Frontend: BFF
- Infra: Docker Compose (로컬), GitHub Actions CI

**디렉토리 구조**
```
erp/
├── backend/                          # Spring Boot (Clean Architecture)
│   ├── src/main/java/com/erp/
│   │   ├── common/                   # 공통 기반 (전 모듈 공유)
│   │   │   ├── audit/               # AuditLog 엔티티·서비스
│   │   │   ├── exception/           # ErpException, ErrorCode, GlobalExceptionHandler
│   │   │   ├── response/            # ApiResponse<T>, PageResponse<T>
│   │   │   ├── security/            # SecurityConfig, JwtTenantFilter, CurrentUserProvider
│   │   │   ├── tenant/              # TenantContext(ThreadLocal), TenantHibernateFilter
│   │   │   └── workflow/            # ApprovalWorkflow 엔진 (제네릭 결재선)
│   │   ├── hr/                      # 인사 모듈
│   │   │   ├── domain/             # 엔티티, 도메인 서비스, 리포지토리 인터페이스
│   │   │   ├── application/        # 유스케이스(서비스), DTO, 포트
│   │   │   └── adapter/            # Controller, JPA 구현체, 이벤트 핸들러
│   │   ├── finance/                 # 재무 모듈 (동일 구조)
│   │   ├── inventory/               # 재고 모듈 (동일 구조)
│   │   └── crm/                     # CRM 모듈 (동일 구조)
│   └── src/main/resources/
│       └── db/migration/            # Flyway: 0xxx=common, 1xxx=hr, 2xxx=finance, 3xxx=inventory, 4xxx=crm
├── frontend/                         # Next.js App Router
│   ├── src/app/
│   │   ├── (auth)/                  # 로그인
│   │   ├── hr/                      # 인사 화면
│   │   ├── finance/                 # 재무 화면
│   │   ├── inventory/               # 재고 화면
│   │   └── crm/                     # CRM 화면
│   ├── src/components/              # 공통 UI (shadcn/ui 기반)
│   ├── src/lib/                     # API 클라이언트, 유틸
│   └── src/types/                   # 공유 타입
└── docs/
    ├── specs/                        # /plan 스펙 파일
    └── milestones/                   # /milestone 추적 파일
```

> **이 repo의 프로젝트 상태는 repo/GitHub에 둔다.** 플랜·스펙은 `docs/specs/`, 백로그·할 일은 GitHub Issues + Milestone(`/milestone`), 작업로그는 git 히스토리 + CHANGELOG/릴리즈노트, 설계 결정·도메인 지식은 `docs/decisions.md`에 기록한다. **도구 로컬 AI 메모리(예: `~/.claude` 메모리)에 프로젝트 상태·백로그·작업로그·결정·도메인 지식을 두지 않는다**(다른 PC·세션·사람이 못 보고 유실). 로컬 메모리는 팀 공유 불필요한 *개인 작업습관*에만 최소로. (정본: `ai-collaboration.md`)

## 아키텍처 원칙

### Clean Architecture — 모듈 내 3계층
1. `domain/` — 엔티티, 값 객체, 도메인 서비스, 리포지토리 인터페이스 (외부 의존 0)
2. `application/` — 유스케이스(서비스), DTO, 포트(인터페이스). domain에만 의존
3. `adapter/` — Controller, JPA 구현체, 이벤트 핸들러. application 포트에 의존

### 모듈 간 경계
- 모듈 직접 참조 금지 — `common/` 공유 타입 또는 Spring 도메인 이벤트로만 통신
- 크로스 스키마 JOIN 금지 — 각 모듈은 자기 스키마(hr/finance/inventory/crm)만 접근

### 멀티테넌시
- 단일 PostgreSQL DB, 모든 테이블에 `tenant_id` 컬럼
- Hibernate `@Filter(TenantFilter)` 자동 적용 — 코드에서 수동 필터링 금지
- `TenantContext`(ThreadLocal) → Keycloak JWT `tenant_id` claim에서 추출
- 테넌트 간 데이터 참조·조인 절대 금지

### DB 표준
- PK: `BIGINT` + `SEQUENCE` (채번)
- 모든 테이블 필수 컬럼: `tenant_id BIGINT NOT NULL`, `version BIGINT NOT NULL DEFAULT 0` (낙관적 잠금),
  `deleted_at TIMESTAMP` (소프트삭제), `created_at`, `updated_at`, `created_by`, `updated_by`
- Flyway: forward-only, `V{모듈번호}{순번}__설명.sql` 네이밍
  - 0xxx: common, 1xxx: hr, 2xxx: finance, 3xxx: inventory, 4xxx: crm

### 인증·인가
- Backend: Spring Security Resource Server (JWT Bearer 검증)
- Frontend: next-auth v5 BFF 패턴 (Keycloak provider)
- RBAC: Permission(기능 권한) + DataScope(부서/팀/전체 데이터 범위) 이중 구조
- CI: `spring.security.enabled=false` (test profile) — Keycloak 없이 실행

## 브랜치 정책 (git flow)

- `main` / `develop` 직접 커밋·push **금지** — branch protection으로 서버에서 강제됨
- 작업 브랜치: `feature/*`, `fix/*`, `hotfix/*`, `release/*`
- 모든 변경은 PR 경유: 브랜치 → PR 생성 → 리뷰·CI 게이트 통과 → 머지
- hotfix는 main 기준 분기 후 **main과 develop 양쪽에** 반영

## 품질 게이트

- 커밋 전: lint + test 통과 필수
- PR 머지 전: CI 전체 통과 + 리뷰 스레드 전부 resolve (+ 리뷰어 있으면 승인 1명 이상)
- **솔로 운용**: 리뷰어가 없으면 승인 충족이 불가하므로 두 방식을 자유 선택 — (a) develop 승인요건 유지 + `/solo-merge`(또는 `sm <PR>` 별칭)로 머지 시 일시 우회·즉시 복구, 또는 (b) develop 승인요건 제거 후 CI 게이트만으로 일반 머지. **main(릴리즈)은 보호 유지.** 상세: team-harness `code-review.md` 솔로 정책.
- 테스트 스킵 플래그(`-DskipTests` 등) 사용 금지

## 빌드·테스트 명령

- 백엔드 품질 검증: `cd backend && ./gradlew check`
- 백엔드 테스트: `cd backend && ./gradlew test`
- 백엔드 빌드: `cd backend && ./gradlew build`
- 프론트엔드 타입 체크: `cd frontend && npm run type-check`
- 프론트엔드 린트: `cd frontend && npm run lint`
- 프론트엔드 빌드: `cd frontend && npm run build`

## 호스팅·배포 대상

| 구성 | 대상 | 비용 | 비고 |
|---|---|---|---|
| 프론트 (Next.js BFF) | **Vercel** (Hobby 무료) | $0 | `output: standalone`, root=`frontend` |
| 백엔드 (Spring Boot) | **Railway** (Dockerfile) | 사용량 | root=`backend`, `railway.json` 헬스체크 |
| PostgreSQL 16 | **Railway** Postgres 플러그인 | 사용량 | (대안: Neon 무료로 비용↓) |
| Keycloak 26 (OIDC) | **Railway** 서비스 | 사용량(메모리 주비용) | 단일 realm `erp` |

- **CD**: Railway·Vercel **GitHub 연동**으로 main 푸시 시 자동 재배포(GH Actions 분 미소모). 릴리즈가 main 머지되면 자동 배포.
- 상세 절차·환경변수·시크릿·Keycloak realm 설정: **`docs/deployment.md`**.
- ⚠️ 인가 DB 기반 → 기동 시 `ERP_IAM_BOOTSTRAP_ADMIN_SUB`(+`ERP_IAM_BOOTSTRAP_TENANT_ID`) 미설정이면 권한 보유자 없음(fail-closed). 필수 설정.

## 배포·헬스체크 명령

**로컬 (docker-compose)**
- 로컬 인프라 실행 (PostgreSQL · Keycloak): `docker compose up -d`
- 백엔드 헬스체크: `curl -sf http://localhost:8080/actuator/health`
- Keycloak 헬스체크: `curl -sf http://localhost:8180/health/ready`
- 프론트엔드 헬스체크: `curl -sf http://localhost:3000/api/auth/session`
- 전체 스택 중지: `docker compose down` / 데이터 초기화: `docker compose down -v`

**운영 (배포 후)** — ⚠️ **2026-06 현재 erp는 운영 미배포** (Railway/Vercel 프로젝트 미생성).
배포 시 아래 `<...>`를 발급 도메인으로 교체하고 이 줄을 제거할 것.
- 백엔드: `curl -sf https://<backend>.up.railway.app/actuator/health`
- Keycloak: `curl -sf https://<keycloak>.up.railway.app/health/ready`
- 프론트: `curl -sf https://<app>.vercel.app/api/auth/session`

**배포 신선도 검증** (team-harness `operations.md` §6 — liveness ≠ freshness):
릴리즈 후 200만 보지 말고 최신 배포가 릴리즈 커밋과 일치하는지 CLI로 확인한다.
- 백엔드(Railway): `railway deployment list` (최신 SUCCESS commit·시각 = 방금 릴리즈)
- 프론트(Vercel): `vercel ls erp` (최신 프로덕션 배포 READY)
- DB(Neon): `neonctl branches list --project-id <id>` (브랜치 `ready`)
- 배포가 안 걸리면 ① 연결 브랜치(prod→main) ② **계정 리소스/크레딧 한도** ③ GitHub 웹훅 순으로 점검

## 팀 표준 문서

상세 표준의 단일 출처: `github.com/grinvi04/team-harness/docs`

| 영역 | 문서 | 핵심 |
|---|---|---|
| API | api-standards.md | 공통 Envelope, 에러코드 체계, offset 페이지네이션 |
| DB | db-standards.md | BIGINT PK+채번, 공통 감사 컬럼, forward-only 마이그레이션 |
| 인증·인가 | auth-standards.md | Keycloak OIDC, RBAC 권한코드+데이터 스코프 |
| 코드 구조 | clean-architecture.md | 도메인 모듈 1차 경계, 모듈 간 이벤트로만 통신 |
| 리뷰·커밋 | code-review.md | Conventional Commits(타입 영어+본문 한국어), PR 규칙 |
| AI 협업 | ai-collaboration.md | 책임 원칙, 금지사항, 기록 위치(스펙→docs/specs, 백로그→Issues/Milestone, 작업로그→git/CHANGELOG, 결정→docs/decisions.md, 로컬 메모리=개인 습관만) |
| 운영·로깅 | operations.md | 로그 레벨 기준(ERROR=알람), traceId 전파 |

## 코딩 컨벤션

- 가정하지 말 것 — 불확실하면 묻는다
- 문제를 풀 수 있는 최소한의 코드 — 요청하지 않은 기능·추상화 금지
- 외과적 수정 — 꼭 필요한 것만 건드린다
- 기존 코드 스타일에 맞춘다

## 금지 사항 (모든 도구 공통)

- `.env`·시크릿을 코드/로그/외부로 노출 금지
- `git reset --hard`, 핵심 디렉터리 `rm -rf` — 사용자가 직접 실행
- 글로벌 패키지 설치 금지 (`npm install -g` 등) — 로컬 설치(`--save-dev`·`npx`) 사용
- 운영(prod) 환경 직접 조작 금지
- main/develop 직접 커밋·push 금지 (branch protection 강제)
