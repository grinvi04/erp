# tenant-user-onboarding 스펙

## 1. 목표 & Why

테넌트 사용자의 초대·재초대·비활성화와 비-HR 역할 배정을 ERP 안의 통제된 흐름으로 제공한다. 고객 위임 관리자가 일상적인 사용자 관리를 수행하되 운영자·HR·IAM 통제 권한이나 자기 권한을 상승시킬 수 없게 한다. **성공 기준(측정 가능): 정상 사용자 온보딩에 Keycloak 관리 콘솔 작업이 0회이고, 교차 테넌트·기존 사용자 재사용·자기 권한상승·HR/IAM 권한상승 거부 테스트가 모두 통과한다.**

## 2. Scope

- **In:**
  - 이메일 기반 사용자 초대, 동일 초대의 안전한 재시도·재초대, 사용자 비활성화
  - 신규 사용자의 현재 테넌트 불변 바인딩과 초기 역할 배정
  - Finance·Inventory·CRM 및 감사 조회용 기본 역할 템플릿
  - 고객 위임 관리자를 위한 제한된 역할 생성·수정·배정·접근 프로파일 관리
  - 변경 전후 값·수행자·테넌트·traceId 감사와 외부 연동 실패 보상/재시도
  - 실제 로컬 Keycloak을 사용하는 정상·거부 경로 통합 검증
- **Out (Non-goals):**
  - HR/급여/인사 데이터 또는 `hr:*` 권한 활성화
  - 고객이 임의 권한 코드를 정의하는 기능
  - Keycloak 비밀번호·MFA·잠금 정책 자체 구현
  - 다른 테넌트로 사용자 이동, 하나의 Keycloak 사용자를 여러 테넌트에서 공유
  - 운영(prod) 계정·SMTP·플랫폼 직접 조작
  - SCIM·외부 IdP 프로비저닝과 대량 사용자 가져오기

## 3. 기능 요구사항 + 수용기준 (= 테스트 계약)

