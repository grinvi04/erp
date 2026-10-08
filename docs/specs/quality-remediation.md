# quality-remediation 스펙 — 상용 제품 품질 리메디에이션 로드맵

> 0~6절은 2026-06 감사와 당시 리메디에이션 계획이다. 현재 QA 계약의 develop 병합 결과와 남은 출시 경계는 7절을 따른다. 상용 파일럿의 수용 기준은 [commercial-readiness](commercial-readiness.md)에 있다.

## 0. Context / Why

erp는 수백 기업(테넌트)이 돈 주고 쓰는 **상용 SaaS ERP**다. 2026-06-28 실 스택(도커 재기동 → 백엔드 클린 부트 → 실 Keycloak 인증)으로 **실 CRUD·전 화면 상호작용·용어를 직접 감사**한 결과, 렌더링 스모크 테스트로는 안 보이던 **"돈 주고 살 수 없는" 결함**이 다수 발견됐다.

- 감사 방식: 실 API curl·DB 조회·코드 정독 + **Playwright로 35개 화면 전부 실 세션 구동**(다이얼로그·워크플로·필터·드릴다운 상호작용). 추측/실측 구분.
- **P0 데이터 정합성 3건은 실패(RED) 통합테스트로 박제**됨(브랜치 `fix/p0-data-integrity`, src/main 무수정).
- 핵심 회계 엔진부(재무제표 계산식·차대·계정유형·낙관적잠금 동작 자체)는 **정확**하다 — 리메디에이션은 그 주변(미연결 워크플로·크래시·필터·식별자·용어)에 집중한다.
- ⚠️ 일부 항목은 **버그 수정이 아니라 미구현 기능**(회계기간 관리 UI·전표 역분개·리드 전환 로직)이라 실제 구현 규모가 있다.

**성공 기준:** Tier 0(출시 차단)·Tier 1(핵심 기능) 결함이 모두 제거되어, 신규 테넌트가 **회계기간 생성 → 전표 입력 → 결재 → 재무제표**, **인보이스 등록 → 결재 → 지급**, **재고 입출고·이전**, **리드 → 기회 전환**의 핵심 업무 흐름을 **끝까지 완수**할 수 있고, 삭제·금액·식별이 정확하다.

---

## 1. 결함 인벤토리 (전부 실측 근거 — 심각도 티어순)

### 🔴 Tier 0 — 출시 차단 (제품이 실제로 작동하지 않음)

| ID | 결함 | 실측 근거 | 비고 |
|---|---|---|---|
| **T0-1** | **invoices·ar-invoices 페이지 풀 크래시** — `vendors.filter`/`customers.filter is not a function`. 페이지네이션 응답을 `apiGet<X[]>` 배열로 캐스팅 | Playwright: 두 화면 로드 즉시 error.tsx. DB엔 AP 인보이스 **10건 존재하나 전부 비가시**, AP/AR 결재·지급·수금 워크플로 전체 접근 불가 | 출처 `45a9b3f`(원 구현), 마이그레이션 무관. 패턴은 이 2곳 한정(타 화면 음성확인) |
| **T0-2** | **회계기간(FiscalYear/Period) 생성 UI 전무** → 전표 입력·재무제표가 신규 테넌트에서 **영구 불가** | `fiscal_year=0`·`fiscal_period=0`. 분개장 "새 분개" 버튼 안 뜸("회계 기간을 선택" 고정). 재무제표 TB API `F008 회계연도 없음`. 백엔드 `FiscalYearController`는 존재하나 프론트 관리 화면 라우트 없음 | **미구현 기능**(소비만 함) |
| **T0-3** | **결재 워크플로 완결 불가** — 결재자가 실사용자로 해소 안 됨 | 전 직원 `manager_id·user_id=NULL` → 결재자 리터럴 `"SYSTEM"`(`approval_step.approver_id` 전건). `requireAuthorizedApprover`는 로그인 sub==결재자를 요구 → 누구도 불일치. `GET /approvals/pending`=`[]`, 대기 4건 처리 불가. 연차 결재도 동일 | 결재함·연차결재 **데드엔드**. 매니저 지정 UI 부재 + SYSTEM fallback이 근본 |
| **T0-4** | **소프트삭제 전역 무효** — 삭제 레코드가 목록·집계·단건조회·리포트에 그대로 | `@SQLRestriction("deleted_at IS NULL")`이 `@MappedSuperclass`(BaseEntity)에 있어 Hibernate가 하위 38개 @Entity에 **상속 안 함**. HR·CRM 모듈 모두 실측 재현(삭제 후 목록·analytics에 잔존) | **RED**: `P0SoftDeleteFilterIntegrationTest` |
| **T0-5** | **FX 조회 500 → 다통화 환산 합계가 외화 누락** | `GET /api/finance/fx` → **HTTP 500**(C999), 환율 0 → 환산 불가 시 외화를 **조용히 합산서 제외**. 대시보드 미지급합계·파이프라인 금액이 KRW분만(USD 누락) — "기준통화 환산 합계"라 표기하면서 미환산 | 재무 수치 **과소 표시**(의사결정 왜곡) |

