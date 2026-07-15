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
