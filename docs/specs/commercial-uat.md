# commercial-uat 스펙

## 1. 목표 & Why

유료 파일럿 후보를 로컬의 실제 Keycloak, PostgreSQL, Spring Boot, Next.js 조합에서 반복 검증한다. 기존 렌더 스모크를 넘어 서로 다른 사용자와 테넌트가 실제 업무를 수행했을 때 결재, 원장, 재고, 부가세, 감사 결과가 일관되는지 증거로 남긴다. **성공 기준(측정 가능): 전용 UAT 테넌트 A/B와 작성자·결재자를 사용한 전체 시나리오가 재실행 가능하고, 모든 업무 결과·격리·거부 단언이 통과한다.**

## 2. Scope

- **In:** 감사 로그의 요청 traceId 영속화·조회, 로컬 전용 UAT 사용자·테넌트 준비, 실 Keycloak 토큰 기반 HTTP 업무흐름, 테넌트 격리, AP/AR·GL·재무제표·부가세, 재고 이동·조정 결재, 핵심 화면 readback, 안전한 실행 문서.
- **Out (Non-goals):** 운영/스테이징 배포, 고객 데이터 사용, 플랫폼 백업 검증, 가격·SLA·RPO/RTO 결정, 법률 문안, 기존 업무 API 변경.

## 3. 기능 요구사항 + 수용기준 (= 테스트 계약)

- **AC-1 (안전한 진입):** WHEN 상용 UAT를 실행하면, the system SHALL 명시적 mutation opt-in, localhost 대상, 서로 다른 작성자·결재자 subject, 동일한 테넌트 A 및 별도 테넌트 B를 검증한 뒤에만 데이터를 변경한다.
- **AC-2 (설정 오류):** IF 필수 자격증명이 없거나 작성자와 결재자가 같거나 테넌트 조건이 맞지 않으면, THEN the system SHALL 업무 데이터를 만들기 전에 명확히 실패하고 토큰·비밀번호를 출력하지 않는다.
- **AC-3 (테넌트 격리):** WHEN 테넌트 A가 만든 고유 업무 데이터를 테넌트 B가 동일 ID·검색 조건으로 조회하면, the system SHALL 성공 응답에 해당 데이터를 포함하지 않는다.
- **AC-4 (재무 정상 흐름):** WHEN 작성자가 AP/AR를 생성·상신하고 다른 결재자가 승인·지급/수금 및 생성된 GL을 상신·전기하면, the system SHALL 상태와 잔액을 갱신하고 시산표·손익계산서·재무상태표 및 부가세 집계에 균형 잡힌 결과를 반영한다.
- **AC-5 (직무분리 경계):** WHEN 작성자가 자신이 상신한 AP/AR/GL 또는 조정 이동을 직접 승인하려 하면, the system SHALL 권한 보유 여부와 관계없이 거부하고 상태·금액·수량을 변경하지 않는다.
- **AC-6 (재고 정상 흐름):** WHEN 입고, 창고 간 이전, 출고를 확정하고 조정 이동을 작성자 상신·타 사용자 승인하면, the system SHALL 각 위치의 전후 수량과 이동 상태를 정확히 반영한다.
- **AC-7 (내보내기·감사):** WHEN UAT 고유 필터로 부가세/CSV 및 감사 로그를 조회하면, the system SHALL 원거래와 일치하는 결과만 반환하고 감사 상세에 수행자·대상·변경 전후·traceId를 제공한다.
- **AC-8 (브라우저 readback):** WHEN API 업무흐름이 끝나면, the system SHALL 실제 Next.js 세션으로 재무·재고·결재·감사 화면에서 생성된 결과를 오류 없이 조회하고 브라우저 `pageerror`를 0건으로 유지한다.
- **AC-9 (재실행):** WHILE 같은 로컬 UAT 환경을 반복 사용하더라도, the system SHALL 실행별 고유 식별자로 데이터를 구분해 이전 실행과 충돌하지 않고 결과 요약에 시크릿 없는 식별자·건수만 기록한다.

## 4. 제약 / 비기능

- 실제 업무 mutation은 기본 비활성화하며 localhost와 명시적 opt-in에서만 허용한다. 원격 실행은 이 스펙 범위에서 금지한다.
- 테스트 자격증명·JWT·DB URL은 환경변수에서만 읽고 테스트 출력, trace, screenshot, GitHub artifact에 남기지 않는다.
- 회계·감사 레코드는 삭제해 정리하지 않는다. 전용 UAT 테넌트와 실행 식별자로 격리한다.

## 5. 경계 / Do-Not

- ✅ 해도 됨: 로컬 Keycloak에 전용 테스트 사용자 생성, 로컬 DB에 전용 UAT 테넌트 프로비저닝, 기존 공개 API를 통한 테스트 데이터 생성, 마스킹된 결과 기록.
- ⚠️ 먼저 물어봐: localhost가 아닌 환경 실행, 고객 데이터 사용, 외부 계정·비용·도메인 생성, 기존 API 동작 변경.
- 🚫 절대 금지: 운영 환경 조작, 시크릿·토큰 출력/커밋, 임의 SQL로 업무 결과 조작, 작성자 본인 승인 허용, 테스트 통과를 위한 인가·회계 불변식 완화.

