# impairment (자산 손상차손) 스펙 + 플랜

> 현지화 로드맵 Tier3 손상차손. 단일 출처: `docs/roadmap-localization.md`.
> 기존 고정자산(#7 PR#149)·감가상각·처분 catch-up(#156)·GL 자동분개·계정설정 패턴을 재사용한다.

## Context (왜 이 변경인가)
고정자산(#7)은 취득→상각→처분 생애주기를 회계에 통합했으나 **손상(impairment)은 명시적 Out**이었다(fixed-assets.md §2). 국내 상용 K-IFRS ERP는 자산 손상차손 인식을 갖춰야 하므로, 고정자산 장부가액이 회수가능액 아래로 떨어졌을 때 **손상차손을 인식하고 GL 자동분개**하는 기능을 추가한다. 결과: 자산별로 회수가능액을 등록하면 손상차손이 계산되어 (차)손상차손비 (대)손상차손누계액 분개가 생성되고, 장부가액이 하향되며, 손상 후 향후 감가상각이 K-IFRS대로 잔여 장부가액 기준으로 재배분된다.

### 확정된 설계 결정 (사용자 승인 완료)
1. **인식 방식 = 수동 등록.** 사용자가 자산별로 회수가능액을 입력해 손상을 인식. 손상징후 자동 트리거는 범위 밖.
2. **회수가능액 = 단일 입력.** max(순공정가치, 사용가치) 산정은 시스템 밖. 회수가능액 한 값만 입력.
3. **환입(reversal) = 이번엔 제외.** 손상 인식만. 환입(한도=손상 없었을 경우 장부금액)은 후속.
4. **손상 후 감가상각 = 잔여 장부가액 기준 재계산** (K-IFRS 정석). → 정액 공식이 취득원가 상수에서 잔여내용연수 재배분으로 바뀜(아래 §7 위험).

---

## 1. 목표 & Why
고정자산 장부가액이 회수가능액보다 클 때 그 차액을 **손상차손으로 인식**하고 GL에 자동 분개하며, 손상 후 향후 감가상각을 잔여 장부가액 기준으로 재계산한다. **성공 기준(측정 가능): 가동 자산에 대해 회수가능액(< 장부가액)을 등록하면, 손상차손액 = 장부가액 − 회수가능액으로 (차)손상차손비 (대)손상차손누계액 균형 DRAFT 분개가 생성되고, 자산 손상누계액·장부가액이 갱신되며, 같은 (자산,기간)을 다시 처리해도 중복 인식되지 않고, 이후 월 상각액이 (손상후 장부가액−잔존가치)/잔여내용연수로 재계산된다.**

## 2. Scope
- **In:**
  - **손상차손 인식(수동)**: 자산·회계기간·회수가능액 입력 → 인식기간까지 감가상각 catch-up → 손상차손액 = 장부가액 − 회수가능액(> 0일 때만) → GL 자동분개((차)손상차손비 (대)손상차손누계액, DRAFT→GL 결재 경유) + 자산 손상누계액 갱신 + 손상 이력 기록. **멱등**: 이미 처리한 (자산,기간)은 거부.
  - **손상 후 감가상각 재계산**: `bookValue() = 취득원가 − 감가상각누계액 − 손상누계액`. 정률은 자동 반영(월초 장부가액 기반). 정액은 (손상후 장부가액 − 잔존가치)/잔여내용연수로 재배분(비손상 자산엔 기존과 동일값).
  - **처분 분개 확장**: 손상된 자산 처분 시 손상차손누계액 계정도 청산하도록 처분 분개에 라인 추가(장부가액 정합).
  - **계정 설정(테넌트)**: 손상차손비(EXPENSE)·손상차손누계액(ASSET 차감) 계정 FK. 기존 고정자산 계정설정(감가상각/처분) 화면·엔드포인트 확장. 조회 FINANCE_READ, 변경 FINANCE_SETTING_WRITE.
  - **화면**: 고정자산 상세/목록에 손상차손 인식 버튼·모달(회수가능액 입력)·손상 이력·손상누계액/장부가액 표시. 계정설정에 손상 계정 2필드 추가.
- **Out (Non-goals):**
  - **손상 환입(reversal)** — 후속(별도). 한도 추적(손상 없었을 경우 장부가액) 미구현.
  - **손상징후 자동 감지/트리거**, 외부 평가(순공정가치·사용가치 산정·현금흐름할인).
  - **재고자산 손상(저가법)**, CGU(현금창출단위) 배분, 영업권 손상.
  - 세무상 손상(세무조정).
  - 손상 분개 자동 POST(결재 없이 전기) — 기존 GL 결재 흐름 경유.
  - 비동기 배치(스케줄) — 사용자 트리거 동기 실행.

## 3. 기능 요구사항 + 수용기준 (= 테스트 계약)
- **AC-1 (손상 인식, 정상):** WHEN 가동 자산에 회계기간·회수가능액(< 장부가액)으로 손상 인식(FINANCE_WRITE), the system SHALL 인식기간까지 감가상각을 catch-up한 뒤 손상차손액 = 장부가액 − 회수가능액으로 (차)손상차손비 (대)손상차손누계액 균형 DRAFT 분개를 만들고 자산 손상누계액을 갱신하며 손상 이력(ImpairmentEntry)을 남긴다.
- **AC-2 (장부가액 정의, 경계):** the system SHALL `bookValue() = 취득원가 − 감가상각누계액 − 손상누계액`로 산정한다(손상 후 장부가액이 회수가능액과 일치).
- **AC-3 (정률 손상후 상각, 정상):** WHILE 상각방법=정률 AND 손상 인식됨, the system SHALL 차기 월상각액 = 월초 장부가액(손상 반영) × (연상각률/12)로 자동 재계산한다.
- **AC-4 (정액 손상후 상각, 정상):** WHILE 상각방법=정액 AND 손상 인식됨, the system SHALL 손상 시점에 1회 산정한 잔여내용연수 재배분 월상각액(= (손상후 장부가액 − 잔존가치)/잔여내용연수)을 차기부터 적용한다. **(비손상 자산은 재배분값을 두지 않고 기존 (취득원가−잔존)/내용연수월 상수 공식을 그대로 사용 — 회귀 없음.)**
- **AC-5 (멱등, 경계):** IF 같은 (자산,기간)을 다시 손상 인식하면, THEN the system SHALL 거부(중복 인식·중복 분개 금지). 서비스 사전조회 + DB UNIQUE(자산,기간) 이중.
- **AC-6 (손상 불요, 예외):** IF 회수가능액 ≥ 장부가액이면, THEN the system SHALL 차단하고 명확한 오류(4xx) 반환(손상차손 0/음수 분개 금지).
- **AC-7 (회수가능액 유효성, 예외):** IF 회수가능액 < 0이면, THEN the system SHALL 400.
- **AC-8 (계정 미설정, 예외):** IF 손상차손비/손상차손누계액 계정 미설정 상태에서 손상 인식하면, THEN the system SHALL 차단하고 4xx(F047) 반환(빈 값 분개 금지).
- **AC-9 (처분 정합, 정상):** WHEN 손상된 자산(손상누계액>0)을 처분, the system SHALL 처분 분개에 손상차손누계액 청산 라인을 포함해 자산을 취득원가로 제거하고 처분손익 = 처분대가 − 장부가액으로 균형 분개한다.
- **AC-10 (회계기간, 경계):** IF 대상 회계기간이 OPEN이 아니면, THEN 손상 분개를 거부.
- **AC-11 (권한, 예외):** 손상 인식 FINANCE_WRITE, 계정 설정 FINANCE_SETTING_WRITE, 생성 분개 결재 FINANCE_GL_APPROVE. 없으면 403.
- **AC-12 (테넌트 격리, 경계):** the system SHALL 현재 테넌트의 자산·분개만 처리(@TenantId).
- **AC-13 (균형, 정상):** 손상·처분 분개는 차대변 균형(±0.01)이며 GL 결재로 POST 가능.
- **AC-14 (자산 상태, 예외):** IF 처분된 자산에 손상 인식하면, THEN 4xx(FIXED_ASSET_ALREADY_DISPOSED 재사용).

## 4. 제약 / 비기능
- 금액 BigDecimal scale 2, 균형 ±0.01.
- 손상 인식은 단일 트랜잭션(catch-up 상각 분개 + 손상 분개). 회계기간 OPEN·계정 설정 전제.
- 분개는 기존 `JournalEntryService.createInternal`로 DRAFT 생성→GL 결재(#69) 경유, `linkReference(ReferenceTypes.IMPAIRMENT, assetId)`.
- 모듈 경계: 전부 finance 내. 타 모듈 미참조. Flyway forward-only.
- **기존 감가상각 회귀 금지**: 정액 공식 변경(§7)이 비손상 자산의 기존 동작·테스트를 깨뜨리지 않아야 함.

## 5. 경계 / Do-Not (3단계)
- ✅ 해도 됨: ImpairmentEntry 엔티티·손상 인식·GL 자동분개·손상후 상각 재계산·처분 분개 손상 청산·계정 설정·화면.
- ⚠️ 먼저 물어봐: 환입, 손상징후 자동 트리거, 재고 손상, CGU, 정액 공식 외 추가 변경.
- 🚫 절대 금지: 회수가능액 ≥ 장부가액에 손상 인식, 같은 기간 중복 인식, 계정 미설정 빈 값 분개, 마감 회계기간 분개, 크로스테넌트, 차대변 불균형, 비손상 자산 감가상각 회귀, 시크릿 커밋.

## 6. Open Questions
- (없음 — 수동 인식·회수가능액 단일입력·환입 제외·정액 잔여내용연수 재배분으로 확정.)

---

## 7. 기술 접근 (HOW)
**패턴(기존 재사용, 조사 기준 파일·라인):**
- 손상 분개: `JournalEntryService.createInternal(JournalEntryCreateRequest)` + `JournalLineRequest`(차/대) — `DepreciationPostingService.postDepreciation()` 동형.
- 계정 설정: `TenantBaseCurrency`(감가상각/처분 계정 FK 패턴, 60–74행) 확장 + `BaseCurrencyService`(DepreciationAccounts record·get/update/current, 172–224행) 확장.
- catch-up: `DepreciationPostingService.catchUpBeforeDisposal(asset, date)` 패턴 → 손상 인식기간까지 상각.
- 멱등·이력: `DepreciationEntry` + UNIQUE(tenant,asset,period) (V2014:47–48) 동형.
- 처분: `FixedAssetService.dispose()` / `postDisposal()` (100–127행) 확장.

**엔티티/도메인 변경 (`FixedAsset.java`):**
- 신규 컬럼: `accumulatedImpairment NUMERIC(20,2) NOT NULL DEFAULT 0`, `straightLineMonthlyOverride NUMERIC(20,2)`(nullable — 손상 후 정액 재배분 월상각액. 정률·비손상은 null).
- `bookValue()` = `acquisitionCost − accumulatedDepreciation − accumulatedImpairment` (AC-2).
- `applyImpairment(loss, remainingMonths)`: 손상누계액 += loss; 정액이면 `straightLineMonthlyOverride = (bookValue − residual)/max(remainingMonths,1)`(DOWN, scale 2) 산정. 정률은 override 미설정.
- `applyDepreciation(amount)`: **무변경**(기존 누계 갱신만).
- `monthlyDepreciation()` 정액 분기: `raw = straightLineMonthlyOverride != null ? straightLineMonthlyOverride : (취득원가−잔존)/내용연수월` 후 `min(raw, bookValue − residual)` 가드. 정률 분기 무변경(`bookValue`가 손상 반영). **비손상 자산은 override=null → 기존 상수 공식 그대로 → 기존 도메인/통합 테스트 전부 무변경(완전 회귀 안전).** 잔여내용연수 = `내용연수월 − DepreciationEntry 개수`(손상 catch-up 후 산정, Task 2).

**신규 엔티티 `ImpairmentEntry`** (DepreciationEntry 동형): fixedAssetId·fiscalPeriodId·recoverableAmount·bookValueBefore·impairmentLoss·journalEntryId. UNIQUE(tenant,asset,period).

**신규 서비스 `ImpairmentPostingService.recognizeImpairment(assetId, fiscalPeriodId, recoverableAmount)`:**
1. 자산 조회·`isActive()`(AC-14) → 회계기간 OPEN(AC-10) → 손상 계정 설정 검증(AC-8) → 회수가능액 ≥ 0(AC-7).
2. `catchUpBeforeImpairment`(catchUpBeforeDisposal 일반화 — 인식기간 포함까지 상각).
3. `loss = bookValue − recoverableAmount`; loss ≤ 0이면 AC-6 거부.
4. (차)손상차손비 (대)손상차손누계액 DRAFT 분개(createInternal) → `linkReference(IMPAIRMENT, assetId)`.
5. `applyImpairment(loss)` + `ImpairmentEntry.of(...)` save(UNIQUE 멱등, AC-5).

**처분 분개 확장 (`FixedAssetService.postDisposal`):** `accumulatedImpairment > 0`이면 (차)손상차손누계액 라인 추가(자산 취득원가 제거 정합). `gainLoss = proceeds − bookValue()`는 이미 손상 반영(AC-9).

**계정 설정 확장:** `TenantBaseCurrency`에 `impairment_loss_account_id`(손상차손비, EXPENSE)·`accumulated_impairment_account_id`(손상차손누계액, ASSET 차감) FK 2개 + `assignImpairmentAccounts`·getters. `BaseCurrencyService`에 **별도 `ImpairmentAccounts` record + getImpairmentAccounts/updateImpairmentAccounts/currentImpairmentAccounts**(기존 Fx·Vat·Depreciation 계정군이 각각 독립 record/DTO/엔드포인트인 패턴과 동일 — DepreciationAccount 확장보다 일관적). DTO `ImpairmentAccountResponse`·`ImpairmentAccountUpdateRequest`. `ReferenceTypes.IMPAIRMENT` 추가. ErrorCode `F047 IMPAIRMENT_ACCOUNT_NOT_CONFIGURED`, `F048 IMPAIRMENT_NOT_REQUIRED`(회수가능액≥장부가액).

**마이그레이션 V2015__impairment.sql:** impairment_entry 테이블·SEQUENCE·UNIQUE 인덱스(tenant,asset,period); fixed_asset에 `accumulated_impairment NUMERIC(20,2) NOT NULL DEFAULT 0`·`straight_line_monthly_override NUMERIC(20,2)`(nullable) 컬럼 추가; tenant_base_currency에 손상 계정 FK 2개. (백필 불필요 — override는 손상 시에만 채워짐.)

**컨트롤러/DTO:** `POST /api/finance/fixed-assets/{id}/impairment` (ImpairmentRecognizeRequest: fiscalPeriodId, recoverableAmount), 손상 이력 GET, 계정설정 GET/PUT 확장. ImpairmentEntryResponse.

**프론트:** `finance/fixed-assets` 상세/목록에 손상 인식 버튼·모달(회수가능액)·손상 이력·손상누계액/장부가액 표시. 계정설정 다이얼로그에 손상 계정 2필드. `actions.ts`에 서버액션. `types/finance.ts`에 ImpairmentEntry·손상 계정 필드.

**테스트 전략(AC↔테스트):**
- 단위(도메인): AC-2 bookValue, AC-4 정액 손상후 재배분 + **비손상 동일값 회귀**, AC-3 정률 손상후, 손상누계 가드.
- 통합(@SpringBootTest): AC-1 인식·분개균형·누계·이력, AC-5 멱등, AC-6/7/8 거부, AC-9 손상자산 처분 정합·균형, AC-10 마감거부, AC-11 권한, AC-12 테넌트, AC-13 균형. **기존 감가상각/처분 통합테스트 전부 GREEN 유지(회귀).**
- 프론트: type-check/lint/build + 손상 인식·이력 렌더 스모크.

## 8. 태스크 (test-first 순서, 한 기능=한 브랜치 `feature/impairment`=한 PR, 태스크당 원자적 커밋)
| # | 태스크 | AC | 대상 파일 | 검증(exit 0) | 의존 | [P] |
|---|---|---|---|---|---|---|
| 0 | `docs/specs/impairment.md`로 이 스펙 커밋(repo 단일출처) | — | docs/specs/impairment.md | 파일 존재 | — | |
| 1 | FixedAsset 도메인(accumulatedImpairment·straightLineMonthlyOverride·bookValue·applyImpairment·정액 override 분기) + ImpairmentEntry + V2015(테이블·컬럼·계정FK) + 손상 계정설정(TenantBaseCurrency·BaseCurrencyService 별도 ImpairmentAccounts) | AC-2,3,4,8(전제),12 | FixedAsset, ImpairmentEntry, V2015, TenantBaseCurrency, BaseCurrencyService, Impairment DTO, ReferenceTypes, ErrorCode | `cd backend && ./gradlew check` (정액 회귀 포함) | #0 | |
| 2 | ImpairmentPostingService(catch-up→손상액 계산→GL분개→누계갱신→이력·멱등·계정필수·회계기간·회수가능액검증) | AC-1,5,6,7,8,10,13 | ImpairmentPostingService, ImpairmentEntryRepository, DepreciationPostingService(catch-up 일반화) | `./gradlew check` | #1 | |
| 3 | 처분 분개 손상차손누계액 청산 확장 | AC-9,13 | FixedAssetService(postDisposal) | `./gradlew check` (처분 회귀 포함) | #1 | |
| 4 | 컨트롤러(손상 인식·이력·계정설정 확장)+DTO+권한 | AC-1,11,14 | FixedAssetController, Impairment DTO | `./gradlew check` | #2,#3 | |
| 5 | 프론트(손상 인식 버튼·모달·이력·계정설정 2필드·장부가액 표시)+타입 | AC-1(표시) | frontend finance/fixed-assets, types/finance.ts | `npm run type-check && npm run lint && npm run build` | #4 | |

## Verification (end-to-end)
1. `cd backend && ./gradlew check` — 도메인 단위(손상후 정액/정률 상각·bookValue·**비손상 회귀**) + 통합(손상 인식·분개균형·멱등·거부·손상자산 처분 정합·마감·권한·테넌트) green + **기존 감가상각/처분 테스트 회귀 없음**.
2. `cd frontend && npm run type-check && npm run lint && npm run build`.
3. 수동(docker-compose): 손상차손비·손상차손누계액 계정 설정 → 자산(취득 1,200만·내용연수 60월·정액, 12개월 상각해 장부가액 960만) → 회수가능액 700만 손상 인식 → (차)손상차손비 260만/(대)손상차손누계액 260만 DRAFT·장부가액 700만 확인. 차기 월상각 = (700만−잔존)/48월로 재배분 확인. 같은 기간 재인식 거부(멱등). 회수가능액 ≥ 장부가액 거부. 계정 미설정·마감기간 차단. 손상자산 처분 시 손상누계액 청산·균형 확인.
4. 게이트: 태스크별 `/feature-add`(TDD) → 전 태스크 GREEN → 사용자 확인 → `/feature-merge`(한 PR, AI 리뷰·CI).