- **AC-1 (초대 정상):** GIVEN 현재 테넌트의 `iam:delegate` 또는 `iam:write` 권한자와 허용된 초기 역할이 있을 때, WHEN 유효한 새 이메일로 사용자를 초대하면, the system SHALL Keycloak 사용자를 활성 상태·현재 `tenant_id`·비밀번호 설정 필수 동작으로 생성하고 로컬 사용자 상태, 초기 역할, 감사 기록을 저장한다.
- **AC-2 (초대 멱등):** GIVEN 같은 테넌트에서 같은 요청 키로 완료되거나 부분 실패한 초대가 있을 때, WHEN 요청을 재시도하면, the system SHALL 중복 Keycloak 사용자를 만들지 않고 동일 결과를 반환하거나 실패 지점부터 안전하게 재개한다.
- **AC-3 (기존 사용자 재사용 거부):** IF 같은 이메일 또는 사용자 ID가 로컬 초대 기록 없이 Keycloak에 이미 존재하거나 다른 테넌트에 바인딩돼 있으면, THEN the system SHALL 초대를 `409 Conflict`로 거부하고 기존 사용자 속성·역할을 변경하지 않는다.
- **AC-4 (테넌트 불변):** WHILE 사용자가 한 테넌트에 바인딩돼 있을 때, the system SHALL API 입력으로 `tenant_id`를 받지 않고 인증된 테넌트만 사용하며 고객 요청으로 바인딩을 변경할 수 없게 한다.
- **AC-5 (위임 카탈로그 경계):** GIVEN `iam:delegate`만 가진 고객 관리자일 때, WHEN 권한 카탈로그나 역할을 조회하면, the system SHALL `SUPER_ADMIN`, `hr:*`, `iam:write`를 포함한 보호 역할·권한을 응답에서 제외한다.
- **AC-6 (위임 쓰기 경계):** GIVEN `iam:delegate`만 가진 고객 관리자일 때, WHEN 역할 생성·수정·삭제·배정 또는 접근 프로파일 변경을 요청하면, the system SHALL 허용된 비-HR 권한만 처리하고 보호 권한이 하나라도 포함되거나 보호 역할을 대상으로 하면 `403/C005`로 거부한다.
- **AC-7 (자기 권한상승 거부):** GIVEN `iam:delegate`만 가진 고객 관리자일 때, WHEN 자기 subject의 역할 배정·해제 또는 자기에게 배정된 역할의 수정·삭제를 시도하면, the system SHALL `403/C005`로 거부하고 상태를 변경하지 않는다.
- **AC-8 (운영자 경로 보존):** GIVEN `iam:write` 권한자일 때, WHEN 기존 IAM 관리 API를 사용하면, the system SHALL 기존 전체 권한 카탈로그와 운영자 역할 관리 기능을 유지한다.
- **AC-9 (비활성화):** GIVEN 현재 테넌트의 활성 사용자가 있을 때, WHEN 허가된 관리자가 사용자를 비활성화하면, the system SHALL Keycloak 로그인을 차단하고 ERP 역할 배정을 회수하며 감사 기록을 남긴다. 위임 관리자는 자기 자신 또는 보호 역할 보유자를 비활성화할 수 없다.
- **AC-10 (재초대):** GIVEN 현재 테넌트에서 이 제품으로 초대했다가 비활성화한 사용자가 있을 때, WHEN 재초대하면, the system SHALL 같은 Keycloak 사용자와 테넌트 바인딩을 유지하고 계정을 재활성화하며 새 초대 동작을 발행한다.
- **AC-11 (외부 실패 안전성):** IF Keycloak 생성·갱신·메일 동작 또는 로컬 저장 중 하나가 실패하면, THEN the system SHALL 권한이 열린 반쪽 상태를 남기지 않고 보상 가능한 변경을 되돌리거나 `FAILED` 상태로 기록해 동일 요청 키로 재시도할 수 있게 한다.
- **AC-12 (감사):** WHEN 초대·재초대·비활성화·역할/접근 프로파일 변경이 성공하거나 보안 경계에서 거부되면, the system SHALL 수행자, 대상 사용자, 테넌트, 변경 전후 또는 거부 사유, traceId를 시크릿 없이 기록한다.
- **AC-13 (실 Keycloak 검증):** GIVEN 로컬 PostgreSQL·Keycloak·백엔드·프런트 실스택일 때, WHEN 상용 UAT 셋업을 실행하면, the system SHALL 신규 초대·재초대·비활성화와 교차 테넌트·기존 사용자 재사용 거부를 실제 Keycloak 상태로 검증한다.

## 4. 제약 / 비기능

- 사용자 이메일은 소문자 정규화 후 테넌트 내 유일해야 하며 로그·감사에는 비밀번호·토큰·클라이언트 시크릿을 기록하지 않는다.
- Keycloak 관리 자격증명은 `erp-user-admin` 전용 서비스 계정으로 분리하고 플랫폼 시크릿에만 저장한다. realm 관리·클라이언트 관리 권한은 부여하지 않는다.
- 서버 검사가 최종 경계다. 프런트의 메뉴·버튼 숨김은 보조 수단이며 API 거부를 대체하지 않는다.
- 초대 요청은 클라이언트가 생성한 요청 키를 필수로 받아 네트워크 재시도의 중복 생성을 방지한다.

## 5. 경계 / Do-Not

- ✅ 해도 됨: `common` IAM·테넌트 프로비저닝, 전용 forward-only Flyway, IAM 화면, 로컬 Keycloak 셋업·상용 UAT를 외과적으로 확장
- ⚠️ 먼저 물어봐: Keycloak 이미지/메일 사업자 변경, 유료 외부 서비스 추가, 고객에게 `iam:write` 부여, 운영 플랫폼 변수 변경
- 🚫 절대 금지: 기존 사용자의 테넌트 강제 덮어쓰기, HR 권한 노출, 시크릿 커밋, main/develop 직접 커밋·push, 운영 환경 직접 조작

## 6. Open Questions

없음. 첫 유료 파일럿의 승인 범위(국내 소규모 무역·유통, HR 제외)와 #197 수용기준에 따라 위 모델로 고정한다.

## 7. 기술 접근 (HOW)

