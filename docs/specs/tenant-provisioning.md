# tenant-provisioning 스펙

## 1. 목표 & Why

유료 고객사를 안전하게 생성하고 고객사별 사용자가 자신의 `tenant_id` 클레임만 받아, 최초 로그인부터 데이터가 다른 고객사와 완전히 격리되게 한다. 현재는 테넌트 마스터 생성 기능이 없고 Keycloak 클라이언트가 모든 사용자에게 `tenant_id=1`을 하드코딩하므로 두 번째 고객사를 온보딩할 수 없다.

**성공 기준:** 서로 다른 두 고객사와 사용자를 온보딩했을 때 각 토큰의 `tenant_id`가 해당 DB 테넌트와 일치하고, 상대 고객사의 API·집계·내보내기 데이터가 0건 노출된다.

## 2. Scope

- **In:** 테넌트 생성·상태 관리, Keycloak 사용자별 tenant claim 설정, 최초 테넌트 관리자 배정, 중복/부분실패 처리, 감사 기록, 2테넌트 격리 E2E, 운영자 온보딩 절차.
- **Out (Non-goals):** 공개 셀프서비스 가입, 구독 결제, 요금제 사용량 제한, 고객 도메인 자동 인증, 한 사용자의 복수 테넌트 전환, 테넌트 간 데이터 이전.

## 3. 기능 요구사항 + 수용기준

- **AC-1 (정상):** WHEN 권한 있는 운영자가 고유 고객사 코드·이름과 최초 관리자 신원을 제공하면, the system SHALL 테넌트와 관리자 권한을 한 번의 온보딩 결과로 생성한다.
- **AC-2 (토큰):** WHEN 온보딩된 사용자가 로그인하면, the system SHALL DB 테넌트 ID와 동일한 `tenant_id`를 숫자 클레임으로 발급한다.
- **AC-3 (격리):** GIVEN 테넌트 A·B에 동일 종류 데이터가 존재할 때, WHEN 각 사용자가 목록·단건·집계·CSV를 요청하면, the system SHALL 자기 테넌트 데이터만 반환한다.
- **AC-4 (중복):** IF 고객사 코드 또는 관리자 신원이 기존 테넌트와 충돌하면, the system SHALL 기존 연결을 변경하지 않고 명확한 충돌 결과를 반환한다.
- **AC-5 (부분실패):** IF DB 생성 후 Keycloak 반영이 실패하면, the system SHALL 재시도 가능한 실패 상태를 기록하고 활성 고객사로 로그인 가능하게 만들지 않는다.
- **AC-6 (중지):** WHEN 고객사가 정지되면, the system SHALL 새 API 접근을 거부하되 데이터와 감사 이력을 삭제하지 않는다.
- **AC-7 (감사):** WHEN 테넌트 생성·활성화·정지·관리자 변경이 발생하면, the system SHALL 수행자·대상·시각·결과를 감사 가능하게 기록한다.

## 4. 제약 / 비기능

- 테넌트 간 데이터 노출은 허용치 0건이다. 온보딩은 멱등 재시도가 가능해야 하며 시크릿·초기 비밀번호를 로그나 DB 평문으로 남기지 않는다.
- 단일 Keycloak realm과 기존 `TenantContext`·Hibernate `@TenantId` 구조를 유지한다.

## 5. 경계 / Do-Not

- ✅ 해도 됨: 테넌트 마스터 도메인·관리 포트, Keycloak Admin API 어댑터, 사용자별 claim 매핑, 온보딩 상태·감사 추가.
- ⚠️ 먼저 물어봐: 공개 가입, 플랫폼 운영자 UI, 복수 테넌트 사용자, 고객사 삭제·데이터 파기.
- 🚫 절대 금지: 클라이언트 전체 hardcoded tenant claim 유지, 요청 파라미터로 tenant_id 신뢰, cross-tenant 조회, Keycloak 관리자 시크릿 커밋.

## 6. Open Questions

- 없음.

### 승인된 제품 결정 (2026-07-13)

- 첫 유료 파일럿은 운영자 보조 온보딩으로 시작하며 공개 셀프서비스 가입은 제외한다.
- Keycloak 사용자는 운영자가 생성하고, ERP 온보딩 흐름은 기존 사용자에게 `tenant_id` 속성과 최초 관리자 권한을 연결한다.
- 플랫폼 관리는 앱 내부 cross-tenant UI를 만들지 않고 감사 가능한 운영 도구와 런북으로 제한한다.

## 7. 기술 접근 (HOW)

- `common.tenant`를 도메인 모델로 승격하고 `PROVISIONING/ACTIVE/SUSPENDED/FAILED` 상태 전이를 둔다. 앱 내부 cross-tenant UI 없이 일반 테넌트 요청 경로와 분리된 운영 도구만 생성·상태 변경을 허용한다.
- DB가 생성한 tenant ID를 Keycloak 사용자 속성 `tenant_id`에 기록하고, 기존 hardcoded mapper를 user-attribute mapper로 교체한다. JWT 필터는 숫자 클레임과 활성 테넌트 존재를 함께 검증한다.
- 외부 Keycloak 변경과 DB를 단일 트랜잭션처럼 가장하지 않는다. `PROVISIONING` 저장 → Keycloak 반영 → 관리자 역할 생성 → `ACTIVE` 순서와 멱등 재시도로 보상한다.
- 일반 모듈은 변경하지 않고 기존 `TenantContext`·`@TenantId`를 소비한다. 격리 검증은 두 개의 실제 JWT와 별도 데이터로 API·집계·CSV를 반증한다.

## 8. 태스크 (test-first 순서)

| # | 태스크 | AC 참조 | 대상 파일 | 검증 | 의존 | [P] |
|---|---|---|---|---|---|---|
| 1 | 테넌트 상태·고유성 도메인과 저장소 계약 | AC-1,4,6 | common tenant domain/repository, migration | backend focused test | — | |
| 2 | Keycloak tenant 속성 포트와 실패/재시도 계약 | AC-2,5 | common provisioning port/adapter | mock HTTP + service test | #1 | |
| 3 | 최초 관리자 역할과 활성화 오케스트레이션 | AC-1,5,7 | provisioning service, IAM, audit | integration test | #1,#2 | |
| 4 | JWT 활성 테넌트 검증과 정지 차단 | AC-2,6 | security filter/resolver | security integration test | #1 | [P] |
| 5 | hardcoded mapper 제거·운영 온보딩 도구/런북 | AC-1,2 | scripts, deployment docs | local Keycloak E2E | #2,#3 | |
| 6 | 두 실제 테넌트의 API·집계·CSV 격리 E2E | AC-3 | backend/frontend E2E | backend check + Playwright backend project | #3~5 | |

각 태스크는 원자적 커밋으로 남기며, DB 마이그레이션은 forward-only로 적용한다.
