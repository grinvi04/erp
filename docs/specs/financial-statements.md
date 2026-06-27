# financial-statements 스펙·플랜

## Context (왜)
GL 분개(JournalEntry/JournalLine)·계정과목(Account)·회계기간(FiscalYear/Period)은 있으나 **재무제표 리포트가 없다** — 전기된 거래를 시산표·재무상태표·손익계산서로 볼 수 없다. 데이터 모델은 이미 충분(Account.accountType·normalBalance·parent_id·isSummary, JournalLine.debit/credit, FiscalPeriod). **순수 집계·조회 리포트**(읽기 전용, 스키마 변경 없음)를 추가한다.

**확정 설계(회계 표준·가용데이터 기반)**: ① 재무제표는 **기준통화**로 — 각 분개의 스냅샷 환율(`exchangeRate`, PR2/#72)을 라인 금액에 곱해 집계(`SUM(debit×rate)`). KRW 단일통화 테넌트는 rate=1이라 raw 합과 동일, 혼합통화도 정합. **환율 미산정(exchangeRate null) 분개는 제외하고 그 건수를 플래그**(조용한 누락 금지). ② **POSTED 분개만**(DRAFT/PENDING_APPROVAL/REVERSED 제외). ③ 시산표·손익계산서=기간(회계연도) 합계, 재무상태표=기준일까지 **누적**(표준 회계).

---

## 1. 목표 & Why
전기된 분개로 시산표·재무상태표·손익계산서를 조회. **성공 기준: 회계연도 지정 시 (a) 시산표 차변합=대변합, (b) 손익계산서 당기순이익=수익−비용, (c) 재무상태표 자산=부채+자본+당기순이익, 모두 기준통화로 산출.**

## 2. Scope
- **In:** (모두 `FINANCE_READ`, 읽기 전용)
  - **시산표(Trial Balance)** `GET /api/finance/reports/trial-balance?year=Y` — 거래계정(isSummary=false, active)별 차변합·대변합·잔액(기준통화). 합계: Σ차변 == Σ대변. 계정코드 순.
  - **손익계산서(Income Statement)** `?year=Y` — REVENUE/EXPENSE 계정의 기간 발생액(수익=대변−차변, 비용=차변−대변), accountType별 소계 + 당기순이익(수익합−비용합).
  - **재무상태표(Balance Sheet)** `?year=Y`(연말 기준일) — ASSET/LIABILITY/EQUITY 계정의 **누적 잔액**(기초~기준일 POSTED), 유형별 소계 + 균형(자산 == 부채+자본+당기순이익). 당기순이익은 IS에서 산출해 자본에 가산(이익잉여금).
  - 집계: `JournalLineRepository`에 계정별 `SUM(debit×entry.exchangeRate)`·`SUM(credit×entry.exchangeRate)` (POSTED·기간/기준일·exchangeRate not null) 쿼리. 서비스가 accountType·normalBalance로 분류·소계.
  - 프론트: 재무 영역에 시산표·BS·IS 조회 화면(연도 선택, 표 형태). 환율 미산정 제외 건수 안내.
- **Out (Non-goals):**
  - 비교기간(전기 대비 컬럼)·현금흐름표·자본변동표·연결재무제표.
  - 계정 다단계 계층 롤업(부모=자식합 재귀) — accountType 그룹 소계까지만(parent 롤업은 후속).
  - 마감분개 자동화·이익잉여금 대체분개(당기순이익은 표시 계산만).
  - 라인별 환율(분개 단위 exchangeRate 적용) 외 정교한 다통화·환산손익(FX 기능이 별도 처리).
  - PDF/Excel 내보내기·드릴다운·부서별 재무제표.
  - 스키마 변경(기존 Account.accountType 등 그대로).

## 3. 기능 요구사항 + 수용기준 (테스트 계약)
- **AC-1 (시산표 균형, 정상):** WHEN `trial-balance?year=Y`, the system SHALL 거래계정별 차변합·대변합을 기준통화로 반환하고 **총차변==총대변**.
- **AC-2 (POSTED만, 경계):** WHILE 분개가 DRAFT/PENDING_APPROVAL/REVERSED, 그 라인 SHALL 모든 리포트에서 제외(POSTED만 집계).
- **AC-3 (기준통화 환산, 정상):** WHEN 분개 통화≠기준통화, the system SHALL `금액×분개 exchangeRate`로 환산 집계(KRW·rate=1이면 원액 그대로).
- **AC-4 (환율 미산정 제외, 예외):** IF 분개 exchangeRate=null(미산정), THEN 그 분개를 집계에서 제외하고 응답에 **제외 건수**를 포함(조용한 누락 금지).
- **AC-5 (손익계산서, 정상):** WHEN `income-statement?year=Y`, the system SHALL REVENUE 합(대변−차변)·EXPENSE 합(차변−대변)·**당기순이익=수익−비용** 반환.
- **AC-6 (재무상태표 누적, 정상):** WHEN `balance-sheet?year=Y`, the system SHALL ASSET/LIABILITY/EQUITY를 기초~연말 **누적**으로 산출하고 **자산합 == 부채합+자본합+당기순이익**(±0.01).
- **AC-7 (기간 필터, 경계):** 시산표·IS는 해당 연도 entryDate(또는 fiscalPeriod) 범위만; BS는 연말 이전 누적. 다른 연도 분개 비포함/포함 정확.
- **AC-8 (권한, 예외):** IF FINANCE_READ 없음 THEN 403.
- **AC-9 (빈 데이터, 경계):** 전기 분개 0건이면 빈 리포트(합계 0, 균형 유지), 오류 아님.

## 4. 제약/비기능
- 읽기 전용·스키마 불변. BigDecimal scale 2, 균형 비교는 ±0.01 허용오차.
- 성능: 계정별 GROUP BY 집계 1~2쿼리(전 분개 스캔이나 기간 인덱스 활용). 대량 시 인덱스 점검(후속).

## 5. 경계 / Do-Not
- ✅ 해도 됨: 신규 집계 쿼리·리포트 서비스·DTO·조회 화면, accountType/normalBalance 분류 로직.
- ⚠️ 먼저 물어봐: 계정 계층 롤업 도입, 비교기간·현금흐름표 확대, 마감분개 자동화, 라인별 정교 다통화.
- 🚫 절대 금지: 분개/계정 데이터 변경(읽기 전용), 환율 없음을 1·0으로 조용히 처리, 기존 마이그레이션 수정, POSTED 외 분개 집계.

## 7. 기술 접근 (HOW)
**패턴(기존 재사용):** FinanceAnalyticsService(집계 서비스)·projection Row·`/api/finance/analytics` 컨트롤러, Account.accountType/normalBalance, FiscalYearRepository(연도→기간/기준일), 프론트 analytics/기준정보 조회 화면, lib/money(기준통화 표시).
- **집계 쿼리(JournalLineRepository)**: `SELECT l.account.id, SUM(l.debitAmount*e.exchangeRate), SUM(l.creditAmount*e.exchangeRate) FROM JournalLine l JOIN l.journalEntry e WHERE e.status=POSTED AND e.exchangeRate IS NOT NULL AND e.entryDate <조건> GROUP BY l.account.id` (projection Row). 시산표/IS=기간, BS=기준일 누적(같은 쿼리 날짜조건만 다름) + 제외(exchangeRate null) 건수 카운트 쿼리.
- **서비스**: `FinancialStatementService`(또는 TrialBalance/Income/BalanceSheet 분리) — 계정별 잔액 Map + Account(accountType·normalBalance·code·name) 조인 → 유형별 분류·소계·당기순이익·균형. record DTO(계정 행 + 유형 소계 + 합계 + excludedEntryCount).
- **컨트롤러**: `FinancialStatementController` `/api/finance/reports/{trial-balance,income-statement,balance-sheet}` FINANCE_READ.
- **프론트**: `finance/reports` 화면(연도 선택, 3개 탭/페이지 표). 기준통화 표시(formatMoneyOne), 제외 건수 안내.

**테스트 전략(AC↔테스트):** 통합테스트(POSTED 분개 시드 — 계정 유형별 차대변): AC-1 균형, AC-2 비POSTED 제외, AC-3 환율 환산(USD 분개 rate 적용), AC-4 미산정 제외+건수, AC-5 IS 당기순이익, AC-6 BS 균형(자산=부채+자본+순이익), AC-7 연도 필터, AC-8 권한403, AC-9 빈데이터. 단위(분류·소계 로직).

## 8. 태스크 (test-first)
### PR1 — feature/financial-statements (백엔드 3 리포트)
| # | 태스크 | AC | 대상 | 검증 | 의존 |
|---|---|---|---|---|---|
| 1 | 계정별 차대변 집계 쿼리(POSTED·기준통화 환산·미산정 제외+카운트) projection+repo | AC-1~4 | JournalLineRepository, *Row | `./gradlew check` | — |
| 2 | 시산표 서비스+컨트롤러(거래계정 차대변·총합 균형) | AC-1,2,8,9 | TrialBalanceService, FinancialStatementController, dto | `./gradlew check` | #1 |
| 3 | 손익계산서 서비스(수익·비용·당기순이익) | AC-5,7 | IncomeStatementService, dto | `./gradlew check` | #1 |
| 4 | 재무상태표 서비스(자산/부채/자본 누적·당기순이익 가산·균형) | AC-6,7 | BalanceSheetService, dto | `./gradlew check` | #1,#3 |

### PR2 — feature/financial-statements-ui (프론트)
| # | 태스크 | AC | 대상 | 검증 | 의존 |
|---|---|---|---|---|---|
| 5 | 재무 리포트 화면(시산표·IS·BS 탭, 연도선택·표·기준통화·제외건수) | AC-1,5,6,9 | frontend finance/reports, types | `type-check && build` | PR1 |

## Verification (end-to-end)
1. `cd backend && ./gradlew check` — 리포트 통합테스트 green(균형·환산·필터·권한) + 회귀 없음.
2. `cd frontend && npm run type-check && npm run lint && npm run build`.
3. 수동(docker-compose): 계정 유형별 분개 전기 → 시산표 차대변 균형, IS 당기순이익, BS 자산=부채+자본+순이익 확인. USD 분개 환산·미산정 제외 안내 확인.
4. 게이트: `/feature-add`(태스크 TDD)→`/feature-merge`. PR1(백엔드)→PR2(프론트).
