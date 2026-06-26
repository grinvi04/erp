# hr-inventory-analytics 스펙·플랜

## Context (왜)
현재 analytics는 CRM(파이프라인·리드)·Finance(월별 인보이스)만 있고 **HR·Inventory 대시보드가 없다**. 인사·재고 관리자가 핵심 지표를 한눈에 못 본다. 기존 analytics 패턴(인터페이스 projection + GROUP BY 쿼리 + 서비스 + `/api/{module}/analytics/*` + 프론트 `/analytics` 허브 페이지)을 그대로 복제해 두 모듈의 대시보드를 추가한다. **신규 코드 최소화 — 기존 청사진을 따른다.**

---

## 1. 목표 & Why
HR·Inventory 관리자가 인원 구성/추이·재고 구성/가치/추이를 시각적으로 파악. **성공 기준: `/analytics` 페이지에 HR 6개·Inventory 6개 차트가 실데이터로 렌더되고, 각 집계 엔드포인트가 권한·(HR)데이터스코프를 준수.**

## 2. Scope
- **In:**
  - **HR analytics 6** (모두 `employeeScope`/`leaveRequestScope` 적용, `HR_EMPLOYEE_READ`/`HR_LEAVE_READ`):
    1. 재직 상태 분포(EmployeeStatus별 count)
    2. 부서별 인원(Department별 ACTIVE count)
    3. 직위별 인원(Position별 ACTIVE count)
    4. 고용형태별 분포(EmploymentType별 count)
    5. 월별 입사/퇴사 추이(year, hire_date·termination_date 월별 두 시리즈)
    6. 휴가 유형별 신청(LeavePolicy.leaveType별 APPROVED 건수·일수)
  - **Inventory analytics 6** (`INVENTORY_READ`, 데이터스코프 없음 — 기존 InventorySummary와 동일):
    1. 카테고리별 활성 품목 수
    2. 창고별 재고 가치(Σ qtyOnHand×unitCost)
    3. 창고별 재고 수량(Σ qtyOnHand)
    4. 이동유형별 건수(MovementType별 CONFIRMED)
    5. 월별 입출고 추이(year, MovementType별 월별 수량·건수)
    6. 저재고 품목 목록(Σqty ≤ reorderPoint, sku·name·카테고리·현재고)
  - 프론트: 기존 `/analytics` 페이지에 **HR 섹션·Inventory 섹션** 추가(기존 SectionCard·HorizontalBar·막대차트 재사용).
- **Out (Non-goals):**
  - 급여 기반 지표(직급별 평균급여 등) — 개인정보·급여이력 부재로 제외.
  - 재고회전율·ABC분석·공급자 분석 — COGS/구매 FK 부재.
  - 새 차트 라이브러리·새 프론트 인프라(기존 막대/HorizontalBar만).
  - FX/멀티커런시(재고가치는 단일 기준통화 — Stock에 currency 없음, ₩ 기준 표시).
  - 결재워크플로·FX는 별도 기능(후속 plan).