### 🟠 Tier 1 — 핵심 기능 결함 (High)

| ID | 결함 | 실측 근거 |
|---|---|---|
| T1-1 | **UPDATE 응답 stale version** (18개 update 전부) → 연속 수정 시 거짓 409 | **RED** `P0VersionStaleResponseIntegrationTest`. 응답 version=0인데 DB=1 |
| T1-2 | **클라이언트 입력오류 → 500**(400이어야) | **RED** `P0InvalidInputStatusIntegrationTest`. 잘못된 enum·깨진 JSON·경로타입불일치. 다수 모듈 API에서 재현(contacts·leave-balances 등) |
| T1-3 | **전표 역분개(reversal) 미구현** — POSTED가 종착, 정정 수단 전무 | `markReversed()` 정의만·호출 0, 컨트롤러 엔드포인트·버튼 없음 |
| T1-4 | **전표 반려(reject) UI 미연결** — 승인권자가 승인만 가능 | `POST /journal-entries/{id}/reject` 백엔드 존재, 프론트 actions 미호출 |
| T1-5 | **재고이동 TRANSFER UI 생성 불가** — 출고/입고 위치가 단일 창고 로케이션에 공동 바인딩 | `movements-client.tsx:516-555` 양쪽 `activeLocations` 공유. 창고간 이전 불가 |
| T1-6 | **Lot/Serial 추적품목 이동 불가** — 다이얼로그에 lot/serial 필드 없고 `null` 하드코딩 → 백엔드 `LOT_NO_REQUIRED` 거부 | `movements-client.tsx:178`, `MovementService.java:244` |
| T1-7 | **UOM 삭제 참조 가드 부재**(카테고리는 가드함 — 비대칭) | `UomService.delete` 무조건 softDelete, `existsByUom_Id` 부재 |
| T1-8 | **파이프라인 단계 사용중 삭제 미차단**(UI는 차단 약속) | `DELETE /pipeline-stages/1`(기회 참조) → 204 |
| T1-9 | **리드 전환 빈껍데기** — account/contact/opportunity 미생성·리드 데이터 미이관 | 전환 시 status=CONVERTED·convertedAccountId만 set, opp/contact 수 불변 |
| T1-10 | **연차 승인이 잔여일수에 미반영**(데이터 자기모순) | APPROVED 3일 보유 직원의 balance used=0 |

### 🟡 Tier 2 — 완성도·데이터·식별 (Med)

| ID | 결함 | 근거 |
|---|---|---|
| T2-1 | **SelectValue가 라벨 대신 raw value(ID) 표시** — 공유 컴포넌트, 전 모듈 영향 | `ui/select.tsx` children 매핑 부재. 직원선택="12", 연도="2026" |
| T2-2 | **사용자 식별자 UUID 노출 + 이름해소 레이어 부재** | 감사 performedBy·결재 requesterId·IAM·CRM ownerId 전부 Keycloak sub. `lib/`에 sub→이름 0 |
| T2-3 | **상세(drill-in) 화면 전무** (`onRowClick` 0회) — 전표 라인·감사 변경전후·결재선 이력 조회 불가 | 전 모듈 |
| T2-4 | 활동을 담당자(Contact)·기회(Opportunity)에 연결 불가 | 생성폼에 고객사 Select만, contactId/opportunityId 항상 null |
| T2-5 | 기회 확률↔단계 분리(기본 0 고정, 단계변경 시 미갱신) | PipelineStage.probability가 dead field |
| T2-6 | 예약/가용 재고가 dead field(`qty_reserved` 미기록) | 항상 예약=0. 출고 예약/할당 미구현 |
| T2-7 | 부서 재편성 불가(수정에 상위부서·활성 필드 없음) | `departments-client.tsx` 수정 다이얼로그 |
| T2-8 | 감사로그 필터 빈약(entityType 1종) + 내보내기 부재 + IP 항상 null | 액션·기간·수행자 필터 없음 |
| T2-9 | IAM 사용자 존재검증 없음 → 유령 sub에 역할부여 가능 | 없는 sub 조회 200·빈 역할로 배정 진행 |
| T2-10 | 안전재고/재주문점 경고 부재(현황 화면) | 미달 품목(보유5 vs min150) 무신호 |
| T2-11 | ADJUSTMENT 방향·검증 공백(빈 조정 no-op 가능) | `MovementService` ADJUSTMENT 분기 없음 |

