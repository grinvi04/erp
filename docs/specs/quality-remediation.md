# quality-remediation 스펙 — 상용 제품 품질 리메디에이션 로드맵

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