- `iam:delegate`를 제한된 고객 관리자 권한으로 추가한다. 기존 `iam:write`는 운영자 전체 관리 권한으로 유지한다. 서비스는 역할명이 아니라 현재 권한과 대상 역할의 권한 집합으로 경계를 판단한다.
- 보호 집합은 `hr:*`, `iam:write`, `iam:delegate`의 임의 재부여와 `SUPER_ADMIN` 역할이다. `iam:delegate` 자체는 테넌트 프로비저닝이 만드는 고객 관리자 템플릿에만 포함하며 위임 API로 새로 부여할 수 없다.
- 최초 테넌트 프로비저닝에서 보호된 `SUPER_ADMIN`과 별도로 Finance·Inventory·CRM·감사 조회·제한 IAM 관리용 기본 역할 템플릿을 멱등 생성한다. 코드에서 역할명으로 인가하지 않고 권한 집합만 검사한다.
- `TenantUser` 로컬 레코드에 정규화 이메일, Keycloak user id, 상태(`PENDING`/`ACTIVE`/`FAILED`/`DISABLED`), 요청 키, 실패 코드와 감사 컬럼을 보관한다. `(tenant_id, normalized_email)`과 `(tenant_id, request_key)`를 유일하게 한다.
- `TenantUserOnboardingService`는 인증된 테넌트와 현재 subject만 사용한다. `TenantIdentityAdminPort`를 통해 Keycloak 생성·조회·활성화·비활성화·초대 동작을 호출하고, 새로 만든 외부 사용자 뒤 로컬 저장이 실패하면 그 요청에서 만든 사용자만 보상 삭제한다. 기존 사용자는 절대 삭제하지 않는다.
- Keycloak 어댑터는 Admin REST API의 `POST /admin/realms/{realm}/users`, 사용자 조회/갱신, `execute-actions-email`을 사용한다. 서비스 계정은 `manage-users`, `view-users`, `query-users`만 가지며 애플리케이션이 `tenant_id` 불변과 기존 사용자 재사용 금지를 추가로 강제한다.
- 초대 메일은 Keycloak의 `VERIFY_EMAIL`, `UPDATE_PASSWORD` required actions를 사용한다. 비밀번호·MFA 로직은 ERP에 구현하지 않는다.
- IAM 조회/쓰기 메서드는 `iam:write`면 기존 동작, `iam:delegate`면 필터·대상 검증 동작을 수행한다. 프런트는 서버가 반환한 필터 결과만 표시하고, 고객 관리자는 이메일 기반 사용자 목록/초대/비활성화 화면을 사용한다.
- 감사 성공 이벤트는 기존 `AuditService`로 기록하고 보안 경계 거부는 구조화 WARN과 traceId를 남긴다. 트랜잭션 롤백 때문에 사라지는 거부 감사 DB 기록을 성공 감사와 같은 방식으로 가장하지 않는다.

### 영향 파일/모듈

- Backend: `common/security`, `common/tenant/provisioning`, Keycloak 어댑터·설정, `db/migration`의 common 대역
- Frontend: IAM 타입·권한 상수·서버 액션·IAM 화면
- Local/UAT: `scripts/keycloak-setup.sh`, 상용 UAT setup/spec, 배포·온보딩 문서

### 테스트 전략

- AC-5~8: `IamServiceTest`와 IAM 통합 테스트에서 운영자/위임자 권한별 응답·거부·무변경 검증
- AC-1~4,9~12: 온보딩 서비스 단위 테스트에서 외부 포트와 DB 상태, 멱등성, 보상, 감사 계약 검증
- AC-1~4,9~11: Keycloak 어댑터 HTTP 계약 테스트에서 요청 본문·기존 사용자 충돌·응답 처리 검증
- AC-1~12: PostgreSQL 통합 테스트에서 유일성·테넌트 격리·역할 회수·감사 검증
- AC-5~10: 프런트 로직 테스트와 Playwright에서 필터된 역할, 자기 관리 잠금, 초대·비활성화 동작 검증
- AC-13: 로컬 상용 UAT에서 실제 Keycloak 사용자 속성·활성 상태·중복 수를 API로 readback

## 8. 태스크 (test-first 순서)

