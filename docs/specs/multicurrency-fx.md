# multicurrency-fx 스펙·플랜

## Context (왜)
거래는 원통화만 저장하고(#60/#61) 대시보드·analytics는 통화별로 **분리 표시**만 한다("KRW 1,000,000 · USD 500,000") — 서로 다른 통화를 한 **기준통화**로 합산·환산하지 못한다. FX 인프라(기준통화 설정·환율·환산)가 전무. 테넌트 기준통화 + 환율(외부 API) + **거래 시점 환산액 저장(GAAP 정석)**을 추가해 혼합통화를 기준통화 합계로 본다.

**확정된 제품 결정**: ① 환율 소스 = **외부 API**(erp 미배포라 외부 연동은 **provider 포트 + 키리스 HTTP impl + 수동 보정**, 온디맨드 조회; @Scheduled 자동수집은 배포 후 후속). ② 환산 = **거래 시점 고정**(거래 생성 시 그 시점 환율로 기준통화액·환율을 거래에 함께 저장 — 환율이 변해도 과거 거래 합계 불변, 회계 정석). ③ 적용 = **대시보드 요약 + analytics**에 기준통화 합계.

> ⚠️ 규모: 거래 4개 엔티티(ApInvoice·ArInvoice·JournalEntry·Opportunity) 스키마 변경+백필, 외부 연동, 환산 서비스 — **3개 PR·다중 세션 기능**. 단계별 독립 출시.

---

## 1. 목표 & Why
혼합통화 거래를 테넌트 기준통화로 환산·합산해 본다. **성공 기준: 기준통화 설정 가능, 환율(외부+수동) 조회 가능, 거래 생성 시 기준통화 환산액 저장, 대시보드·analytics가 통화별 분리 + 기준통화 합계를 함께 표시.**

## 2. Scope
- **In:**
  - **기준통화 설정**: 테넌트별 `TenantBaseCurrency`(기본 KRW), 관리자 변경(FINANCE_SETTING_WRITE). FiscalYear 마스터 패턴.
  - **환율**: `ExchangeRate`(fromCurrency→기준통화, effectiveDate, rate) 테이블 + 조회(일자 ≤ date 최신). **provider 포트 + 키리스 외부 HTTP impl**(예 frankfurter/ECB, 키 불필요)로 온디맨드 수집·저장, **수동 등록/보정** 가능.
  - **환산 서비스**: `CurrencyConverter`(amount·currency·date → 기준통화액, rate≤date 사용; 기준통화는 rate=1; **환율 부재 시 명시적 처리**).
  - **거래 시점 스냅샷**: ApInvoice·ArInvoice·JournalEntry·Opportunity에 `base_amount`·`exchange_rate` 컬럼 추가, **생성 시 환산액·환율 저장**. 기존 행 백필(기준통화=원통화면 rate=1·base=원액; 그 외는 가용 환율 또는 null 플래그).
  - **표시**: 대시보드 요약(미지급·파이프라인)·analytics(월별·파이프라인)에 **기준통화 합계** 추가(통화별 분리 유지 + 합계 한 줄). FX 설정·환율 관리 화면.
  - 권한 `FINANCE_SETTING_WRITE="finance:setting:write"` 신설+백필.
- **Out (Non-goals):**
  - 환율 **자동 스케줄 수집**(@Scheduled/cron) — 배포 후 후속(온디맨드+수동으로 대체).
  - 재무제표(재무상태표·손익계산서) — 아직 미존재, 별도 기능.
  - 환차손익(FX gain/loss) 회계 분개 — 정산 시점 평가손익은 범위 밖.
  - 거래별 통화 변경 UI·다통화 입력 폼 개편(기존 통화 입력 유지).
  - 결재·SoD 변경.

## 3. 기능 요구사항 + 수용기준 (테스트 계약)
**Phase 1 — FX 기반(PR1)**
- **AC-1 (기준통화 설정, 정상):** WHEN 관리자가 기준통화 조회/변경(FINANCE_SETTING_WRITE), the system SHALL 테넌트당 1개 기준통화를 반환/저장(미설정 시 KRW 기본).
- **AC-2 (환율 등록, 정상):** WHEN 환율 수동 등록(from·to=기준·date·rate), the system SHALL 저장(테넌트·통화쌍·일자 UNIQUE, 중복 거부).
- **AC-3 (환율 조회, 경계):** WHEN 특정 통화·일자 환율 조회, the system SHALL effectiveDate ≤ 조회일 중 최신 rate 반환; 기준통화면 1.
- **AC-4 (외부 수집, 정상):** WHEN provider로 환율 수집(온디맨드), the system SHALL 외부 rate를 ExchangeRate로 저장(provider는 포트 — 테스트는 mock).
- **AC-5 (환율 부재, 예외):** IF 통화 환율이 없음 THEN 환산 SHALL 명시적 실패/스킵(CURRENCY_RATE_NOT_FOUND) — 0이나 1로 조용히 처리 금지.
- **AC-6 (환산, 정상):** WHEN CurrencyConverter.convert(amount, currency, date), the system SHALL base = amount × rate(통화,date) 반환(기준통화면 amount 그대로).
- **AC-7 (권한, 예외):** IF FINANCE_SETTING_WRITE 없음 THEN 설정/환율 변경 403.

**Phase 2 — 거래 스냅샷(PR2)**
- **AC-8 (생성 시 스냅샷, 정상):** WHEN ApInvoice/ArInvoice/JournalEntry/Opportunity 생성, the system SHALL 생성일 기준 환율로 `base_amount`·`exchange_rate`를 거래에 저장(기준통화면 rate=1).
- **AC-9 (스냅샷 불변, 경계):** WHILE 이후 환율이 변경돼도, 기존 거래의 base_amount SHALL 불변(거래 시점 고정).
- **AC-10 (백필, 정상):** 마이그레이션 후 기존 거래는 base_amount 보유(기준통화=원통화 행은 원액·rate1; 비기준 행은 가용 환율 또는 미산정 플래그).
- **AC-11 (환율 부재 생성, 예외):** IF 생성 시 해당 통화 환율 없음 THEN 정책에 따라 거부 또는 base 미산정 플래그(조용한 0 금지).

**Phase 3 — 표시(PR3)**
- **AC-12 (대시보드 합계, 정상):** 대시보드 미지급·파이프라인이 통화별 분리 + **기준통화 합계**(Σbase_amount) 표시.
- **AC-13 (analytics 합계, 정상):** 월별·파이프라인 analytics가 통화별 + 기준통화 합계 표시.
- **AC-14 (FX 관리 화면, 정상):** 기준통화·환율 조회/등록 화면 동작(FINANCE_SETTING_WRITE 게이팅).

## 4. 제약/비기능
- 외부 provider는 **포트 뒤로 격리**(테스트는 mock, CI 네트워크 의존 금지). 키리스 엔드포인트 사용, 실패 시 수동 환율로 graceful.
- 거래 스냅샷 마이그레이션은 forward-only·무중단(nullable 컬럼 추가 후 백필).
- BigDecimal 정밀도: rate(precision 18, scale 8 등 충분), base_amount(20,2).
- 기존 마이그레이션 무수정.

## 5. 경계 / Do-Not
- ✅ 해도 됨: FiscalYear 마스터 패턴 복제, CurrencyAmount/lib money 확장, 새 엔티티·환산 서비스·provider 포트, 4 거래 스냅샷 컬럼.
- ⚠️ 먼저 물어봐: 환율 부재 시 거래 생성 거부 vs 미산정 허용(Phase2 진입 시 확정), 스케줄 자동수집 도입, 재무제표·환차손익 착수, 기준통화 변경 시 과거 스냅샷 재계산 여부.
- 🚫 절대 금지: 환율 없음을 0·1로 조용히 처리(잘못된 합계), CI에서 실제 외부 API 호출, 거래 통화/금액 원본 변경, 기존 #60/#61 통화별 분리표시 제거(합계는 *추가*), 시크릿(API키) 커밋.

## 7. 기술 접근 (HOW)
**패턴(기존 재사용):** FiscalYear/Account 마스터(엔티티 BaseEntity + repo + service + controller + 마이그레이션 V2xxx), CurrencyAmount(common/response), 권한+백필(#65 V0004), 프론트 lib/money·types/money·기준정보 CRUD 화면.
- **신규 엔티티(finance/domain/model)**: `TenantBaseCurrency`(tenantId UNIQUE, baseCurrency), `ExchangeRate`(tenantId, fromCurrency, toCurrency, effectiveDate, rate; UNIQUE(tenant,from,to,date)). repo: 최신 rate≤date 조회.
- **provider 포트(application/port)**: `ExchangeRateProvider`(fetch(from,to,date)) + adapter impl(키리스 HTTP, WebClient/RestClient). `ExchangeRateService`(provider 수집+저장+조회+수동등록).
- **CurrencyConverter(domain/application service)**: convert(amount,currency,date)→base. 환율 부재 시 ErrorCode.CURRENCY_RATE_NOT_FOUND.
- **거래 스냅샷(Phase2)**: 4 엔티티에 baseAmount·exchangeRate 필드+컬럼(V2006 finance, V4xxx crm). create 서비스에서 CurrencyConverter 호출해 저장. 마이그레이션 백필(기준통화 행 rate1, 비기준은 ExchangeRate 조회 또는 null).
- **표시(Phase3)**: 집계 쿼리에 Σbase_amount 추가(스냅샷 저장돼 단순 SUM) → 요약/analytics DTO에 baseTotal 필드. 프론트 formatMoneyOne(base)로 합계 한 줄. FX 관리 화면(기준정보 CRUD 패턴).
- 권한 `FINANCE_SETTING_WRITE`+all()+백필(finance:write→setting:write).

**테스트 전략(AC↔테스트):** 단위(CurrencyConverter 환산·rate≤date·부재예외, provider mock), 통합(설정/환율 CRUD·권한403·스냅샷 생성·백필 flyway·집계 base합계). 프론트 표시는 type-check+build.

## 8. 태스크 (3 PR)
### PR1 — feature/fx-foundation (Phase1: AC-1~7)
| # | 태스크 | AC | 대상 | 검증 | 의존 |
|---|---|---|---|---|---|
| 1 | 권한 FINANCE_SETTING_WRITE+all()+백필, TenantBaseCurrency 엔티티·테이블·CRUD(서비스·컨트롤러) | AC-1,7 | Permission, finance domain/model·service·web, V0007·V2006(설정) | `./gradlew check` | — |
| 2 | ExchangeRate 엔티티·테이블·repo(최신 rate≤date)·수동 등록 CRUD | AC-2,3 | finance ExchangeRate·repo·service·web | `./gradlew check` | #1 |
| 3 | ExchangeRateProvider 포트 + 키리스 HTTP adapter + 수집 서비스(mock 테스트) | AC-4 | application/port, adapter/out, service | `./gradlew check` | #2 |
| 4 | CurrencyConverter(환산·rate≤date·부재예외 CURRENCY_RATE_NOT_FOUND) | AC-5,6 | CurrencyConverter, ErrorCode | `./gradlew check` | #2 |
| 5 | 프론트 FX 설정·환율 관리 화면(기준정보 CRUD 패턴) | AC-14(부분) | frontend finance/fx | `type-check && build` | #1~2 |

### PR2 — feature/fx-transaction-snapshot (Phase2: AC-8~11)
| # | 태스크 | AC | 대상 | 검증 | 의존 |
|---|---|---|---|---|---|
| 6 | 4 거래 엔티티 base_amount·exchange_rate 컬럼+마이그레이션+백필 | AC-10 | ApInvoice·ArInvoice·JournalEntry·Opportunity, V2007·V4xxx | `./gradlew check` | PR1 |
| 7 | 생성 서비스에서 CurrencyConverter로 스냅샷 저장(부재 처리 정책 확정) | AC-8,9,11 | 각 create 서비스 | `./gradlew check` | #6 |

### PR3 — feature/fx-base-total-display (Phase3: AC-12~14)
| # | 태스크 | AC | 대상 | 검증 | 의존 |
|---|---|---|---|---|---|
| 8 | 요약/analytics 쿼리·DTO에 Σbase_amount 합계 추가 | AC-12,13 | finance/crm summary·analytics service·dto·repo | `./gradlew check` | PR2 |
| 9 | 프론트 대시보드·analytics 기준통화 합계 표시 + FX 화면 마무리 | AC-12,13,14 | frontend page·analytics·money | `type-check && build` | #8 |

## Verification (end-to-end)
1. `cd backend && ./gradlew check` — FX 단위/통합 green + 회귀 없음(기존 통화별 분리·거래 생성 무영향, provider mock).
2. `cd frontend && npm run type-check && npm run lint && npm run build`.
3. 수동(docker-compose): 기준통화 USD로 변경·환율 등록→KRW 거래 생성 시 base_amount 환산 저장 확인→대시보드에 기준통화 합계 표시. 환율 부재 통화는 명시적 처리 확인.
4. 게이트: 단계별 `/feature-add`(태스크 TDD·원자 커밋)→`/feature-merge`(focused 리뷰·CI). PR1(기반)→PR2(스냅샷)→PR3(표시). 다중 세션.