### 🟢 Tier 3 — 용어·일관성·UX polish (별도 용어 감사 + Low)

- **용어**(별도 감사): AP전표·GL전표→매입/일반전표, 정산방향→대차구분, 불량→부적격, 인보이스→계산서 + 비일관 8쌍 용어집 단일화 + enum 라벨맵 정리.
- **UX polish**: Select placeholder 표준화, 날짜 포맷 유틸 통일, FormField 인라인 검증 확산, EmptyState/PageHeader 미적용 화면 통일, `window.confirm`→공통 Dialog, 필수표시(*) 표준화, stocks/locations 초기 무선택 UX.
- **A4(부수)**: DELETE 동사 의미 3종 혼재 + budget·salesTeam 하드삭제(DB표준 위반).

---

## 2. 수정 후 수용기준 (AC)

- **AC-T0-1:** invoices·ar-invoices가 실데이터로 크래시 없이 렌더(인보이스 10건 표시) + 페이지네이션 응답을 `apiGetPage`로 안전 처리. 배열캐스팅 패턴 전수 제거.
- **AC-T0-2:** 회계연도·기간을 **생성·마감하는 관리 화면** 제공, 이후 전표 입력·재무제표 산출이 동작.
- **AC-T0-3:** 직원에 매니저/계정(user) 지정 UI 제공 + 결재자가 실사용자로 해소되어 **승인/반려가 끝까지 실행**. SYSTEM fallback 시 명확한 처리(차단·재지정).
- **AC-T0-4:** 소프트삭제된 엔티티는 목록·단건·집계·리포트에서 제외. `P0SoftDeleteFilterIntegrationTest` GREEN + 대표 모듈 표본.
- **AC-T0-5:** FX 조회 200 + 환율 부재 시에도 환산 정책 명확. "환산 합계"는 외화 포함 또는 미환산임을 정직 표기.
- **AC-T1:** version 응답 증가 후 값(RED→GREEN)·입력오류 400(RED→GREEN)·역분개/반려 동작·TRANSFER 및 lot/serial 이동 생성·UOM/단계 삭제 가드·리드 전환 실데이터 생성·연차 잔여 반영.
- **AC-T2:** SelectValue 라벨 표시·사용자 이름 해소·핵심 상세 뷰·활동 연결·확률 연동·예약재고 또는 컬럼 제거·부서 재편성·감사 필터/내보내기·IAM 사용자 검증.
- **AC-T3:** 용어집 단일화·UX 표준 컴포넌트 통일(e2e/`/qa` 라벨 단언 갱신, 약화 금지).

---

## 3. PR 분해·순서·의존 (작은 응집 PR — 한 PR = 한 결함군/모듈)

| PR | 티어 | 범위 | 의존 | 비고 |
|---|---|---|---|---|
| **PR1** | T0 | T0-1 invoices/ar-invoices 크래시 수정(배열캐스팅 전수) | — | 즉시·저위험·고가치 |
| **PR2** | T0 | T0-4 소프트삭제 필터(@Entity 적용/@Filter 전환) + T1-1 version + T1-2 400. RED→GREEN(`fix/p0-data-integrity`) | — | 전 엔티티 영향, 회귀 표본 필수 |
| **PR3** | T0 | T0-5 FX 500 수정 + 환산 정책 정직화 | — | 재무 정확성 |
| **PR4** | T0 | T0-3 결재자 해소: 매니저/user 지정 UI + 결재선 fallback 정책 | — | 워크플로 핵심 |
| **PR5** | T0 | T0-2 회계기간 관리 화면(신규 기능) | — | 규모 큼(별도 /plan) |
| **PR6** | T1 | 전표 역분개·반려(T1-3·4) | PR2 | finance 응집 |
| **PR7** | T1 | 재고 TRANSFER·lot/serial·ADJUSTMENT·삭제가드(T1-5~7) | PR2 | inventory 응집 |
| **PR8** | T1 | CRM 리드 전환·단계 삭제가드(T1-8·9) + 연차 잔여(T1-10) | PR2 | |
| **PR9** | T2 | 용어 일괄(T3 용어) | — | 병행 가능·저위험 |
| **PR10** | T2 | SelectValue 라벨(T2-1, 공유 컴포넌트) | — | 전 모듈 파급 |
| **PR11** | T2 | 사용자 이름 해소 백엔드+프론트(T2-2) | PR2 | |
| **PR12+** | T2/T3 | 상세 뷰·활동연결·감사필터·UX polish | 응집 단위 | 모듈별 다수 |