| # | 태스크 | AC 참조 | 대상 파일 | 검증(이 명령 exit 0) | 의존 | [P] |
|---|---|---|---|---|---|---|
| 1 | 위임 권한과 서버측 역할·자기관리 경계를 RED→GREEN으로 추가 | AC-5~8,12 | `backend/src/main/java/com/erp/common/security`, 대응 테스트, `frontend/src/lib/permissions.ts` | `cd backend && ./gradlew test --tests '*Iam*'` 및 `cd frontend && npm test` | — | |
| 2 | 테넌트 사용자 상태·유일성·기본 비-HR 역할 템플릿을 추가 | AC-2,4,8,11 | common Flyway, `common/security`, provisioning, 통합 테스트 | `cd backend && ./gradlew test --tests '*TenantUser*' --tests '*Provisioning*'` | #1 | |
| 3 | Keycloak 사용자 관리 포트·어댑터와 초대/재초대/비활성화 유스케이스를 추가 | AC-1~4,9~12 | backend Keycloak 설정·adapter·service·controller·tests | `cd backend && ./gradlew test --tests '*UserOnboarding*' --tests '*Keycloak*'` | #2 | |
| 4 | 이메일 기반 사용자 관리 UI를 추가하고 위임자에게 안전한 동작만 노출 | AC-1,5~10 | frontend IAM page/client/actions/types/tests | `cd frontend && npm test && npm run type-check && npm run lint && npm run build` | #3 | |
| 5 | 로컬 Keycloak 실스택 UAT와 운영 문서를 갱신 | AC-3,4,9~13 | `scripts/keycloak-setup.sh`, commercial UAT, deployment/onboarding docs | 상용 UAT dry-run 후 로컬 실스택 `./scripts/commercial-uat.sh --all` | #3,#4 | |
| 6 | 전체 회귀·반증 검증 후 한 기능 PR 준비 | 전체 | 전체 변경 | `cd backend && ./gradlew check`; frontend test/type/lint/design/build/e2e | #1~5 | |

### 롤백

- 태스크 1은 독립 커밋으로 되돌릴 수 있다. 태스크 2 이후에는 forward-only DB 마이그레이션과 후속 코드가 의존하므로 단독 revert 대신 fix-forward한다.
- 런타임 Keycloak 사용자 관리 설정은 환경변수 미설정 시 안전하게 기능을 비활성화하고 기존 운영자 보조 절차를 유지한다. 이 상태에서 고객에게 `iam:delegate` 역할을 배정하지 않는다.

## 9. 2026-10-07 Harness QA 계약 보강 (로컬 fix 브랜치)

대상은 `origin/develop`의 `d10a916`에서 만든 `/tmp/harness-consumers-20261007/erp`의 `fix/harness-qa-contract`이다. 기존 `feature/tenant-user-onboarding` 체크아웃과 미추적 `.codex/`는 보존한다. 이 변경은 AC-13의 안전한 진입과 검증 증거를 보강한다. 기존 온보딩 구현·수용기준·진행 상태를 완료로 다시 판정하지 않으며, 실 Keycloak UAT는 전용 격리 DB/identity에서만 실행한다.