## 6. Open Questions

- 없음. 서비스 정책·법률·외부 배포 결정은 GitHub #187~#190에서 별도로 승인한다.

## 7. 기술 접근 (HOW)

- 기존 `E2E_BACKEND` Playwright 프로젝트와 실제 Keycloak password grant 패턴을 재사용하되, mutation 시나리오는 별도 `E2E_COMMERCIAL` 게이트로 완전히 분리한다.
- 로컬 준비 스크립트는 Keycloak Admin API와 기존 `provisionTenant`/IAM API만 사용해 UAT-A 작성자·결재자와 UAT-B 관리자를 멱등하게 준비한다. UAT-A 두 사용자 모두 `SUPER_ADMIN` 역할과 테스트 금액 이상의 전결 한도를 가진 접근 프로파일을 받되, 서로 다른 `sub`를 유지한다. 기존 tenant 1이나 임의 SQL에 의존하지 않는다.
- 현재 감사 로그 스키마·응답에는 traceId가 없으므로 common 대역의 forward-only migration으로 nullable `trace_id`를 추가하고, 웹 요청에서는 `TraceIdFilter`가 설정한 MDC 값을 `AuditService`가 함께 저장한다. 웹 요청 밖의 프로비저닝·배치 기록은 null을 허용해 기존 경로를 깨지 않는다.
- Playwright의 설치된 `APIRequestContext`로 각 bearer token을 분리하고 모든 응답 envelope·HTTP 상태·업무 상태를 단언한다. API 완료 후 기존 Auth.js 세션 생성 패턴으로 브라우저 readback을 수행한다.
- 각 실행은 충돌하지 않는 UAT run ID를 사용한다. 전후 수량과 보고서 합계는 테스트가 생성한 금액·수량의 delta로 비교해 기존 UAT 데이터와 독립적으로 검증한다.
- AP/AR 승인 시 생성되는 GL의 작성자는 승인 사용자이므로, 인보이스는 작성자→결재자 순서로 승인하고 생성된 GL은 반대 사용자가 승인한다. 이 교차 순서로 모든 전기에서 작성자≠결재자 불변식을 실제로 검증한다.
- 업무 API의 동작은 변경하지 않는다. 제품 변경은 감사 로그 `traceId`의 저장·상세 조회와 이를 위한 forward-only migration에 한정한다.

### 테스트 전략

- AC-1,2,9: 환경 계약·토큰 claim·run ID helper의 Vitest 정상/예외/경계 테스트.
- AC-7: AuditService·repository 통합 테스트에서 MDC traceId 저장/조회와 기존 null 경로를 검증하고, 상용 UAT에서 요청 `X-Trace-Id`와 감사 상세 값을 대조한다.
- AC-3~7: `E2E_COMMERCIAL=1`에서만 포함되는 실제 Keycloak·Backend Playwright API 시나리오.
- AC-8: 같은 프로젝트의 실제 Next.js 브라우저 readback 및 `pageerror` 단언.
- 전체 회귀: backend `check`, frontend unit/type/lint/design/build, 기본 E2E는 기존과 동일하게 필수이며 상용 UAT는 명시적 로컬 명령으로 별도 실행한다.

## 8. 태스크 (test-first 순서)

| # | 태스크 | AC 참조 | 대상 파일 | 검증(이 명령 exit 0) | 의존 | [P] |
|---|---|---|---|---|---|---|
| 1 | 감사 로그 traceId 저장·상세 조회 계약을 RED로 만들고 nullable forward-only migration으로 구현 | AC-7 | `backend/src/main/`, `backend/src/test/`, `db/migration/V0011*` | `cd backend && ./gradlew check` | — | |
| 2 | 상용 UAT 환경 계약과 로컬 전용 안전 가드·run ID helper를 테스트 우선으로 추가 | AC-1,2,9 | `frontend/src/lib/commercial-uat.*`, `frontend/playwright.config.ts` | `cd frontend && npm test && npm run type-check` | — | [P] |
| 3 | UAT-A 작성자·결재자와 UAT-B 관리자를 멱등 준비하는 로컬 setup을 추가 | AC-1~3 | `scripts/`, `docs/` | setup dry-run/validation + `cd backend && ./gradlew check` | #2 | |
| 4 | 테넌트 격리와 AP/AR·GL·재무제표·부가세·직무분리 API 시나리오 추가 | AC-3~5,7 | `frontend/e2e/commercial-workflow.spec.ts`, helpers | `cd frontend && E2E_COMMERCIAL=1 npm run test:e2e -- --project=commercial` | #1,#3 | |
| 5 | 입고·이전·출고·조정 결재 및 화면 readback·감사 시나리오와 실행 문서 추가 | AC-5~9 | `frontend/e2e/`, `README.md`, `docs/release-readiness.md` | 상용 UAT + frontend 전체 품질 + `cd backend && ./gradlew check` | #4 | |

태스크별 커밋을 롤백 단위로 유지한다. #1의 migration과 #3 이후 태스크는 의존 관계 때문에 단독 롤백 대신 fix-forward한다.