## 3. 기능 요구사항 + 수용기준 (테스트 계약)
- **AC-1 (HR 상태분포, 정상):** WHEN `GET /api/hr/analytics/status-distribution`, the system SHALL EmployeeStatus별 count를 status 순서로 반환(스코프 적용).
- **AC-2 (HR 부서별, 정상):** WHEN `GET /api/hr/analytics/by-department`, the system SHALL 부서별 ACTIVE 인원 count 반환. 인원 0인 부서도 보존(LEFT JOIN, count=0).
- **AC-3 (HR 직위별, 정상):** 직위별 ACTIVE count 반환(level_order 순).
- **AC-4 (HR 고용형태별, 정상):** EmploymentType별 count 반환.
- **AC-5 (HR 월별 입퇴사, 정상):** WHEN `GET /api/hr/analytics/hires-terminations?year=Y`, the system SHALL 1~12월 입사/퇴사 두 시리즈를 0채움해 반환.
- **AC-6 (HR 휴가유형, 정상):** LeavePolicy.leaveType별 APPROVED 신청 건수·합계일수 반환.
- **AC-7 (HR 스코프, 경계):** WHILE 사용자 DataScope=DEPARTMENT, 모든 HR 집계 SHALL 자기 부서+하위부서로 한정(다른 부서 인원 비노출).
- **AC-8 (HR 권한, 예외):** IF HR_EMPLOYEE_READ 없음 THEN 403(FORBIDDEN). 휴가 지표는 HR_LEAVE_READ.
- **AC-9 (Inv 카테고리별, 정상):** WHEN `GET /api/inventory/analytics/by-category`, the system SHALL 카테고리별 활성 품목 수 반환. 빈 카테고리도 보존(count=0).
- **AC-10 (Inv 창고별 가치/수량, 정상):** WHEN `GET /api/inventory/analytics/by-warehouse`, the system SHALL 창고별 Σqty·Σ(qty×unitCost) 반환(활성 창고).
- **AC-11 (Inv 이동유형별, 정상):** MovementType별 CONFIRMED 건수 반환.
- **AC-12 (Inv 월별 입출고, 정상):** WHEN `?year=Y`, MovementType별 1~12월 수량·건수 0채움 반환(CONFIRMED만).
- **AC-13 (Inv 저재고, 정상):** Σqty ≤ reorderPoint인 활성 품목을 sku·name·카테고리·현재고와 함께 반환.
- **AC-14 (Inv 권한, 예외):** IF INVENTORY_READ 없음 THEN 403.
- **AC-15 (프론트, 정상):** `/analytics` 페이지가 HR 6·Inventory 12개 섹션을 렌더. 한 엔드포인트 실패해도 나머지 정상 렌더(safeGetArray try-catch).
- **AC-16 (빈 데이터, 경계):** 데이터 0건 시 각 섹션 "데이터 없음" 표시(빈 배열 처리).

## 5. 경계 / Do-Not
- ✅ 해도 됨: 기존 analytics 패턴 그대로 복제, 새 projection/DTO/쿼리, `/analytics` 페이지 확장.
- ⚠️ 먼저 물어봐: 차트 종류 변경·지표 추가/삭제, 재고가치 통화 정책 변경, 별도 페이지 분리(현재는 `/analytics` 허브 확장).
- 🚫 절대 금지: 기존 CRM/Finance analytics·summary 동작 변경, 급여 개인정보 노출, 크로스모듈 직접참조, 마이그레이션(읽기 전용 집계라 스키마 불변).

## 7. 기술 접근 (HOW)
**패턴(청사진 — CrmAnalytics/FinanceAnalytics 복제):**
- 백엔드 3계층: `adapter/in/web/{Hr,Inventory}AnalyticsController` → `application/service/{Hr,Inventory}AnalyticsService`(`@Transactional(readOnly)`, `permissionChecker.require`, HR은 `HrDataScopeResolver` 적용) → `domain/repository`에 인터페이스 projection(`*Row`) + `@Query`(GROUP BY, LEFT JOIN으로 빈 그룹 보존, `COALESCE(...,0)`).
- 월별(AC-5·12): Finance `monthlyInvoices` 패턴(`EXTRACT(MONTH..)`, year 파라미터, 서비스에서 1~12월 0채움, LinkedHashMap 순서보존).
- 응답: `ResponseEntity<ApiResponse<List<DTO>>>`, DTO는 record. 재고가치는 BigDecimal(기준통화 ₩, 단일).
- 프론트: `frontend/src/app/(dashboard)/analytics/page.tsx`에 HR·Inventory `safeGetArray` fetch + SectionCard/HorizontalBar/막대차트 섹션 추가. `frontend/src/types/analytics.ts`에 응답 타입 추가. 재고가치 표시는 기존 `formatMoneyOne(v,'KRW')`(lib/money) 재사용.
- 데이터스코프(HR): `HrDataScopeResolver.employeeScope()`/`leaveRequestScope()`를 집계 쿼리 조건에 결합(CRM analytics가 ownerScope 거는 방식과 동일). Inventory는 스코프 없음.