| 요구·위험 / 선정 이유 | 조건·행동 / 환경 | 기대 결과·관찰 경계 | 필수 | 증거·판정 |
|---|---|---|---|---|
| AC-13 / URL override가 원격 인증·데이터 변경으로 이어질 수 있음 | backend·Keycloak·Mailpit 중 하나에 외부 URL, URL 자격증명, 경로·query 등 잘못된 origin 입력. 합성 `.env.local`과 curl stub | 첫 인증·네트워크 호출 이전 종료 코드 2. curl 호출 0, 자격증명 전송 0, 변경 요청 0 | 필수 | `bash scripts/verify-user-onboarding-test.sh` |
| AC-13 / 로컬 설정 파일이 사전 검사 뒤 URL을 재정의할 수 있음 | 합성 `.env.local`에 외부 `BACKEND_URL` | source 후 재검사에서 종료 코드 2, curl 호출 0 | 필수 | 같은 stub 검사 |
| AC-13 / 허용 경로 유지 | backend `127.0.0.1:18180`, Keycloak `[::1]:18180` | URL 검사를 통과해 첫 헬스체크까지 도달. stub의 의도된 종료 코드 69, 호출 1 | 필수 | 같은 stub 검사. 실제 서비스 동작 증거는 아님 |
| AC-1~13 / 제품 유지 | CI와 같은 Java 21 백엔드 `./gradlew check`, 프런트 타입·포맷·린트·디자인·단위·빌드·Chromium e2e | 각 명령 exit 0. DB가 필요한 검사는 전용 격리 DB에서 수행 | 필수 | 아래 실행 결과에 후보·환경·명령별 기록 |
| AC-13 / 실 Keycloak 상태 | 전용 격리 PostgreSQL·Keycloak·Mailpit·백엔드, 합성 사용자 | 초대·재초대·비활성화·교차 테넌트 거부의 실제 저장/identity readback | 필수 | 격리 환경을 준비한 경우에만 실행, 미실행은 UNVERIFIED |

문서·CI 범위: 기존 `commitlint.yml` 검사와 validator를 보존하고 신뢰된 기본 브랜치 코드로 PR metadata를 검사하는 `commitlint-trusted.yml`을 추가한다. `repo-sync`는 v0.67.0 pin에서 v0.81.0의 공식 태그 SHA로 올리되 검사기 변경 여부를 비교한다. 이 둘은 로컬 구문·구성 확인과 원격 CI 실행을 구분한다. PR·머지·배포 및 GitHub 정책 변경은 이 로컬 작업에 포함되지 않는다.

### 실행 증거와 상태

후보는 `d10a916` 위의 이 로컬 diff, 호스트 Java 21.0.11·Node 22.18.0·Docker Compose 2.39.2다. 아래 명령은 별도 언급이 없으면 `/tmp/harness-consumers-20261007/erp`에서 실행했다. 인증정보·토큰은 결과에 남기지 않았고, 실스택 출력 원본은 제한된 권한의 `/tmp/harness-consumers-20261007/erp-uat-*.log`에만 캡처했다.