**권장 순서:** PR1(크래시) → PR2(데이터정합성 RED→GREEN) → PR3(FX) → PR9(용어, 병행) → PR4·PR5(결재·회계기간) → PR6~PR8(워크플로) → PR10~(완성도).

---

## 4. 검증 전략

- **Tier 0/1 백엔드:** `fix/p0-data-integrity` RED 3종 GREEN + 대표 모듈 표본 통합테스트. 회귀 `cd backend && ./gradlew check`.
- **크래시·워크플로:** 실 세션 Playwright(이번 QA 하네스 `e2e/explore.config.ts` 패턴 재사용)로 invoices 렌더·결재 승인·재고 이전·리드 전환을 **실데이터 end-to-end** 단언. 더미세션 스모크로는 불충분(이번 사고의 교훈).
- **데이터 정확성:** API/DB 대조(인보이스 합계·환산·재고수량·연차잔여).
- **프론트:** `type-check && lint && lint:design && build`. 용어집 대조. e2e 라벨 단언 갱신(약화 금지).

---

## 5. 경계 / Do-Not

- ✅ **해도 됨:** 크래시·응답매핑·필터·예외핸들러 수정, 미연결 워크플로(역분개·반려·전환) 연결, 회계기간 UI 신설, 라벨/용어 교체, 이름해소 레이어, 표준 컴포넌트 도입.
- ⚠️ **먼저 물어봐:** 사용자 디렉터리 출처(Keycloak Admin API vs 로컬 user 미러), 회계기간 관리 UX 범위, DELETE 의미 통일로 API 계약 변경, FX 환율 부재 시 환산 정책, enum **코드값** 변경 여부(라벨만 바꾸는 게 원칙).
- 🚫 **절대 금지:** 정확한 회계 엔진(재무제표·차대·계정유형) 동작 변경, 테스트 약화·게이트 우회, 한 PR에 여러 티어 몰기(큰 diff), 시크릿 커밋, main/develop 직접 푸시.

---

## 6. Open Questions

- [ ] **회계기간 관리(T0-2)** UX 범위 — 연도/기간 생성·마감·재오픈 어디까지? 별도 `/plan` 필요.
- [ ] **결재자 해소(T0-3)** — 매니저 기반 결재선 + SYSTEM fallback 정책(미지정 시 차단? 관리자 결재?).
- [ ] **사용자 이름 해소(T2-2)** 출처: Keycloak Admin API 조회 vs 로컬 user 미러 테이블(성능·오프라인).
- [ ] **FX 500(T0-5)** 근본원인(환율 0 vs 코드 버그) 별도 확인 후 환산 정책 확정.
- [ ] enum 라벨 변경 시 **DB 코드값 불변**(라벨 매핑만 교체, 마이그레이션 불필요) 확인.

## 7. QA 계약 develop 병합 후 현행 판정 (2026-10-08)