**테스트 전략 (AC↔테스트):**
- 백엔드 통합테스트(`AbstractIntegrationTest`, JWT+권한 세팅): AC-1~6·9~13 각 엔드포인트 집계 정확성(분포·월별 0채움·빈 그룹 보존·저재고 판정), AC-7 스코프 한정(DEPARTMENT 사용자), AC-8·14 권한 403. 기존 PipelineDistribution/MonthlyInvoice 테스트 패턴 모방.
- 프론트: 순수 표시(서버컴포넌트 집계 렌더) — 단위테스트 대상 아님, type-check+build로 검증.

## 8. 태스크 (test-first, 모듈별 2 PR 권장)
> 규모(12차트)상 **HR PR → Inventory PR** 두 개로 분리(각 모듈 독립 출시 가능). 한 모듈=한 feature 브랜치=한 PR.

### PR1 — feature/hr-analytics
| # | 태스크 | AC | 대상 | 검증(exit 0) | 의존 |
|---|---|---|---|---|---|
| 1 | HR 분포 집계(상태·부서·직위·고용형태) projection+쿼리+DTO+서비스+컨트롤러, 스코프·권한 | AC-1~4,7,8 | hr/domain/repository, application/service/HrAnalyticsService, adapter/in/web/HrAnalyticsController, dto | `./gradlew check` | — |
| 2 | HR 월별 입퇴사 추이(year, 0채움) | AC-5 | HrAnalyticsService·repo·dto | `./gradlew check` | #1 |
| 3 | HR 휴가유형별 신청(leaveRequestScope, HR_LEAVE_READ) | AC-6,8 | HrAnalyticsService·LeaveRequest repo·dto | `./gradlew check` | #1 |
| 4 | 프론트 `/analytics`에 HR 6섹션 + 타입 | AC-15,16 | analytics/page.tsx, types/analytics.ts | `npm run type-check && npm run build` | #1~3 |

### PR2 — feature/inventory-analytics
| # | 태스크 | AC | 대상 | 검증(exit 0) | 의존 |
|---|---|---|---|---|---|
| 5 | Inv 분포 집계(카테고리별 품목·창고별 가치/수량·이동유형별) projection+쿼리+DTO+서비스+컨트롤러, 권한 | AC-9~11,14 | inventory/domain/repository, application/service/InventoryAnalyticsService, adapter/in/web/InventoryAnalyticsController, dto | `./gradlew check` | — |
| 6 | Inv 월별 입출고 추이(year, MovementType별 0채움) | AC-12 | InventoryAnalyticsService·Movement repo·dto | `./gradlew check` | #5 |
| 7 | Inv 저재고 품목 목록 | AC-13 | InventoryAnalyticsService·Item repo·dto | `./gradlew check` | #5 |
| 8 | 프론트 `/analytics`에 Inventory 6섹션 + 타입 | AC-15,16 | analytics/page.tsx, types/analytics.ts | `npm run type-check && npm run build` | #5~7 |

## Verification (end-to-end)
1. `cd backend && ./gradlew check` — 신규 analytics 통합테스트 green + 회귀 없음(기존 CRM/Finance analytics 무영향).
2. `cd frontend && npm run type-check && npm run lint && npm run build` — `/analytics` 빌드.
3. 수동(로컬 docker-compose 기동 시): `/analytics` 페이지에서 HR·Inventory 섹션이 실데이터로 렌더, DEPARTMENT 권한 사용자로 로그인 시 HR 집계가 자기 부서로 한정되는지 확인.
4. 각 게이트: `/feature-add`(태스크별 TDD·원자적 커밋) → 모듈별 `/feature-merge`(focused 리뷰·CI).