| 주장 / 관찰 경계 | 실제 명령·환경 | 최초 결과 → 최종 결과 / 판정 |
|---|---|---|
| UAT 진입 거부와 허용 | `bash scripts/verify-user-onboarding-test.sh`; 합성 `.env.local`, curl stub | 수정 전 remote backend 사례 exit 1: 기대 exit 2/curl 0, 실제 exit 69/curl 1 (**RED**). 수정 후 외부 backend·Keycloak·Mailpit, userinfo·host suffix·옵션 문자열·경로·query·source 재정의 모두 exit 2/curl 0; loopback 두 사례 exit 69/curl 1 (**PASS**). 실제 서비스 변경은 stub에서 일어나지 않음. |
| backend 품질 | `cd backend && SPRING_DATASOURCE_URL=jdbc:postgresql://127.0.0.1:55442/erp SPRING_PROFILES_ACTIVE=test ./gradlew check` (전용 시험 DB, 합성 DB 계정 환경변수) | exit 0, `BUILD SUCCESSFUL` (**PASS**). 실 Keycloak 인증·UAT를 대신하지 않음. |
| frontend 품질 | `cd frontend && npm ci --ignore-scripts`, `npm run type-check`, `npm run format:check`, `npm run lint`, `npm run lint:design`, `npm run test`, `npm run build`, `npx playwright install chromium`, `CI=true npm run test:e2e` | 수정된 lockfile 후보에서 각 exit 0; unit 60건, Chromium auth-gate 38건 (**PASS**). e2e 중 백엔드 미기동으로 발생한 fetch 거부 로그가 있어 업무 API e2e로 해석하지 않음. |
| 컨테이너 빌드 | `docker build --tag erp-backend:harness-qa .` (backend), `docker build --tag erp-frontend:harness-qa .` (frontend), `docker image inspect` | backend exit 0/user `10001:10001`. frontend 최초 exit 1: npm 11 증분 lockfile에 선택적 `@emnapi/*` 누락, 컨테이너 npm 10 `EUSAGE`; `npx --yes npm@10.9.8 install --package-lock-only --ignore-scripts --no-audit --no-fund` 뒤 재빌드 exit 0/user `node` (**PASS**). |
| 의존성 보안 | `cd frontend && npm audit --json`; 공식 GitHub advisory의 Next.js 수정 버전 확인 | 최초 21건(critical 2 포함). 비강제 `npm audit fix`와 Next.js·eslint-config-next `16.3.6` 갱신 뒤 9건 high/critical 0. 남은 9건은 패치 버전이 없는 `braces` 전이 경로라 **미해결**; 강제 major 교체는 하지 않음. |
| 표준·문서 구성 | `node .../team-harness/scripts/check-repo-sync.mjs --repo . --harness .../team-harness`, workflow YAML parse, `bash -n` (두 UAT 스크립트), `git diff --check` | repo-sync 21/21 OK, YAML·구문·diff exit 0 (**PASS**). v0.81.0 태그가 `9838c2ef288b4566f81fae03acb56530ee165c06`이며 v0.67.0 대비 검사기 내용 변경 없음. 원격 workflow 실행은 **UNVERIFIED**. |
| 실 Keycloak 초대·재초대·거부 | 전용 `erp-harness-qa` Compose, PostgreSQL `erp_uat`·Keycloak·Mailpit, backend `127.0.0.1:18080`; `E2E_COMMERCIAL=1 E2E_COMMERCIAL_MUTATION=LOCAL_MUTATION_ACCEPTED BACKEND_URL=http://127.0.0.1:18080 KEYCLOAK_URL=http://127.0.0.1:18180 MAILPIT_URL=http://127.0.0.1:18025 bash scripts/verify-user-onboarding.sh`를 합성 이메일로 2회 | 첫 신규 초대·두 번째 재초대 각각 exit 0. Keycloak 사용자 수 1, ID 동일, 최종 disabled를 별도 readback으로 단언 (**PASS**, 이 격리 후보에 한정). Mailpit·감사·역할 회수·교차 테넌트 409/C008은 스크립트의 단언으로 확인. |

실스택 최초 환경 실패도 보존한다. 새 Keycloak master realm은 호스트 HTTP 토큰 요청을 `403 HTTPS required`로 거부해 **그 시험 컨테이너 안에서만** SSL 요구를 변경했다. 첫 `provisionTenant`는 전용 Keycloak URL 미설정으로 `ConnectException`, 재시도는 선행 백엔드 검사 DB의 tenant ID 충돌, 새 `erp_uat` DB에서 `ERP_PROVISION_RETRY=true`는 미존재 tenant 오류였다. 새 DB의 최초 생성 모드로 바꾼 뒤 provisioning exit 0이었다. 최초 UAT 두 실행은 Compose 포트가 `0.0.0.0/[::]`에 publish된 상태에서 통과했으나 노출 경계가 틀려 최종 증거로 사용하지 않았다. 해당 새 컨테이너만 중지하고 포트를 모두 `127.0.0.1`로 재생성했다. `docker inspect`의 네 publish `HostIp=127.0.0.1`, 백엔드 listener `127.0.0.1:18080` 확인 후 새 합성 이메일로 신규 초대·재초대 두 실행과 동일 ID readback을 다시 통과했다. 기존 프로젝트 DB·identity와 원본 checkout은 변경하지 않았다.

판정: 스크립트의 URL 차단, 로컬 품질 명령 및 격리 실 Keycloak의 이 시나리오는 **PASS**. 시험 뒤 백엔드와 전용 Docker 세 컨테이너를 중지했고 전용 볼륨은 보존했다. 남은 npm high 9건과 원격 CI/PR gate는 **미해결·UNVERIFIED**이며, 로컬 구현을 병합·릴리즈·배포 완료로 표시하지 않는다.

### 독립 검토 후 `curlrc` 보완 및 새 후보 검증

