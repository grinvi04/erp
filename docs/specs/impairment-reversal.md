# impairment-reversal (자산 손상차손 환입) 스펙 + 플랜

> 현지화 로드맵 Tier3 손상차손 후속. 단일 출처: `docs/roadmap-localization.md`.
> 기존 impairment(#160)·FixedAsset·ImpairmentPostingService·BaseCurrencyService 손상 계정 패턴을 확장한다.

## Context (왜)
손상차손(#160)은 인식만 구현하고 **환입(reversal)은 명시적 Out**이었다(impairment.md §2). K-IFRS는 손상 사유가 해소되면 환입을 요구하되, **한도는 "손상을 인식하지 않았을 경우의 장부금액"**(가상 감가상각 반영)이다. 핵심 제약: 손상 기간 동안 실제 상각이 가상 상각보다 적게 누적되므로 **손상누계액 전액 환원은 한도를 초과**한다 → 가상 장부금액을 산정해 상한을 거는 것이 필수(이 기능의 본질적 복잡도). 결과: 회수가능액이 회복된 손상 자산에 환입을 등록하면 한도 내에서 (차)손상차손누계액 (대)손상차손환입 분개가 생성되고 장부가액이 상향되며 향후 상각이 재배분된다.

### 확정 설계 결정 (권장안 — ExitPlanMode에서 승인 확인)
1. **한도 산정 = 온더플라이 재계산.** 환입 시점 경과월수로 가상 장부금액 계산 — 정액=cost−min(경과월×원월상각, cost−residual); 정률=경과월 declining 시뮬레이션(실제 상각과 동일 DOWN 반올림). 새 컬럼·코어 상각 루프 무영향(외과적). 비손상 자산 무관.
2. **환입 대변 = 신규 손상차손환입(수익) 계정.** (대)손상차손환입 — K-IFRS PL 이익. `TenantBaseCurrency`·`ImpairmentAccounts`에 계정 1개 추가.
3. (대칭 기본) 수동 인식·회수가능액 단일입력 / 인식기간까지 상각 catch-up 후 측정 / 환입 후 정액 잔여내용연수 재배분·정률 자동 / (자산,기간,유형) 멱등.
4. (묶음) 직전 보안리뷰 LOW — 손상 인식·환입·계정설정 **음성 인가 테스트**(권한 미보유→거부) 보강.

## 1. 목표 & Why
손상된 고정자산의 회수가능액이 회복되면 손상차손을 **환입**하되 "손상 없었을 경우 장부금액"을 상한으로 한다. **성공 기준: 손상 자산(손상누계액>0)에 회수가능액(>현재 장부가액)을 등록하면, 환입액 = min(min(회수가능액, 한도) − 장부가액, 손상누계액)으로 (차)손상차손누계액 (대)손상차손환입 균형 DRAFT 분개가 생성되고, 손상누계액이 차감되며 장부가액이 한도를 넘지 않고, 이후 월 상각액이 상향된 장부가액 기준으로 재배분된다.**

## 2. Scope
- **In:**
  - **환입 인식(수동)**: 자산·회계기간·회수가능액 입력 → 인식기간까지 상각 catch-up → 한도 산정 → 환입액 산정(한도·손상누계액·회수가능액 3중 상한) → GL 자동분개((차)손상차손누계액 (대)손상차손환입 DRAFT) + 손상누계액 차감 + 이력(REVERSAL). **멱등**: 이미 처리한 (자산,기간,REVERSAL) 거부.
  - **한도 산정(도메인)**: 가상(손상 없었을 경우) 장부금액 — 정액 상수식·정률 시뮬레이션.
  - **환입 후 상각 재계산**: 정액은 (장부가액−잔존)/잔여내용연수 재산정(override 갱신), 정률 자동.
  - **계정 설정**: 손상차손환입(수익) 계정 FK 추가(기존 손상 계정설정 확장).
  - **화면**: 환입 인식 버튼·모달(회수가능액)·이력에 환입 표시·계정설정에 환입 계정 필드.
  - **음성 인가 테스트 보강**(손상 인식·환입·계정설정 권한 미보유 거부).
- **Out (Non-goals):**
  - 재평가모형(환입을 기타포괄손익으로) — 원가모형 PL 환입만.
  - 영업권 손상 환입(K-IFRS 금지) — 대상 아님(고정자산만).
  - 자동 환입징후 감지, 재고 손상 환입, CGU 환입 배분.
  - 환입 분개 자동 POST(결재 경유).

## 3. 기능 요구사항 + 수용기준 (= 테스트 계약)
- **AC-1 (환입 인식, 정상):** WHEN 손상 자산(손상누계액>0)에 회계기간·회수가능액(>현재 장부가액)으로 환입(FINANCE_WRITE), the system SHALL 인식기간까지 상각 catch-up 후 환입액=min(min(회수가능액,한도)−장부가액, 손상누계액)으로 (차)손상차손누계액 (대)손상차손환입 균형 DRAFT 분개를 만들고 손상누계액을 차감하며 이력(REVERSAL)을 남긴다.
- **AC-2 (한도 산정, 경계):** the system SHALL 한도=손상 없었을 경우 장부금액(정액=cost−min(경과월×(cost−residual)/내용연수월, cost−residual); 정률=경과월 declining 시뮬레이션, 실제 상각과 동일 DOWN 반올림)으로 산정한다.
- **AC-3 (한도 상한, 경계):** WHILE 회수가능액이 한도보다 큼, the system SHALL 환입 후 장부가액을 한도까지만 올린다(초과 환입 금지).
- **AC-4 (손상누계 상한, 경계):** the system SHALL 환입액이 손상누계액을 초과하지 않게 한다(인식한 손상보다 많이 환입 불가).
- **AC-5 (환입 후 상각, 정상):** WHILE 정액, the system SHALL 환입 후 (장부가액−잔존)/잔여내용연수로 월상각을 재배분한다. 정률은 상향된 bookValue로 자동.
- **AC-6 (환입 불요, 예외):** IF 회수가능액 ≤ 현재 장부가액 OR 손상누계액=0 OR 산정 환입액 ≤ 0, THEN 차단(4xx, F051).
- **AC-7 (멱등, 경계):** IF 같은 (자산,기간,REVERSAL) 재환입, THEN 거부(F052). 서비스 사전조회 + DB UNIQUE 이중.
- **AC-8 (계정 미설정, 예외):** IF 손상차손누계액/손상차손환입 계정 미설정, THEN 차단(4xx, F050).
- **AC-9 (회수가능액 유효성, 예외):** IF 회수가능액 < 0, THEN 400.
- **AC-10 (회계기간, 경계):** IF 기간 OPEN 아님, THEN 거부.
- **AC-11 (취득 전 기간, 예외):** IF 기간 종료일 < 취득일, THEN 400(impairment과 동일 가드).
- **AC-12 (처분 자산, 예외):** IF 처분된 자산, THEN 거부(FIXED_ASSET_ALREADY_DISPOSED).
- **AC-13 (권한, 예외):** 환입 FINANCE_WRITE, 계정설정 FINANCE_SETTING_WRITE, 분개 결재 FINANCE_GL_APPROVE. 없으면 거부. **음성 인가 테스트 신설**(손상 인식·환입·계정설정).
- **AC-14 (테넌트 격리):** 현재 테넌트만(@TenantId).
- **AC-15 (균형):** 환입 분개 차대변 균형(±0.01)·GL 결재 POST 가능.

## 4. 제약 / 비기능
- 금액 BigDecimal scale 2, 균형 ±0.01. 단일 트랜잭션(catch-up 상각 + 환입 분개).
- 분개는 `JournalEntryService.createInternal` DRAFT→GL 결재 경유, `linkReference(IMPAIRMENT_REVERSAL, assetId)`.
- 모듈 경계 finance 내. Flyway forward-only. 기존 손상/감가상각 회귀 금지.

## 5. 경계 / Do-Not
- ✅: 환입 인식·한도 산정·GL 분개·환입후 상각 재계산·계정설정·화면·음성 인가 테스트.
- ⚠️ 먼저 물어봐: 재평가모형 환입(OCI), 자동 환입징후, 한도 산정 방식 변경.
- 🚫: 한도 초과 환입, 손상누계 초과 환입, 손상 없는 자산 환입, 계정 미설정 빈 분개, 마감기간, 크로스테넌트, 불균형, 시크릿 커밋.

## 6. Open Questions
- (없음 — 온더플라이 한도·신규 환입계정·수동·환입후 재배분으로 확정. ExitPlanMode에서 승인.)

---

## 7. 기술 접근 (HOW)
**도메인 (`FixedAsset.java`):**
- `noImpairmentCarryingAmount(int monthsElapsed)`: 한도(가상 장부금액). 정액=`cost − min(monthsElapsed×(cost−residual)/usefulLifeMonths, cost−residual)`; 정률=`bv=cost; monthsElapsed회 {m=min(floor(bv×rate/12,2), bv−residual); bv−=m}` 후 bv. residual 하한.
- `applyReversal(BigDecimal amount, int remainingMonths)`: `accumulatedImpairment −= amount`; 정액이면 `straightLineMonthlyOverride = (bookValue − residual)/max(remainingMonths,1)` 재산정(상향 반영). (applyImpairment 대칭.)
- (기존 bookValue·monthlyDepreciation override 분기 재사용 — 변경 없음.)

**이력/멱등 (`ImpairmentEntry`):** `entry_type`(IMPAIRMENT|REVERSAL) 컬럼 추가. 환입은 type=REVERSAL, impairmentLoss에 환입액(부호 양수, 의미는 유형으로 구분), recoverableAmount·bookValueBefore 기록. UNIQUE를 (tenant,asset,period,**type**)로 변경.

**서비스 (`ImpairmentPostingService.reverseImpairment(assetId, fiscalPeriodId, recoverableAmount)`):**
1. FINANCE_WRITE → 회수가능액≥0(AC-9) → 자산·isActive(AC-12) → 기간 OPEN(AC-10)·취득전 가드(AC-11) → 미인식 REVERSAL(AC-7) → 환입 계정 설정(AC-8).
2. `catchUpThroughPeriod`(기존 재사용) — 측정 전 상각 현행화.
3. `monthsElapsed`=ChronoUnit(취득~인식기간, 손상과 동일 공식). `ceiling`=asset.noImpairmentCarryingAmount(monthsElapsed). `cap`=min(recoverable, ceiling). `reversal`=min(cap − bookValue, accumulatedImpairment). reversal≤0이면 F051(AC-6).
4. (차)손상차손누계액 (대)손상차손환입 DRAFT(createInternal) → linkReference(IMPAIRMENT_REVERSAL).
5. `remainingMonths`=usefulLifeMonths−monthsElapsed. `applyReversal(reversal, remainingMonths)` + ImpairmentEntry(REVERSAL) save.

**계정설정:** `TenantBaseCurrency.impairment_reversal_account_id` FK + `assignImpairmentAccounts` 확장(3개). `BaseCurrencyService.ImpairmentAccounts`에 reversalAccount 추가, get/update/current·DTO(`ImpairmentAccountResponse/UpdateRequest`) 확장. `ReferenceTypes.IMPAIRMENT_REVERSAL`. ErrorCode F050(계정 미설정)·F051(환입 불요)·F052(중복).

**마이그레이션 V2016__impairment_reversal.sql:** impairment_entry에 `entry_type VARCHAR(20) NOT NULL DEFAULT 'IMPAIRMENT'`; 기존 `uq_impairment_asset_period` DROP → `(tenant_id,fixed_asset_id,fiscal_period_id,entry_type) WHERE deleted_at IS NULL` 신규 UNIQUE; tenant_base_currency에 `impairment_reversal_account_id` FK. (forward-only·운영 미배포라 백필 안전.)

**컨트롤러:** `POST /api/finance/fixed-assets/{id}/impairment-reversal`(ImpairmentRecognizeRequest 재사용 — fiscalPeriodId·recoverableAmount). 계정설정 GET/PUT 확장. 손상 이력 응답에 entryType 포함.

**프론트:** `finance/fixed-assets` 환입 버튼(손상누계>0·ACTIVE)·모달(회수가능액)·이력에 유형(손상/환입) 표시·계정설정에 환입 계정 필드. types·actions 확장.

**테스트(AC↔):** 단위(도메인): AC-2 한도(정액·정률), AC-5 환입후 재배분, applyReversal. 통합: AC-1 분개균형·누계차감·이력, AC-3 한도상한, AC-4 손상누계상한, AC-6/9 거부, AC-7 멱등, AC-8 계정필수, AC-10/11/12 거부, AC-13 음성 인가(손상 인식·환입·계정설정 권한미보유 throw), AC-14, AC-15. **기존 손상/감가상각 회귀 GREEN.**

## 8. 태스크 (한 기능=한 브랜치 `feature/impairment-reversal`=한 PR)
| # | 태스크 | AC | 대상 | 검증(exit 0) | 의존 |
|---|---|---|---|---|---|
| 0 | 스펙 `docs/specs/impairment-reversal.md` 커밋 + impairment.md 환입 Out→In 갱신 | — | docs/specs | 파일 존재 | — |
| 1 | 도메인(noImpairmentCarryingAmount 정액/정률·applyReversal) + ImpairmentEntry entry_type + V2016 + 환입 계정설정(TenantBaseCurrency·BaseCurrencyService·DTO 확장) + ReferenceTypes·ErrorCode F050~F052 | AC-2,5,8(전제),14 | FixedAsset, ImpairmentEntry, V2016, TenantBaseCurrency, BaseCurrencyService, Impairment DTO, ReferenceTypes, ErrorCode | `cd backend && ./gradlew check`(한도·회귀 포함) | #0 |
| 2 | ImpairmentPostingService.reverseImpairment(catch-up→한도→환입액 3중상한→GL분개→누계차감→이력·멱등·검증) | AC-1,3,4,6,7,9,10,11,12,15 | ImpairmentPostingService, ImpairmentEntryRepository | `./gradlew check` | #1 |
| 3 | 컨트롤러(환입 엔드포인트·계정설정 확장)+DTO+권한 | AC-1,13(권한경로) | FixedAssetController | `./gradlew check` | #2 |
| 4 | 음성 인가 테스트 보강(손상 인식·환입·계정설정 권한미보유 거부) | AC-13 | ImpairmentPostingIntegrationTest 등 | `./gradlew check` | #2 |
| 5 | 프론트(환입 버튼·모달·이력 유형표시·계정설정 환입 필드)+타입 | AC-1(표시) | frontend finance/fixed-assets, types/finance.ts | `npm run type-check && lint && lint:design && test && build` | #3 |

## Verification (end-to-end)
1. `cd backend && ./gradlew check` — 도메인 단위(정액/정률 한도·환입후 재배분) + 통합(환입·분개균형·한도상한·손상누계상한·멱등·거부·계정필수·음성인가) green + **기존 손상/감가상각/처분 회귀 없음**.
2. `cd frontend && npm run type-check && lint && lint:design && test && build`.
3. 수동(docker-compose): 손상차손환입 계정 설정 → 자산(1,200만·60월·정액) 12개월 상각·480만 손상(장부 480만→재배분) → 이후 회수가능액 700만 환입 → 한도(가상 장부금액)까지만 환입·(차)손상누계 (대)환입 DRAFT·장부 상향·이후 상각 재배분 확인. 한도 초과 입력 시 한도까지만. 손상 없는 자산·중복·계정미설정·마감 차단. 권한 미보유 403.
4. 게이트: 태스크별 `/feature-add`(TDD) → 전 GREEN → 사용자 확인 → `/feature-merge`(한 PR).