- [PR #255](https://github.com/grinvi04/erp/pull/255)의 최종 head `7a13802ce712edb93240933bcd7841b629b42574`에서 develop 보호에 필요한 8개 검사(`backend`, `frontend`, `secret-scan`, `test-guard`, `commitlint`, `migration-safety`, `e2e`, `repo-sync`)가 모두 PASS였다. PR은 [병합 커밋 `9acfb7600c2f2e3abfaf6886211a6fd20e0fe4cc`](https://github.com/grinvi04/erp/commit/9acfb7600c2f2e3abfaf6886211a6fd20e0fe4cc)으로 develop에 반영됐다. main/default의 trusted 검사 전환과 운영 릴리즈는 별도 단계이며 기존 보호 게이트를 유지한다.
- 로컬 보안·프론트 범위는 고정 코드 후보 `18b18a0633c332407a0679807664af2d9d2da4fb`의 입력을 최종 PR head까지 대조해 재사용했다. 깊은 `braces` 패턴 거부 시험 11건, 프론트 단위 60건, 합성 세션·백엔드 부재 브라우저 38건과 Docker 설치·빌드는 기록된 범위에서 PASS였다. 독립 읽기 전용 검토는 해당 후보에서 추가 P1/P2를 발견하지 못했다. 명령·실패·소스 지문은 [로컬 QA 증거](erp-braces-local-evidence.json)와 `$HOME/Documents/Codex/2026-10-07/erp-braces-local-adoption/`에 보존했다.
- 전체 의존성 감사 high 9건·운영 의존성 감사 high 7건은 **FAIL**이다. 공식 `braces` 수정판은 확인되지 않았고 확인된 최신 3.0.3도 해당 advisory 범위에 포함돼 로컬 패치를 유지한다. GitHub deployment·외부 commit status·Vercel PR 댓글은 최종 PR head에서 0건이었고, Vercel CLI 인증이 없어 preview URL·실화면은 **UNVERIFIED**다. 실 Keycloak·업무 API의 원격 UAT, 운영 환경·백업·복구·출시 게이트도 미완료다. 다음 단계는 별도 승인된 환경에서 preview·실스택 UAT·감사 잔여 위험을 판정하고 [릴리즈 준비 체크리스트](../release-readiness.md)의 미완료 항목을 유지하는 것이다. develop 병합은 상용 출시 승인이 아니다.

## 8. 오래된 의존성 PR 정리 (2026-10-09)

기준 develop은 `c8cb9056265c8f21c8e8ddb5f6f3c05cc194b445`이다. 현재 버전·직접 소비자와 대조한 뒤 호환 가능한 변경만 새 `fix/dependency-pr-refresh` 후보에 반영한다. 과거 PR의 실패 기록은 보존하며 새 후보의 결과로 대체 여부를 판단한다.

| 구 PR | 현재 판정 / 이번 범위 |
|---|---|
| #247, #249 | shadcn 4.21.3·Vitest 4.1.11이 이미 반영되어 구 목표를 대체했다. 추가 업데이트 없이 닫는다. |
| #246 | React만 올리면 React DOM과 정확한 버전이 달라 렌더 오류가 난다. 두 패키지를 함께 19.2.8로 고정하고 타입 lock을 맞춘다. |
| #245, #248, #250 | 기존 허용 범위 안에서 Sonner 2.0.8·Playwright 1.62.1·Recharts 3.10.1 lock을 갱신한다. |
| #236, #238, #241–243 | checkout 7.0.1·setup-node 7·dependency-review 5·gitleaks 3·setup-java 6으로 갱신하되, Harness v0.81.0 정본과 일치하는 `commitlint-trusted.yml`은 그대로 유지한다. |
| #240 | Mailpit 1.31.0의 고정 digest를 반영하고 별도 임시 컨테이너에서 readiness·SMTP 수신·조회 UI를 확인한다. |
| #254, #237 | Java 21·PostgreSQL 16 제품 계약을 유지해 닫는다. Java 24는 기존 빌드 실패·지원 종료, PostgreSQL 18은 데이터 경로·DB 전환 미검증이다. 자동 업데이트에도 이 두 major 전환을 제외한다. |
| #239 | **병합 보류.** Keycloak 26.0은 아직 26.7로 전환되지 않았다. 현재 develop 기반 후보, 승인된 DB 사본에서 전환·재시작, readiness, 실제 OIDC 로그인·refresh·issuer/JWKS·tenant claim 및 잘못된 토큰/테넌트 거부를 확인한 뒤 재개한다. 기존 DB와 인증 환경을 이번 작업에서 바꾸지 않는다. |

### 이번 후보의 수용 기준과 관찰 경계

- 설치·React 서버 렌더·기본 인증 화면 렌더가 정상이어야 한다. React/React DOM 불일치 후보는 같은 렌더 검사에서 거부되어야 한다.
- 프론트 타입·format·lint·디자인 검사·단위·빌드·기본 E2E, 백엔드 check/build 및 고정 의존성 패치 검사를 통과해야 한다. 기본 E2E는 합성 세션·백엔드 부재 범위이며 실 Keycloak/업무 UAT를 대신하지 않는다.
- Recharts 차트·Sonner 토스트의 대표 렌더/표시를 격리 브라우저에서 확인한다. 전체 업무 화면의 수용 기준 §2는 완료 처리하지 않는다.
- Mailpit 임시 컨테이너의 readiness와 SMTP로 보낸 합성 메일의 내용·UI 조회를 확인한다. 기존 컨테이너·메일·DB를 사용하거나 변경하지 않는다.
- YAML·고정 Action SHA·정본 일치·repo-sync 및 최신 PR head의 필수 CI·미해결 리뷰 스레드 0·독립 리뷰를 확인한 뒤 develop에만 병합한다. 릴리즈·운영 배포는 범위 밖이다.
- 구 PR은 이미 반영됨/제품 기준과 충돌함 또는 새 후보 병합으로 대체됨의 근거를 구분해 닫는다. Keycloak PR은 재개 조건과 함께 유지한다.

### 현재 검증 결과

- 변경한 프론트 package/lock과 YAML·이미지 입력을 고정한 로컬 검사에서 `npm ci`, 의존성 패치·거부 시험 11건, 타입·format·lint·디자인 검사, 단위 60건, 빌드가 PASS였다. 백엔드 Java 21의 `./gradlew check build`도 PASS였으며 957건, 실패·오류·skip 0건이었다. 아키텍처 신선도와 Harness v0.81.0 repo-sync 21/21도 PASS였다.
- 기본 E2E는 초기 macOS 브라우저 프로세스 권한으로 실패(21 실패·16 미실행·1 통과)했으며 제품 코드를 바꾸지 않고 허용된 실행 권한으로 재검증해 **38건 모두 PASS**였다. 사용자 공용 브라우저 캐시 잠금 대기도 기록하고 작업 전용 임시 캐시로 분리했다. 이 결과는 실 Keycloak·업무 API UAT가 아니다.
- YAML 구문·Action SHA 고정·React 업데이트 그룹·Java/PG major 제외와 trusted 정본 보존을 대조했다. 다음 Dependabot 예약 실행에서 규칙 적용을 실제 관찰한 결과는 아니다.
- 2026-10-09의 새 npm 감사는 전체 high **10건**, 운영 의존성 high **8건**으로 **FAIL**이다. `braces` 패치·공식 수정판 부재 외에 변경하지 않은 Next.js 16.3.6의 새 advisory(`GHSA-3w37-wq28-93x7`, `GHSA-4jqv-mc3x-m676`, `GHSA-39w2-rjm5-chcv`, `GHSA-f87g-xv8r-7p7x`, `GHSA-mcj8-r9mp-w47p`, `GHSA-cjq9-62q9-8jv4`)가 포함됐다. Next.js 수정판 검토는 별도 후속 후보이며 이번 구 PR 대체를 보안 감사 전체 통과로 표시하지 않는다.
- React 19.2.8 + React DOM 19.2.8 서버 렌더는 PASS였고, 격리한 React DOM 19.2.4 조합은 `Incompatible React versions`로 거부됐다. 실제 Chromium에서 차트 A/B/C 세 축 값·세 점의 표시, 클릭 전 부재/클릭 후 1개의 토스트, 페이지 오류 0건을 확인했다. 최초 임시 시험은 이전 SVG 축 내부 구조를 선택해 실패했으며 현재 DOM의 별도 tick-label 레이어를 확인한 뒤 같은 값·표시 기준으로 PASS였다. 제품 코드와 원래 기대값을 바꾸지 않았다.
- Mailpit 1.31.0의 명시 digest를 별도 임시 컨테이너로 실행해 healthy, 초기 메일 0건, SMTP 합성 메일 수신 후 정확히 1건·제목·본문·HTML, HTTP UI 및 브라우저의 메일 본문 표시가 PASS였다. 기존 컨테이너·DB·메일에는 접근하지 않았다.
- 두 독립 읽기 전용 검토에서 담당 입력의 지문·직접 소비자·trusted 계약을 대조해 추가 P1/P2를 발견하지 못했다. 위 결과는 PR 제출 전 로컬 후보의 기록이다. 최종 PR head·원격 필수 CI·리뷰·병합 및 구 PR 정리 결과는 이 변경을 포함하는 PR 본문에서 현행화한다. §7의 원격 UAT·출시 미완료를 유지한다.