위 기록은 당시 후보 `564dcced`의 상태를 보존한다. 독립 검토에서 UAT 스크립트의 curl wrapper가 `--noproxy`를 첫 인자로 전달해, 기본 `~/.curlrc`의 `connect-to` 또는 `location` 설정을 읽을 수 있음이 확인됐다. URL origin 검사를 통과한 뒤에도 인증 POST가 다른 목적지로 전송될 수 있는 AC-13 진입 경계 결함이다. wrapper의 첫 인자를 `-q`로 바꾸고 합성 `CURL_HOME`에서 실제 curl과 서로 다른 127.0.0.1 임시 수신점 두 개를 사용해 반증했다. 사용자의 HOME·curl 설정·기존 계정은 변경하지 않았다.

| 필수 범위와 기대값 | 최초 반례 → 수정 후보 관찰 | 증거 / 한계 |
|---|---|---|
| 합성 `.curlrc`의 `connect-to`가 허용 origin의 POST 본문을 다른 수신점으로 바꾸지 못해야 함 | 이전 wrapper에서 첫 실 curl 시험 exit 1, 다른 수신점 요청 4회·자격증명 본문 POST 1회 (**RED**). `curl -q --noproxy '*'` 이후 의도된 수신점 POST 1회, 다른 수신점 요청 0회 (**PASS**) | `/tmp/harness-consumers-20261007/erp-curlrc-red.log`, `/tmp/harness-consumers-20261007/erp-qa-v2/curlrc-green.log`. 두 수신점 모두 loopback이며 자격증명은 합성값이고 결과 출력에 값은 없다. |
| 기본 curl 설정의 `location`이 307/308 응답의 POST를 다른 수신점에 재전송하지 못해야 함 | 각 상태에서 의도된 수신점 POST 1회, 다른 수신점 요청 0회 (**PASS**). curl 기본 설정을 적용한 이전 wrapper의 위치 전송 가능성은 첫 `connect-to` 실패로 전체 시험이 멈췄으므로 별도 RED로 주장하지 않음 | 같은 실제 curl 시험. 서버의 리다이렉트는 합성 loopback 주소로만 구성. |
| 기존 외부·malformed 주소 거부 0요청과 loopback 허용 경로 유지 | 11개 stub 사례 전부 기존 기대 종료 코드·호출 수 유지 (**PASS**) | 같은 `bash scripts/verify-user-onboarding-test.sh` 결과. stub은 인증·서비스 실동작 증거가 아님. |
| 현재 후보의 로컬 필수 검사, 전용 실 Keycloak, 종료 경계 | Java 21 Gradle check; 프런트 npm ci, 타입·포맷·린트·디자인·단위 60건·빌드·Chromium e2e 38건; 백엔드/프런트 Docker 빌드와 비 root 사용자, repo-sync 21/21, workflow YAML·shell 구문·diff 모두 exit 0 (**PASS**). 새 합성 사용자 초대·재초대 각각 exit 0, Keycloak 직접 readback 사용자 1명·ID 동일·최종 disabled (**PASS**). 시험 전 Docker 네 publish HostIp 모두 127.0.0.1, 종료 후 세 컨테이너 stopped·백엔드 listener 없음 | 명령별 cwd·실제 종료 코드·원문 로그 경로/sha256·변경 대상 파일 지문은 `/tmp/harness-consumers-20261007/erp-qa-v2/manifest.json`에 기록. 전용 격리 DB `erp_uat`, 18180/18080/55442/18025, 전용 identity 및 메일에만 한정. 원문 로그는 제한 권한으로 보존. |
| 의존성 보안·원격 gate | `npm audit --json` exit 1: high 9, critical 0 (**미해결**). 원격 commitlint/repo-sync/CI, PR·병합·릴리즈·운영 배포는 **UNVERIFIED** | npm high는 이 후보에서 패치가 없는 `braces` 전이 경로; 로컬 검사 성공을 원격 gate 성공으로 확대하지 않음. |

첫 `curlrc` 실패와 수정 뒤의 명령·원문 로그를 구분해 보존한다. 품질 검사와 실스택 재시험은 이 보완 후보에서 다시 실행했다. 합성 자격증명 값은 로그·커밋·문서에 두지 않았으며, 원문 로그와 소스 지문을 대조한 범위에서만 PASS를 주장한다. 전용 fixture는 종료 후 DB 볼륨만 남겼다.
